package org.woen.threading

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.telemetry.ThreadedConfigs
import java.util.concurrent.Executors

class ThreadManager : DisposableHandle {
    companion object {
        private var _nullableInstance: ThreadManager? = null

        private val _instanceMutex = Mutex()

        val LAZY_INSTANCE: ThreadManager
            get() =
                runBlocking {
                    _instanceMutex.withLock {
                        if (_nullableInstance == null)
                            _nullableInstance = ThreadManager()

                        return@withLock _nullableInstance!!
                    }
                }

        fun restart() {
            runBlocking {
                _instanceMutex.withLock {
                    _nullableInstance?.dispose()
                    _nullableInstance = null
                }
            }
        }
    }

    private val _threadPool =
        Executors.newFixedThreadPool(ThreadedConfigs.THREAD_POOL_THREADS_COUNT.get())
    val globalCoroutineScope = CoroutineScope(_threadPool.asCoroutineDispatcher() + Job())

    private val _allThreads = mutableSetOf<Thread>()

    private val _allThreadsMutex = Mutex()

    private var _mainHandler: Handler? = null

    fun attachExceptionHandler() {
        _mainHandler = Looper.myLooper()?.let { Handler(it) }
    }

    fun register(thread: Thread): Thread {
        thread.setUncaughtExceptionHandler { _, exception ->
            _mainHandler?.post {
                throw exception
            }
        }

        runBlocking {
            _allThreadsMutex.withLock {
                _allThreads.add(thread)
            }
        }

        return thread
    }

    override fun dispose() {
        runBlocking {
            _allThreadsMutex.withLock {
                for (i in _allThreads)
                    i.interrupt()
            }
        }

        _threadPool.shutdown()
    }
}