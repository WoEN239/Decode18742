package org.woen.threading.hardware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import java.util.concurrent.Executors

class HardwareLink: DisposableHandle{
    companion object{
        private val _coroutineExecutor = Executors.newSingleThreadExecutor()
        private val _coroutineDispatcher = _coroutineExecutor.asCoroutineDispatcher()
        private val _coroutineScope = CoroutineScope(_coroutineDispatcher + Job())
    }

    private val _modules = mutableSetOf<IModule>()

    fun addModules(vararg modules: IModule){
        _modules.addAll(modules)
    }

    fun update(): Job = _coroutineScope.launch {
        for(i in _modules){
            if(!i.isBusy())
                i.process()
        }
    }

    override fun dispose() {
        _coroutineExecutor.shutdown()
    }
}