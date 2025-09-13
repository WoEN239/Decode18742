package org.woen.modules.turret

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.threading.ThreadManager
import org.woen.threading.hardware.HardwareThreads

class Turret: IModule {
    private val _hardwareTurret = HardwareTurret("turretMotor")

    init {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareTurret)
    }

    private var _turretJob: Job? = null

    override suspend fun process() {
        _turretJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {

        }
    }

    override val isBusy: Boolean
        get() = _turretJob == null || _turretJob!!.isCompleted

    override fun dispose() {
        _turretJob?.cancel()
    }
}