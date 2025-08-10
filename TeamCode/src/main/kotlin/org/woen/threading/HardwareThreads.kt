package org.woen.threading

import kotlinx.coroutines.DisposableHandle
import org.woen.modules.IModule

class HardwareThreads private constructor(): DisposableHandle {
    companion object{
        var INSTANCE = HardwareThreads()

        fun restart(){
            INSTANCE.dispose()
            INSTANCE = HardwareThreads()
        }
    }

    val CONTROL = HardwareThread(HardwareLink(arrayOf()))
    val EXPANSION = HardwareThread(HardwareLink(arrayOf()))

    override fun dispose() {
        CONTROL.dispose()
        EXPANSION.dispose()
    }
}