package org.woen.threading

import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ThreadedTimers: DisposableHandle {
    companion object {
        private var _nullableInstance: ThreadedTimers? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: ThreadedTimers
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null)
                    _nullableInstance = ThreadedTimers()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance?.dispose()
                _nullableInstance = null
            }
        }
    }

    private val _timersExecutor = Executors.newSingleThreadExecutor()
    private val _timersCoroutineDispatcher = _timersExecutor.asCoroutineDispatcher()
    private val _timersCoroutineScope = CoroutineScope(_timersCoroutineDispatcher + Job())

    interface ITimer {
        suspend fun start()

        val isActive: Boolean
    }

    class Timer(val time: Double, val function: () -> Unit) : ITimer {
        private var _isActive = AtomicBoolean(true)

        override suspend fun start() {
            _isActive.set(true)

            delay((time * 1000.0).toLong())

            function()

            _isActive.set(false)
        }

        override val isActive: Boolean
            get() = _isActive.get()
    }

    class SupplerTimer(
        val suppler: () -> Boolean,
        val function: () -> Unit,
        val timeout: Double = -1.0,
        val timeoutFunction: () -> Unit = {}
    ) : ITimer {
        private var _isActive = AtomicBoolean(true)

        private val _activeTime = ElapsedTime()

        override suspend fun start() {
            _isActive.set(true)

            _activeTime.reset()

            while (!suppler() && (_activeTime.seconds() < timeout || timeout < 0.0))
                delay(5)

            if (_activeTime.seconds() >= timeout && timeout >= 0.0)
                timeoutFunction()
            else
                function()

            _isActive.set(false)
        }

        override val isActive: Boolean
            get() = _isActive.get()
    }

    fun startTimer(timer: ITimer) {
        _timersCoroutineScope.launch {
            timer.start()
        }
    }

    override fun dispose() {
        _timersExecutor.shutdown()
    }

    private constructor(){
        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            _timersCoroutineScope.cancel()
        }
    }
}