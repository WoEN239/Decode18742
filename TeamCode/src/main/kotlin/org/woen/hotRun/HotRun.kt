package org.woen.hotRun

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
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
import org.woen.utils.events.SimpleEvent
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicReference

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

    var currentRunState = AtomicReference(RunState.STOP)
        private set

    var currentRunMode = AtomicReference(RunMode.MANUAL)
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
        private val _basketPosition: Vec2, private val _startOrientation: Orientation
    ) {
        RED(Configs.TURRET.RED_BASKET_POSITION, Configs.ODOMETRY.START_RED_ORIENTATION),
        BLUE(Configs.TURRET.BLUE_BASKET_POSITION, Configs.ODOMETRY.START_BLUE_ORIENTATION);

        val basketPosition
            get() = _basketPosition.clone()

        val startOrientation
            get() = _startOrientation.clone()
    }

    var currentRunColor = AtomicReference(RunColor.RED)

    val opModeInitEvent = SimpleEvent<LinearOpMode>()
    val opModeStartEvent = SimpleEvent<LinearOpMode>()
    val opModeUpdateEvent = SimpleEvent<LinearOpMode>()
    val opModeStopEvent = SimpleEvent<LinearOpMode>()

    fun run(opMode: LinearOpMode, runMode: RunMode) {
        try {
            currentRunMode.set(runMode)
            currentRunState.set(RunState.INIT)

            ThreadManager.LAZY_INSTANCE
            ThreadedGamepad.LAZY_INSTANCE.init()
            ThreadedTelemetry.LAZY_INSTANCE.setDriveTelemetry(opMode.telemetry)
            HardwareThreads.LAZY_INSTANCE
            ActionRunner.LAZY_INSTANCE
            Camera.LAZY_INSTANCE

            opModeInitEvent.invoke(opMode)

            ThreadedTelemetry.LAZY_INSTANCE.log("init complited")

            opMode.waitForStart()
            opMode.resetRuntime()

            ThreadedTelemetry.LAZY_INSTANCE.log("start")

            currentRunState.set(RunState.RUN)

            opModeStartEvent.invoke(opMode)

            while (opMode.opModeIsActive()) {
                opModeUpdateEvent.invoke(opMode)
            }

            currentRunState.set(RunState.STOP)

            opModeStopEvent.invoke(opMode)
        } catch (e: Exception) {
            throw e
        }
    }
}