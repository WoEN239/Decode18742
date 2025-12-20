package org.woen.utils.events

import java.util.concurrent.CopyOnWriteArrayList

class SimpleEvent<T> {
    private val listeners = CopyOnWriteArrayList<(T) -> Unit>()

    operator fun plusAssign(listener: (T) -> Unit) {
        listeners.add(listener)
    }

    operator fun minusAssign(listener: (T) -> Unit) {
        if (listeners.contains(listener))
            listeners.remove(listener)
    }

    operator fun invoke(data: T) {
        for (i in listeners)
            i.invoke(data)
    }
}

class SimpleEmptyEvent {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    operator fun plusAssign(listener: () -> Unit) {
        listeners.add(listener)
    }

    operator fun minusAssign(listener: () -> Unit) {
        if (listeners.contains(listener))
            listeners.remove(listener)
    }

    operator fun invoke() {
        for (i in listeners)
            i.invoke()
    }
}