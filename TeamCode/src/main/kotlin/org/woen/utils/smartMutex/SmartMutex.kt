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

        runBlocking {
            if (_lockedThread.get() == null || _lockedThread.get() != Thread.currentThread())
                _mutex.withLock {
                    _lockedThread.set(Thread.currentThread())
                    result = action()
                    _lockedThread.set(null)
                }
            else
                result = action()
        }

        return result
    }
}