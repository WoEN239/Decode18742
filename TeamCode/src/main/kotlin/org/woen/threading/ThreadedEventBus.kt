package org.woen.threading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class ThreadedEventBus private constructor() {
    companion object {
        private var _nullableInstance: ThreadedEventBus? = null

        private val _instanceMutex = Mutex()

        @JvmStatic
        val LAZY_INSTANCE: ThreadedEventBus
            get() = runBlocking {
                _instanceMutex.withLock {
                    if (_nullableInstance == null)
                        _nullableInstance = ThreadedEventBus()

                    return@withLock _nullableInstance!!
                }
            }

        fun restart() {
            runBlocking {
                _instanceMutex.withLock {
                    _nullableInstance = null
                }
            }
        }
    }

    private val _events = hashMapOf<KClass<*>, MutableSet<Pair<suspend (Any) -> Unit, CoroutineScope>>>()

    private val _eventsMutex = Mutex()

    @OptIn(DelicateCoroutinesApi::class)
    fun <T : Any> subscribe(
        event: KClass<T>,
        callback: suspend (T) -> Unit,
        scope: CoroutineScope = ThreadManager.LAZY_INSTANCE.globalCoroutineScope
    ) {
        runBlocking {
            _eventsMutex.withLock {
                if (_events[event] == null)
                    _events[event] = mutableSetOf()

                _events[event]?.add(Pair(callback as suspend (Any) -> Unit, scope))
            }
        }
    }

    fun <T : Any> invoke(event: T): T {
        val callbacks: MutableSet<Pair<suspend (Any) -> Unit, CoroutineScope>>?

        runBlocking {
            _eventsMutex.withLock {
                callbacks = _events[event::class]
            }
        }

        if (callbacks == null)
            return event

        runBlocking {
            for (i in callbacks) {
                i.second.launch {
                    i.first.invoke(event)
                }
            }
        }

        return event
    }
}