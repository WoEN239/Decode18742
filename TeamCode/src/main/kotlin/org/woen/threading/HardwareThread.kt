package org.woen.threading

import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import kotlin.concurrent.thread

class HardwareThread(val link: HardwareLink) : DisposableHandle {
    private val _devices = mutableSetOf<IHardwareDevice>()

    private val _devicesMutex = Mutex()

    fun addDevices(vararg devices: IHardwareDevice) {
        runBlocking {
            _devicesMutex.withLock {
                _devices.addAll(devices)
            }
        }
    }

    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = true) {
        var lastJob: Job? = null

        val hardwareMap =
            OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).hardwareMap

        runBlocking {
            _devicesMutex.withLock {
                for (i in _devices)
                    i.init(hardwareMap)

            }
        }

        while (!Thread.currentThread().isInterrupted) {
            runBlocking {
                _devicesMutex.withLock {
                    for (i in _devices)
                        i.update()
                }
            }

            if (lastJob == null || lastJob.isCompleted)
                lastJob = link.update()
        }
    })

    override fun dispose() {
        for (i in _devices)
            i.dispose()
    }
}