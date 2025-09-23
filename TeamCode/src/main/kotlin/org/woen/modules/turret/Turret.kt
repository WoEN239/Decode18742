package org.woen.modules.turret

import barrel.enumerators.BallRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.RequestLookModeEvent
import org.woen.modules.driveTrain.odometry.RequireOdometryEvent
import org.woen.telemetry.Configs
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

data class TurretShootAllBarrelEvent(
    val process: Process = Process(),
    var isSuccessful: Boolean = false
)

data class RequireBallEvent(val ball: BallRequest.Name, val process: Process = Process())

class Turret : IModule {
    private val _hardwareTurret = HardwareTurret("turretMotor")

    init {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareTurret)
    }

    private var _turretJob: Job? = null

    override suspend fun process() {
        _turretJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if (!ThreadedEventBus.LAZY_INSTANCE.invoke(RequestLookModeEvent()).enable) {
                _hardwareTurret.targetVelocity = Configs.TURRET.QUIET_PULLEY_SPEED

                return@launch
            }

            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            (odometry.odometryOrientation.pos - HotRun.LAZY_INSTANCE.currentRunColor.get()
                .getBasketPosition()).length()

            Configs.TURRET.BASKET_TARGET_HEIGHT - Configs.TURRET.TURRET_HEIGHT
        }
    }

    override val isBusy: Boolean
        get() = _turretJob == null || _turretJob!!.isCompleted

    override fun dispose() {
        _turretJob?.cancel()
    }
}