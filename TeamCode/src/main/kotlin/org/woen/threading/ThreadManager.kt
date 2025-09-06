package org.woen.threading

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.DisposableHandle

class ThreadManager : DisposableHandle {
    companion object {
        private var _nullableInstance: ThreadManager? = null

        @get:Synchronized
        val LAZY_INSTANCE: ThreadManager
            get() {
                if (_nullableInstance == null)
                    _nullableInstance = ThreadManager()

                return _nullableInstance!!
            }

        fun restart() {
            _nullableInstance?.dispose()
            _nullableInstance = null
        }
    }

    private val _allThreads = mutableSetOf<Thread>()

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

        _allThreads.add(thread)

        return thread
    }

    override fun dispose() {
        for (i in _allThreads)
            i.interrupt()
    }
}