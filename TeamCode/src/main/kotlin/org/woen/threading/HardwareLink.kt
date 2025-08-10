package org.woen.threading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import java.util.concurrent.Executors

class HardwareLink(private val _modules: Array<IModule<Any>>): DisposableHandle {
    private val _coroutineExecutor = Executors.newSingleThreadExecutor()
    val coroutineDispatcher = _coroutineExecutor.asCoroutineDispatcher()
    val coroutineScope = CoroutineScope(coroutineDispatcher + Job())

    fun getDevices(): Array<IHardwareDevice>{
        val devices = arrayListOf<IHardwareDevice>()

        for(i in _modules)
            devices.addAll(i.getDevices())

        return devices.toTypedArray()
    }

    fun updateModules(): Job{
        return coroutineScope.launch {
            for(i in _modules) {
                if(!i.isBusy())
                    i.process(i.getData())
            }
        }
    }

    override fun dispose() {
        for(i in _modules)
            i.dispose()

        _coroutineExecutor.shutdown()
        coroutineScope.cancel()
    }
}