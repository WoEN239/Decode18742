package org.woen.threading

import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ThreadedTimers private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: ThreadedTimers? = null

        @get:Synchronized
        val LAZY_INSTANCE: ThreadedTimers
            get() {
                if (_nullableInstance == null)
                    _nullableInstance = ThreadedTimers()

                return _nullableInstance!!
            }

        fun restart() {
            _nullableInstance?.dispose()
            _nullableInstance = null
        }
    }

    private val _timersExecutor = Executors.newSingleThreadExecutor()
    private val _timersCoroutineDispatcher = _timersExecutor.asCoroutineDispatcher()
    private val _timersCoroutineScope = CoroutineScope(_timersCoroutineDispatcher)

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

            while (!suppler() && _activeTime.seconds() < timeout)
                delay(5)

            if (_activeTime.seconds() >= timeout)
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
}