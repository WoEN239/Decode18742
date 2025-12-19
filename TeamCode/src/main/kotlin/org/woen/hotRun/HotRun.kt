package org.woen.hotRun

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.launch
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.modules.camera.Camera
import org.woen.modules.runner.actions.ActionRunner
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedTimers
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.events.SimpleEmptyEvent
import org.woen.utils.events.SimpleEvent
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

class HotRun private constructor() {
    companion object {
        private var _nullableInstance: HotRun? = null

        private val _instanceMutex = SmartMutex()

        val LAZY_INSTANCE: HotRun
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null)
                    _nullableInstance = HotRun()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _nullableInstance?.disposableEvent?.invoke()

            _instanceMutex.smartLock {
                _nullableInstance = null
            }

            ThreadManager.restart()
            ThreadedTelemetry.restart()
            ActionRunner.restart()
            ThreadedTimers.restart()
            ThreadedEventBus.restart()
            Camera.restart()
            ThreadedGamepad.restart()
            ThreadedBattery.restart()
            HardwareThreads.restart()
        }
    }

    var currentRunState = RunState.STOP
        private set

    var currentRunMode = RunMode.MANUAL
        private set

    enum class RunState {
        INIT,
        STOP,
        RUN
    }

    enum class RunMode {
        MANUAL,
        AUTO
    }

    enum class RunColor(
        val basketPosition: Vec2,
        val startOrientation: Orientation,
        val parkingOrientation: Orientation
    ) {
        RED(
            Configs.TURRET.RED_BASKET_POSITION,
            Configs.ODOMETRY.START_RED_ORIENTATION,
            Configs.DRIVE_TRAIN.RED_PARKING_ORIENTATION
        ),
        BLUE(
            Configs.TURRET.BLUE_BASKET_POSITION,
            Configs.ODOMETRY.START_BLUE_ORIENTATION,
            Configs.DRIVE_TRAIN.BLUE_PARKING_ORIENTATION
        );
    }

    var currentRunColor = RunColor.RED

    val opModeInitEvent = SimpleEvent<LinearOpMode>()
    val opModeStartEvent = SimpleEvent<LinearOpMode>()
    val opModeUpdateEvent = SimpleEvent<LinearOpMode>()
    val opModeStopEvent = SimpleEvent<LinearOpMode>()
    val disposableEvent = SimpleEmptyEvent()

    fun run(opMode: LinearOpMode, runMode: RunMode, isGamepadStart: Boolean = false) {
        try {
            currentRunMode = runMode
            currentRunState = RunState.INIT

            ThreadManager.LAZY_INSTANCE
            ThreadedTelemetry.LAZY_INSTANCE.setDriveTelemetry(opMode.telemetry)
            ThreadedGamepad.LAZY_INSTANCE.init(opMode.gamepad1)
            HardwareThreads.LAZY_INSTANCE
            ActionRunner.LAZY_INSTANCE
            Camera.LAZY_INSTANCE

            opModeInitEvent.invoke(opMode)

            ThreadedTelemetry.LAZY_INSTANCE.log("init completed")

            while (!opMode.isStarted()) {
                if (isGamepadStart && ThreadedGamepad.LAZY_INSTANCE.getIsGamepadTriggered())
                    OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
                        .startActiveOpMode()
            }
            opMode.resetRuntime()

            ThreadedTelemetry.LAZY_INSTANCE.log("start")

            currentRunState = RunState.RUN

            opModeStartEvent.invoke(opMode)

            val updateTimer = ElapsedTime()

            while (opMode.opModeIsActive()) {
                val updateCoroutine = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                    opModeUpdateEvent.invoke(opMode)
                }

                updateTimer.reset()

                while (!updateCoroutine.isCompleted){
                    Thread.sleep(5)

                    if(updateTimer.seconds() > Configs.HOT_RUN.MAX_UPDATE_TIME) {
                        ThreadedTelemetry.LAZY_INSTANCE.log("update coroutine is killed")

                        updateCoroutine.cancel()
                        break
                    }
                }
            }

            currentRunState = RunState.STOP

            val stopCoroutine = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                opModeStopEvent.invoke(opMode)
            }

            val stopTimer = ElapsedTime()
            stopTimer.reset()

            while (!stopCoroutine.isCompleted) {
                Thread.sleep(5)

                if(stopTimer.seconds() > Configs.HOT_RUN.MAX_STOP_TIME) {
                    ThreadedTelemetry.LAZY_INSTANCE.log("stop coroutine is killed")

                    stopCoroutine.cancel()

                    break
                }
            }

            for (i in opMode.hardwareMap.servoController)
                i.pwmDisable()
        } catch (e: Exception) {
            throw e
        }
    }
}