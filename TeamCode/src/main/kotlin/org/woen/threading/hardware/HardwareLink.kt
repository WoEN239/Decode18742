package org.woen.threading.hardware

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.threading.ThreadManager
import java.util.concurrent.CopyOnWriteArrayList

class HardwareLink : DisposableHandle {
    private val _modules = CopyOnWriteArrayList<IModule>()

    private var _isStopped = true

    fun addModules(vararg modules: IModule) {
        _modules.addAll(modules)
    }

    fun update(): Job = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
        if(_isStopped)
            return@launch

        for (i in _modules) {
            if (!i.isBusy)
                i.process()
        }
    }

    constructor(){
        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            for(i in _modules)
                i.opModeStart()

            _isStopped = false
        }

        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            _isStopped = true

            for(i in _modules)
                i.opModeStop()
        }
    }

    override fun dispose() {
        for (i in _modules)
            i.dispose()
    }
}