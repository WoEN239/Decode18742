package org.woen.utils.smartMutex

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference

class SmartMutex {
    private val _mutex = Mutex()

    private val _lockedThread = AtomicReference<Thread>()

    fun <T> smartLock(action: suspend () -> T): T {
        val result: T

        val lockedThread = _lockedThread.get()

        runBlocking {
            when (lockedThread) {
                null -> {
                    _mutex.withLock {
                        _lockedThread.set(Thread.currentThread())
                        result = action()
                        _lockedThread.set(null)
                    }
                }
                Thread.currentThread() -> result = action()
                else -> _mutex.withLock {
                    _lockedThread.set(Thread.currentThread())
                    result = action()
                    _lockedThread.set(null)
                }
            }
        }

        return result
    }
}