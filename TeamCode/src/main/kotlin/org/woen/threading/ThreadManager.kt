package org.woen.threading

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.Executors

class ThreadManager private constructor(): DisposableHandle {
    companion object {
        private var _nullableInstance: ThreadManager? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: ThreadManager
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null)
                    _nullableInstance = ThreadManager()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance?.dispose()
                _nullableInstance = null
            }
        }
    }

    private val _threadPool =
        Executors.newFixedThreadPool(Configs.THREAD_POOL.THREAD_POOL_THREADS_COUNT)
    val globalCoroutineScope = CoroutineScope(_threadPool.asCoroutineDispatcher() + Job())

    private val _allThreads = mutableSetOf<Thread>()

    private val _allThreadsMutex = SmartMutex()

    private var _mainHandler: Handler? = null

    fun attachExceptionHandler() {
        _mainHandler = Looper.myLooper()?.let { Handler(it) }
    }

    fun register(thread: Thread): Thread {
        thread.setUncaughtExceptionHandler { _, exception ->
            ThreadedTelemetry.LAZY_INSTANCE.log(exception.message!!)

            for (i in exception.stackTrace)
                ThreadedTelemetry.LAZY_INSTANCE.log(i.className + ": " + i.methodName)

            _mainHandler?.post {
                throw exception
            }
        }

        _allThreadsMutex.smartLock {
            _allThreads.add(thread)
        }

        return thread
    }

    override fun dispose() {
        _allThreadsMutex.smartLock {
            for (i in _allThreads)
                i.interrupt()
        }

        _threadPool.shutdown()
    }
}