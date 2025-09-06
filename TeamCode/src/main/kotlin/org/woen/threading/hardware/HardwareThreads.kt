package org.woen.threading.hardware

import kotlinx.coroutines.DisposableHandle

class HardwareThreads private constructor(): DisposableHandle {
    companion object{
        private var _nullableInstance: HardwareThreads? = null

        @get:Synchronized
        val LAZY_INSTANCE : HardwareThreads
            get() {
                if(_nullableInstance == null)
                    _nullableInstance = HardwareThreads()

                return _nullableInstance!!
            }

        fun restart(){
            _nullableInstance?.dispose()
            _nullableInstance = null
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