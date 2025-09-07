package org.woen.utils.events

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SimpleEvent<T> {
    private val listeners = mutableSetOf<(T) -> Unit>()

    private val _listenersMutex = Mutex()

    operator fun plusAssign(listener: (T) -> Unit) {
        runBlocking {
            _listenersMutex.withLock {
                listeners.add(listener)
            }
        }
    }

    operator fun minusAssign(listener: (T) -> Unit) {
        runBlocking {
            _listenersMutex.withLock {
                if (listeners.contains(listener))
                    listeners.remove(listener)
            }
        }
    }

    operator fun invoke(data: T) {
        runBlocking {
            _listenersMutex.withLock {
                for (i in listeners)
                    i.invoke(data)
            }
        }
    }
}