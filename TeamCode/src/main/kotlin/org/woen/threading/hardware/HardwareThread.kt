package org.woen.threading.hardware

import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun
import org.woen.hotRun.HotRun.RunState.STOP
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.updateCounter.UpdateCounter
import kotlin.concurrent.thread

class HardwareThread(val link: HardwareLink) : DisposableHandle {
    private val _devices = mutableSetOf<IHardwareDevice>()

    private val _updateCounter = UpdateCounter()

    private val _devicesMutex = SmartMutex()

    fun addDevices(vararg devices: IHardwareDevice) {
        val hardwareMap =
            OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).hardwareMap

        for (i in devices) {
            i.init(hardwareMap)

            _devicesMutex.smartLock {
                _devices.add(i)
            }
        }
    }

    fun removeDevices(vararg devices: IHardwareDevice) {
        for (i in devices) {
            _devicesMutex.smartLock {
                if (_devices.contains(i))
                    _devices.remove(i)
            }
        }
    }

    private val _thread = ThreadManager.Companion.LAZY_INSTANCE.register(thread(start = true) {
        var lastJob: Job? = null

        while (!Thread.currentThread().isInterrupted) {
            if (HotRun.LAZY_INSTANCE.currentRunState.get() == STOP) {
                Thread.sleep(10)
                continue
            }

            _devicesMutex.smartLock {
                for (i in _devices)
                    i.update()
            }

            _updateCounter.update()

            if (lastJob == null || lastJob.isCompleted) {
                lastJob = link.update()
            }
        }
    })

    override fun dispose() {
        _devicesMutex.smartLock {
            for (i in _devices)
                i.dispose()
        }

        link.dispose()
    }

    init {
        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("hardware ups + " + _thread.name, _updateCounter.currentUPS)
        }
    }
}