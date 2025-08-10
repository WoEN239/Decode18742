package org.woen.threading

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class HardwareThread(val link: HardwareLink): DisposableHandle  {
    private val _devices = link.getDevices()

    private val _thread = thread(start = true) {
        for(i in _devices)
            i.init()

        var lastCoroutineJob: Job? = null

        while (Thread.interrupted()){
            for(i in _devices)
                i.update()

            if (lastCoroutineJob?.isActive ?: true)
                lastCoroutineJob = link.updateModules()
        }

        for(i in _devices)
            i.dispose()
    }

    override fun dispose() {
        _thread.interrupt()
        link.dispose()
    }
}