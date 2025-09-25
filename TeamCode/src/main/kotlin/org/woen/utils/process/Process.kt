package org.woen.utils.process

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference

class Process {
    var closed = AtomicReference(false)
        private set

    fun close() {
        closed.set(true)
    }

    suspend fun wait() {
        while (!closed.get())
            delay(5)
    }
}