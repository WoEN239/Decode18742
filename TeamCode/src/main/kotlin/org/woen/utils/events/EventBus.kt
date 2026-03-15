package org.woen.utils.events

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class EventBus {
    private val _events =  hashMapOf<KClass<*>, MutableSet<(Any) -> Unit>>()

    fun <T: Any> subscribe(event: KClass<T>, callback: (T) -> Unit){
        if(_events[event] == null)
            _events[event] = mutableSetOf()

        _events[event]?.add(callback as (Any) -> Unit)
    }

    fun <T: Any> invoke(event: T): T{
        if(_events[event::class] == null)
            return event

        for(i in _events[event::class]!!)
            i.invoke(event)

        return event
    }
}