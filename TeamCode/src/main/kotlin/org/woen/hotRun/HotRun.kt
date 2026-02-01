package org.woen.hotRun

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
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
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

class HotRun private constructor() {
    companion object {
        private var _nullableInstance: HotRun? = null

        private val _instanceMutex = SmartMutex()

        val LAZY_INSTANCE: HotRun
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null) _nullableInstance = HotRun()

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
            //Camera.restart()
            ThreadedGamepad.restart()
            ThreadedBattery.restart()
            HardwareThreads.restart()
            RobotState.restart()
        }
    }

    var currentRunState = RunState.STOP
        private set

    var currentRunMode = RunMode.MANUAL
        private set

    enum class RunState {
        INIT, STOP, RUN
    }

    enum class RunMode {
        MANUAL, AUTO
    }

    enum class StartPosition(
        val basketPosition: Vec2,
        val startOrientation: Orientation,
        val parkingOrientation: Orientation,
        val color: RunColor,
        val shootingOrientation: Orientation,
        val position: RunPosition
    ) {
        RED_CLOSE(
            Configs.TURRET.RED_BASKET_POSITION,
            Configs.ODOMETRY.START_RED_CLOSE_ORIENTATION,
            Configs.DRIVE_TRAIN.RED_PARKING_ORIENTATION,
            RunColor.RED,
            Configs.TURRET.SHOOTING_RED_ORIENTATION,
            RunPosition.CLOSE
        ),
        BLUE_CLOSE(
            Configs.TURRET.BLUE_BASKET_POSITION,
            Configs.ODOMETRY.START_BLUE_CLOSE_ORIENTATION,
            Configs.DRIVE_TRAIN.BLUE_PARKING_ORIENTATION,
            RunColor.BLUE,
            Configs.TURRET.SHOOTING_BLUE_ORIENTATION,
            RunPosition.CLOSE
        ),
        RED_FAR(
            Configs.TURRET.RED_BASKET_POSITION,
            Configs.ODOMETRY.START_RED_FAR_ORIENTATION,
            Configs.DRIVE_TRAIN.RED_PARKING_ORIENTATION,
            RunColor.RED,
            Configs.TURRET.SHOOTING_RED_ORIENTATION,
            RunPosition.FAR
        ),
        BLUE_FAR(
            Configs.TURRET.BLUE_BASKET_POSITION,
            Configs.ODOMETRY.START_BLUE_FAR_ORIENTATION,
            Configs.DRIVE_TRAIN.BLUE_PARKING_ORIENTATION,
            RunColor.BLUE,
            Configs.TURRET.SHOOTING_BLUE_ORIENTATION,
            RunPosition.FAR
        );
    }

    enum class RunColor {
        RED,
        BLUE
    }

    enum class RunPosition {
        FAR,
        CLOSE
    }

    var currentStartPosition = StartPosition.RED_CLOSE

    val opModeInitEvent = SimpleEmptyEvent()
    val opModeStartEvent = SimpleEmptyEvent()
    val opModeUpdateEvent = SimpleEmptyEvent()
    val opModeStopEvent = SimpleEmptyEvent()
    val disposableEvent = SimpleEmptyEvent()

    fun run(opMode: LinearOpMode, runMode: RunMode, isGamepadStart: Boolean = false) {
        currentRunMode = runMode
        currentRunState = RunState.INIT

        ThreadManager.LAZY_INSTANCE
        ThreadedTelemetry.LAZY_INSTANCE.setDriveTelemetry(opMode.telemetry)
        ThreadedGamepad.LAZY_INSTANCE.init(opMode.gamepad1, opMode.gamepad2)
        HardwareThreads.LAZY_INSTANCE
        ActionRunner.LAZY_INSTANCE
        //Camera.LAZY_INSTANCE

        ThreadedTelemetry.LAZY_INSTANCE.telemetryEnabled = true

        opModeInitEvent.invoke()

        ThreadedTelemetry.LAZY_INSTANCE.log("init completed")

        while (!opMode.isStarted()) {
            if (isGamepadStart && ThreadedGamepad.LAZY_INSTANCE.getIsGamepadsTriggered()) OpModeManagerImpl.getOpModeManagerOfActivity(
                AppUtil.getInstance().activity
            ).startActiveOpMode()
        }

        if (opMode.opModeIsActive()) {
            opMode.resetRuntime()

            ThreadedTelemetry.LAZY_INSTANCE.log("op mode start")

            currentRunState = RunState.RUN

            opModeStartEvent.invoke()

            while (opMode.opModeIsActive())
                opModeUpdateEvent.invoke()
        }

        currentRunState = RunState.STOP

        ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            opModeStopEvent.invoke()

            for (i in opMode.hardwareMap.servoController) i.pwmDisable()

            ThreadedTelemetry.LAZY_INSTANCE.log("op mode stoped")

            ThreadedTelemetry.LAZY_INSTANCE.telemetryEnabled = false
        }
    }
}