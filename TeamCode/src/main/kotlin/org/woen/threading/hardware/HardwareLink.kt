package org.woen.threading.hardware

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.threading.ThreadManager
import org.woen.utils.smartMutex.SmartMutex

class HardwareLink : DisposableHandle {
    private val _modules = mutableSetOf<IModule>()

    private val _modulesMutex = SmartMutex()

    fun addModules(vararg modules: IModule) {
        _modulesMutex.smartLock {
            _modules.addAll(modules)
        }
    }

    fun update(): Job = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
        _modulesMutex.smartLock {
            for (i in _modules) {
                if (!i.isBusy)
                    i.process()
            }
        }
    }

    override fun dispose() {
        _modulesMutex.smartLock {
            for (i in _modules)
                i.dispose()
        }
    }
}