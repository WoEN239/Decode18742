package org.woen.modules.turret

import barrel.enumerators.BallRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.process.Process

data class TurretShootEvent(
    val ball: BallRequest.Name,
    var isSuccessful: Boolean = false,
    val process: Process = Process()
)

data class TurretShootPatternEvent(
    val pattern: Pattern,
    var isSuccessful: Boolean = false,
    val process: Process = Process()
)

data class TurretShootAllBarrelEvent(val process: Process = Process())

data class RequireBallEvent(val ball: BallRequest.Name, val process: Process = Process())

class Turret : IModule {
    private val _hardwareTurret = HardwareTurret("turretMotor")

    init {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareTurret)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(TurretShootEvent::class, {

        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(TurretShootPatternEvent::class, {
            
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(TurretShootAllBarrelEvent::class, {

        })
    }

    private var _turretJob: Job? = null

    override suspend fun process() {

    }

    override val isBusy: Boolean
        get() = _turretJob == null || _turretJob!!.isCompleted

    override fun dispose() {
        _turretJob?.cancel()
    }
}