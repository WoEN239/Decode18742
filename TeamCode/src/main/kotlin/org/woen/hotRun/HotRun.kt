package org.woen.hotRun

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import kotlinx.coroutines.DisposableHandle
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.HardwareThreads
import org.woen.threading.ThreadManager
import java.util.concurrent.atomic.AtomicReference

class HotRun private constructor() : DisposableHandle {
    companion object {
        var INSTANCE: HotRun? = null
            private set

        fun init() {
            INSTANCE = HotRun()
            HardwareThreads.init()
        }

        fun restart() {
            INSTANCE?.dispose()
            ThreadManager.restart()
            init()
        }
    }

    var currentRunState = AtomicReference(RunState.STOP)
        private set

    enum class RunState {
        INIT,
        STOP,
        RUN
    }

    fun run(opMode: LinearOpMode) {
        ThreadManager.LAZY_INSTANCE.attachExceptionHandler()
        ThreadedTelemetry.LAZY_INSTANCE.setDriveTelemetry(opMode.telemetry)

        currentRunState.set(RunState.INIT)

        opMode.waitForStart()
        opMode.resetRuntime()

        currentRunState.set(RunState.RUN)

        while (opMode.opModeIsActive()) {

        }

        currentRunState.set(RunState.STOP)
    }

    override fun dispose() {
        HardwareThreads.INSTANCE?.dispose()
    }
}