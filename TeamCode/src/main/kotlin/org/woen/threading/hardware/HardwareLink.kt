package org.woen.threading.hardware

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.threading.ThreadManager
import java.util.concurrent.CopyOnWriteArrayList

class HardwareLink : DisposableHandle {
    private val _modules = CopyOnWriteArrayList<IModule>()

    fun addModules(vararg modules: IModule) {
        _modules.addAll(modules)
    }

    fun update(): Job = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
        for (i in _modules) {
            if (!i.isBusy)
                i.process()
        }
    }

    override fun dispose() {
        for (i in _modules)
            i.dispose()
    }
}