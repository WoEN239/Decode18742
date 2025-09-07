package org.woen.threading.hardware

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HardwareThreads private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: HardwareThreads? = null

        private val _instanceMutex = Mutex()

        val LAZY_INSTANCE: HardwareThreads
            get() =
                runBlocking {
                    _instanceMutex.withLock {
                        if (_nullableInstance == null)
                            _nullableInstance = HardwareThreads()

                        return@withLock _nullableInstance!!
                    }
                }


        fun restart() {
            runBlocking {
                _instanceMutex.withLock {
                    _nullableInstance?.dispose()
                    _nullableInstance = null
                }
            }
        }
    }

    val CONTROL = HardwareThread(HardwareLink())
    val EXPANSION = HardwareThread(HardwareLink())

    fun initModules() {

    }

    override fun dispose() {
        CONTROL.dispose()
        EXPANSION.dispose()
    }
}