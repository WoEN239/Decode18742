package org.woen.utils.process

import kotlinx.coroutines.delay

class Process {
    var closed = false
        private set

    fun close(){
        closed = true
    }

    suspend fun wait(){
        while (!closed)
            delay(5)
    }
}