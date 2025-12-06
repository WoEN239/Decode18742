package org.woen.threading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

class ThreadManager private constructor() : DisposableHandle {
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

    val threadFactory = ThreadFactory { runnable ->
        val thread = register(Thread(runnable))
        thread
    }

    private val _threadPool =
        Executors.newFixedThreadPool(Configs.THREAD_POOL.THREAD_POOL_THREADS_COUNT, threadFactory)
    val globalCoroutineScope = CoroutineScope(_threadPool.asCoroutineDispatcher() + Job())

    private val _allThreads = CopyOnWriteArrayList<Thread>()

    fun register(thread: Thread): Thread {
        thread.setUncaughtExceptionHandler { _, exception ->
            if (exception !is InterruptedException) {
                if (exception.message != null)
                    ThreadedTelemetry.LAZY_INSTANCE.log(exception.message!!)

                exception.cause?.let { ThreadedTelemetry.LAZY_INSTANCE.log(it.toString()) }

                ThreadedTelemetry.LAZY_INSTANCE.log(exception.toString())

                for (i in exception.stackTrace)
                    ThreadedTelemetry.LAZY_INSTANCE.log(i.className + ": " + i.lineNumber.toString())
            }
        }

        _allThreads.add(thread)

        return thread
    }

    override fun dispose() {
        for (i in _allThreads)
            i.interrupt()

        _threadPool.shutdown()
    }
}