package org.woen.threading.hardware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.modules.IModule
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

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