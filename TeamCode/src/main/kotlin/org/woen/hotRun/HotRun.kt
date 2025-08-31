package org.woen.hotRun

import com.qualcomm.robotcore.eventloop.opmode.GamepadOpMode
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import kotlinx.coroutines.DisposableHandle
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.HardwareThreads
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedGamepad
import java.util.concurrent.atomic.AtomicReference

class HotRun private constructor() {
    companion object {
        private var _nullableInstance: HotRun? = null

        val LAZY_INSTANCE: HotRun
            get() {
                if(_nullableInstance == null)
                    _nullableInstance = HotRun()

                return _nullableInstance!!
            }

        fun restart() {
            _nullableInstance = null
            ThreadManager.restart()
            HardwareThreads.restart()
            ThreadedGamepad.restart()
            ThreadedTelemetry.restart()
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

    fun run(opMode: GamepadOpMode, runMode: RunMode) {
        ThreadManager.LAZY_INSTANCE.attachExceptionHandler()
        ThreadedGamepad.LAZY_INSTANCE.initCallbacks(opMode)
        ThreadedTelemetry.LAZY_INSTANCE.setDriveTelemetry(opMode.telemetry)
        HardwareThreads.LAZY_INSTANCE

        currentRunMode.set(runMode)
        currentRunState.set(RunState.INIT)

        opMode.waitForStart()
        opMode.resetRuntime()

        currentRunState.set(RunState.RUN)

        while (opMode.opModeIsActive()) {

        }

        currentRunState.set(RunState.STOP)
    }
}