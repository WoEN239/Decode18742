package org.woen.threading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class ThreadingEventBus private constructor() {
    companion object {
        private var _nullableInstance: ThreadingEventBus? = null

        @get:Synchronized
        val LAZY_INSTANCE: ThreadingEventBus
            get() {
                if (_nullableInstance == null)
                    _nullableInstance = ThreadingEventBus()

                return _nullableInstance!!
            }

        fun restart() {
            _nullableInstance = null
        }
    }

    private val _events = hashMapOf<KClass<*>, MutableSet<Pair<(Any) -> Unit, CoroutineScope>>>()

    private val _eventsMutex = Mutex()

    @OptIn(DelicateCoroutinesApi::class)
    fun <T : Any> subscribe(
        event: KClass<T>,
        callback: (T) -> Unit,
        scope: CoroutineScope = GlobalScope
    ) {
        runBlocking {
            _eventsMutex.withLock {
                if (_events[event] == null)
                    _events[event] = mutableSetOf()

                _events[event]?.add(Pair(callback as (Any) -> Unit, scope))
            }
        }
    }

    fun <T : Any> invoke(event: T): T {
        val callbacks: MutableSet<Pair<(Any) -> Unit, CoroutineScope>>?

        runBlocking {
            _eventsMutex.withLock {
                callbacks = _events[event::class]
            }
        }

        if (callbacks == null)
            return event

        for (i in callbacks) {
            i.second.launch {
                i.first.invoke(event)
            }
        }

        return event
    }
}