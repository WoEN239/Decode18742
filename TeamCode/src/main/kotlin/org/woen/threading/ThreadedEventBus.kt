package org.woen.threading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.woen.utils.smartMutex.SmartMutex
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class ThreadedEventBus private constructor() {
    companion object {
        private var _nullableInstance: ThreadedEventBus? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: ThreadedEventBus
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null)
                    _nullableInstance = ThreadedEventBus()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance = null
            }
        }
    }

    private val _events =
        hashMapOf<KClass<*>, MutableSet<Pair<suspend (Any) -> Unit, CoroutineScope>>>()

    private val _eventsMutex = SmartMutex()

    @OptIn(DelicateCoroutinesApi::class)
    fun <T : Any> subscribe(
        event: KClass<T>,
        callback: suspend (T) -> Unit,
        scope: CoroutineScope = ThreadManager.LAZY_INSTANCE.globalCoroutineScope
    ) {
        _eventsMutex.smartLock {
            if (_events[event] == null)
                _events[event] = mutableSetOf()

            _events[event]?.add(Pair(callback as suspend (Any) -> Unit, scope))
        }
    }

    fun <T : Any> invoke(event: T): T {
        val callbacks =
            _eventsMutex.smartLock {
                _events[event::class]
            }

        if (callbacks == null)
            return event

        val coroutine = arrayListOf<Job>()

        for (i in callbacks) {
            coroutine.add(i.second.launch {
                i.first.invoke(event)
            })
        }

        for (i in coroutine) {
            runBlocking {
                i.join()
            }
        }

        return event
    }
}