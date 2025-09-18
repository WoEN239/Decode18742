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
import java.util.concurrent.Executors

class HardwareLink : DisposableHandle {
    companion object {
        private val _coroutineExecutor = Executors.newSingleThreadExecutor()
        private val _coroutineDispatcher = _coroutineExecutor.asCoroutineDispatcher()
        private val _coroutineScope = CoroutineScope(_coroutineDispatcher + Job())
    }

    private val _modules = mutableSetOf<IModule>()

    fun addModules(vararg modules: IModule) {
        runBlocking {
            _modulesMutex.withLock {
                ThreadedTelemetry.LAZY_INSTANCE.log(_modules.size.toString())
                _modules.addAll(modules)
            }
        }
    }

    private val _modulesMutex = Mutex()

    fun update(): Job = _coroutineScope.launch {
        _modulesMutex.withLock {
            for (i in _modules) {
                ThreadedTelemetry.LAZY_INSTANCE.log(i.isBusy.toString())
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

                _coroutineExecutor.shutdown()
            }
        }
    }
}