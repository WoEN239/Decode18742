package org.woen.utils.events

class SimpleEvent<T> {
    private val listeners = ArrayList<(T) -> Unit>()

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

    val listenersCount
        get() = listeners.size
}

class SimpleEmptyEvent {
    private val listeners = ArrayList<() -> Unit>()

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

    val listenersCount
        get() = listeners.size
}