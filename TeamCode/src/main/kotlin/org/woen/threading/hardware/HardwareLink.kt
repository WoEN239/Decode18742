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
import java.util.concurrent.Executors

class HardwareLink : DisposableHandle {
    private val _modules = mutableSetOf<IModule>()

    fun addModules(vararg modules: IModule) {
        runBlocking {
            _modulesMutex.withLock {
                _modules.addAll(modules)
            }
        }
    }

    private val _modulesMutex = Mutex()

    fun update(): Job = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
        _modulesMutex.withLock {
            for (i in _modules) {
                if (!i.isBusy)
                    i.process()
            }
        }
    }

    override fun dispose() {
        runBlocking {
            _modulesMutex.withLock {
                for (i in _modules)
                    i.dispose()
            }
        }
    }
}