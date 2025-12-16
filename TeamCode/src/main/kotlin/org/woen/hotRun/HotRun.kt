package org.woen.hotRun

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.hardware.Gamepad
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
        val basketPosition: Vec2, val startOrientation: Orientation
    ) {
        RED(Configs.TURRET.RED_BASKET_POSITION, Configs.ODOMETRY.START_RED_ORIENTATION),
        BLUE(Configs.TURRET.BLUE_BASKET_POSITION, Configs.ODOMETRY.START_BLUE_ORIENTATION);
    }

    var currentRunColor = RunColor.BLUE

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

            while (opMode.opModeIsActive())
                opModeUpdateEvent.invoke(opMode)

            currentRunState = RunState.STOP

            opModeStopEvent.invoke(opMode)

            for (i in opMode.hardwareMap.servoController)
                i.pwmDisable()
        } catch (e: Exception) {
            throw e
        }
    }
}