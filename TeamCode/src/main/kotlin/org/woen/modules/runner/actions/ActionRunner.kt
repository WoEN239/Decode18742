package org.woen.modules.runner.actions

import kotlinx.coroutines.DisposableHandle
import org.woen.hotRun.HotRun
import org.woen.threading.ThreadManager
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.smartMutex.SmartMutex
import kotlin.concurrent.thread

class ActionRunner private constructor(): DisposableHandle {
    companion object{
        private var _nullableInstance: ActionRunner? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: ActionRunner
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null)
                    _nullableInstance = ActionRunner()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance?.dispose()
                _nullableInstance = null
            }
        }
    }

    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread {

    })

    init {
        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            if (HotRun.LAZY_INSTANCE.currentRunMode.get() == HotRun.RunMode.AUTO)
                _thread.start()
        }

        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            _thread.interrupt()
        }
    }

    override fun dispose() {
        _thread.stop()
    }
}