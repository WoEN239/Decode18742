package org.woen.hotRun

import com.qualcomm.robotcore.eventloop.opmode.GamepadOpMode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedTimers
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.events.SimpleEvent
import java.util.concurrent.atomic.AtomicReference

class HotRun private constructor() {
    companion object {
        private var _nullableInstance: HotRun? = null

        private val _instanceMutex = Mutex()

        val LAZY_INSTANCE: HotRun
            get() = runBlocking {
                _instanceMutex.withLock {
                    if (_nullableInstance == null)
                        _nullableInstance = HotRun()

                    return@withLock _nullableInstance!!
                }
            }

        fun restart() {
            runBlocking {
                _instanceMutex.withLock {
                    _nullableInstance = null
                }
            }

            ThreadManager.restart()
            HardwareThreads.restart()
            ThreadedGamepad.restart()
            ThreadedTelemetry.restart()
            ThreadedBattery.restart()
            ThreadedTimers.restart()
            ThreadedEventBus.restart()
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

    val opModeInitEvent = SimpleEvent<GamepadOpMode>()
    val opModeStartEvent = SimpleEvent<GamepadOpMode>()
    val opModeUpdateEvent = SimpleEvent<GamepadOpMode>()
    val opModeStopEvent = SimpleEvent<GamepadOpMode>()

    fun run(opMode: GamepadOpMode, runMode: RunMode) {
        ThreadManager.LAZY_INSTANCE.attachExceptionHandler()
        ThreadedGamepad.LAZY_INSTANCE.initCallbacks(opMode)
        ThreadedTelemetry.LAZY_INSTANCE.setDriveTelemetry(opMode.telemetry)
        HardwareThreads.LAZY_INSTANCE

        currentRunMode.set(runMode)
        currentRunState.set(RunState.INIT)

        opModeInitEvent.invoke(opMode)

        opMode.waitForStart()
        opMode.resetRuntime()

        currentRunState.set(RunState.RUN)

        opModeStartEvent.invoke(opMode)

        while (opMode.opModeIsActive()) {

        }

        currentRunState.set(RunState.STOP)

        opModeStopEvent.invoke(opMode)
    }
}