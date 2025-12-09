package org.woen.modules.scoringSystem.turret

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.RequireOdometryEvent
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Angle
import org.woen.utils.units.Vec2
import kotlin.math.PI
import kotlin.math.pow

class SetTurretMode(val mode: Turret.TurretMode)

class CurrentlyShooting()
class TurretVoltageDropped()


class Turret : IModule {
    private val _hardwareTurret = HardwareTurret()

    enum class TurretState {
        STOP,
        SHOOT
    }

    enum class TurretMode {
        SHORT,
        LONG
    }

    private var _turretJob: Job? = null

    private var _currentTurretState = TurretState.STOP

    private var _isShooting = false

    private var _currentMode = TurretMode.SHORT

    override suspend fun process() {
        _turretJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if (_currentTurretState != TurretState.SHOOT)
                return@launch

           val aimPos = aim()

            _hardwareTurret.anglePosition =
                (aimPos - Configs.TURRET.MIN_TURRET_ANGLE) /
                        (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) *
                        (Configs.TURRET.MAX_TURRET_ANGLE_SERVO - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) +
                        Configs.TURRET.MIN_TURRET_ANGLE_SERVO

            ThreadedTelemetry.LAZY_INSTANCE.log((aimPos / PI * 180.0).toString())

            _hardwareTurret.currentVoltage = getTargetVoltage()

            if (_isShooting && _hardwareTurret.shotWasFired) {
                _isShooting = false
                ThreadedEventBus.LAZY_INSTANCE.invoke(TurretVoltageDropped())

                ThreadedTelemetry.LAZY_INSTANCE.log("\t!!!")
            }
        }
    }

    private fun getTargetVoltage(): Double {
        val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

        val shootDistance =
            (odometry.odometryOrientation.pos + Configs.TURRET.TURRET_SHOOT_POS.turn(odometry.odometryOrientation.angle)
                    - HotRun.LAZY_INSTANCE.currentRunColor
                .basketPosition).length()

        return shootDistance
    }

    private fun aim(): Double {
        if(_hardwareTurret.currentVelocity < Configs.TURRET.MINIMAL_SHOOT_VELOCITY)
            return 0.0

        val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

        val shootDistance =
            (odometry.odometryOrientation.pos + Configs.TURRET.TURRET_SHOOT_POS.turn(odometry.odometryOrientation.angle)
                    - HotRun.LAZY_INSTANCE.currentRunColor
                .basketPosition).length()

        val robotRotationBasketErr = Angle(
            (HotRun.LAZY_INSTANCE.currentRunColor.basketPosition
                    - odometry.odometryOrientation.pos).rot()
                    - odometry.odometryOrientation.angle
        ).angle

        val robotXVel = odometry.odometryVelocity.turn(robotRotationBasketErr).x

        fun getHitHeight(startVel: Double, angle: Double): Double {
            var vecVel = Vec2(startVel * Configs.TURRET.PULLEY_U, 0.0).setRot(angle)
            vecVel += robotXVel
            var pos = Vec2.ZERO

            while (pos.x < shootDistance && !Thread.currentThread().isInterrupted) {
                vecVel -= (Vec2(
                    vecVel.length()
                        .pow(2.0) * Configs.TURRET.AIR_FORCE_K / Configs.TURRET.BALL_MASS, 0.0
                ).setRot(vecVel.rot()) +
                        Vec2(0.0, Configs.TURRET.CALCULATING_G)) *
                        Configs.TURRET.TIME_STEP

                pos += vecVel * Configs.TURRET.TIME_STEP
            }

            return pos.y
        }

        var left = Configs.TURRET.MIN_TURRET_ANGLE
        var right = Configs.TURRET.MAX_TURRET_ANGLE

        var iterations = 0

        getHitHeight(_hardwareTurret.currentVelocity, (left + right) / 2.0)

        while (iterations < Configs.TURRET.APPROXIMATION_MAX_ITERATIONS && !Thread.currentThread().isInterrupted) {
            iterations++

            val middle = (left + right) / 2.0

            val dif =
                getHitHeight(_hardwareTurret.currentVelocity, middle) -
                        (Configs.TURRET.BASKET_TARGET_HEIGHT - Configs.TURRET.TURRET_HEIGHT)

            if (dif > 0.0)
                right = middle
            else
                left = middle
        }

        return (left + right) / 2.0
    }

    override val isBusy: Boolean
        get() = _turretJob != null && !_turretJob!!.isCompleted

    override fun dispose() {
        _turretJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareTurret)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(CurrentlyShooting::class, {
            _isShooting = true
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetTurretMode::class, {
            _currentMode = it.mode
        })

        fun setTurretState(state: TurretState) {
            _currentTurretState = state

            when (_currentTurretState) {
                TurretState.STOP -> _hardwareTurret.currentVoltage = 0.0
                TurretState.SHOOT -> _hardwareTurret.currentVoltage = getTargetVoltage()
            }
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            setTurretState(TurretState.SHOOT)
        }

        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            setTurretState(TurretState.STOP)
        }
    }
}