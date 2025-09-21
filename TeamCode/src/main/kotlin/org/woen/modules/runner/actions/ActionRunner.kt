package org.woen.modules.runner.actions

import org.woen.hotRun.HotRun
import org.woen.threading.ThreadManager
import kotlin.concurrent.thread

class ActionRunner {
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
}