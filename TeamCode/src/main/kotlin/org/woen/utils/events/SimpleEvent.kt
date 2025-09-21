package org.woen.utils.events

import kotlinx.coroutines.runBlocking
import org.woen.utils.smartMutex.SmartMutex

class SimpleEvent<T> {
    private val listeners = mutableSetOf<(T) -> Unit>()

    private val _listenersMutex = SmartMutex()

    operator fun plusAssign(listener: (T) -> Unit) {
        runBlocking {
            _listenersMutex.smartLock {
                listeners.add(listener)
            }
        }
    }

    operator fun minusAssign(listener: (T) -> Unit) {
        runBlocking {
            _listenersMutex.smartLock {
                if (listeners.contains(listener))
                    listeners.remove(listener)
            }
        }
    }

    operator fun invoke(data: T) {
        runBlocking {
            _listenersMutex.smartLock {
                for (i in listeners)
                    i.invoke(data)
            }
        }
    }
}