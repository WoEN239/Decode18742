package org.woen.utils.events

class SimpleEvent<T> {
    private val listeners = mutableSetOf<(T) -> Unit>()

    @Synchronized
    operator fun plusAssign(listener: (T) -> Unit){
        listeners.add(listener)
    }

    @Synchronized
    operator fun minusAssign(listener: (T) -> Unit){
        listeners.remove(listener)
    }

    @Synchronized
    operator fun invoke(data: T){
        for(i in listeners)
            i.invoke(data)
    }
}