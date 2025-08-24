package org.woen.threading

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlin.concurrent.thread

class HardwareThread(val link: HardwareLink): DisposableHandle  {
    private val _devices = arrayListOf<IHardwareDevice>()

    fun addDevices(vararg devices: IHardwareDevice){
        _devices.addAll(devices)
    }

    private val _thread = thread(start = true) {
        var lastJob: Job? = null

        for(i in _devices)
            i.init()

        while (!Thread.currentThread().isInterrupted){
            for(i in _devices)
                i.update()

            if(lastJob == null || lastJob.isCompleted)
                lastJob = link.update()
        }
    }

    override fun dispose() {
        _thread.interrupt()

        for(i in _devices)
            i.dispose()
    }
}