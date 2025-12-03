package org.woen.modules.scoringSystem.turret

import androidx.core.math.MathUtils.clamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.RequireOdometryEvent
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.process.Process
import org.woen.utils.units.Angle
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI

data class RequestTurretAtTargetEvent(var atTarget: Boolean = false) : StoppingEvent
data class WaitTurretAtTargetEvent(val targetProcess: Process = Process()) : StoppingEvent

class CurrentlyShooting()
class TurretVoltageDropped()


class Turret : IModule {
    private val _hardwareTurret = HardwareTurret("pulleyMotor", "turretAngleServo")

    enum class TurretState {
        STOP,
        SHOOT
    }

    private var _turretJob: Job? = null

    private var _currentTurretState = AtomicReference(TurretState.STOP)

    private var _currentTargetProcess = Process()
    private var _isShooting = AtomicBoolean(false)



    override suspend fun process() {
        _turretJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if (_hardwareTurret.velocityAtTarget.get())
                _currentTargetProcess.close()

            if (_currentTurretState.get() != TurretState.SHOOT)
                return@launch

            _hardwareTurret.targetVelocity = calculatePulleySpeed()


            if (_isShooting.get() && _hardwareTurret.detShoot) {
                ThreadedEventBus.LAZY_INSTANCE.invoke(TurretVoltageDropped())
                _isShooting.set(false)
            }
        }
    }

    private fun calculatePulleySpeed(): Double {
        val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

        val shootDistance =
            (odometry.odometryOrientation.pos + Configs.TURRET.TURRET_SHOOT_POS.turn(odometry.odometryOrientation.angle)
                    - HotRun.LAZY_INSTANCE.currentRunColor.get()
                .basketPosition).length()

        val shootingAngle = clamp(
            (shootDistance / Configs.TURRET.MAX_SHOOTING_DISTANCE) * (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) + Configs.TURRET.MIN_TURRET_ANGLE,
            Configs.TURRET.MIN_TURRET_ANGLE, Configs.TURRET.MAX_TURRET_ANGLE
        )

        _hardwareTurret.anglePosition =
            (shootingAngle - Configs.TURRET.MIN_TURRET_ANGLE) /
                    (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) *
                    (Configs.TURRET.MAX_TURRET_ANGLE_SERVO - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) +
                    Configs.TURRET.MIN_TURRET_ANGLE_SERVO

        val robotRotationBasketErr = Angle(
            (HotRun.LAZY_INSTANCE.currentRunColor.get().basketPosition
                    - odometry.odometryOrientation.pos).rot()
                    - odometry.odometryOrientation.angle
        ).angle

        val robotXVel = odometry.odometryVelocity.turn(robotRotationBasketErr).x

        fun getHitHeight(startVel: Double): Double {
            var vecVel = Vec2(startVel * Configs.TURRET.PULLEY_U, 0.0).setRot(shootingAngle)
            vecVel += robotXVel
            var pos = Vec2.ZERO

            while (pos.x < shootDistance) {
                vecVel -= (Vec2(
                    vecVel.x * vecVel.x,
                    vecVel.y * vecVel.y
                ) * Vec2(
                    Configs.TURRET.AIR_FORCE_K / Configs.TURRET.BALL_MASS
                ) + Vec2(0.0, Configs.TURRET.CALCULATING_G)) * Configs.TURRET.TIME_STEP

                pos += vecVel * Vec2(Configs.TURRET.TIME_STEP)

                ThreadedTelemetry.LAZY_INSTANCE.log(pos.x.toString())
            }

            return pos.y
        }

        var left = Configs.TURRET.MIN_APPROXIMATION
        var right = Configs.TURRET.MAX_MOTOR_RPS * (2.0 * PI * Configs.TURRET.PULLEY_RADIUS)

//        var iterations = 0
//
//        while (iterations < Configs.TURRET.APPROXIMATION_MAX_ITERATIONS) {
//            iterations++
//
//            val middle = (left + right) / 2.0
//
//            val dif =
//                getHitHeight(middle) - (Configs.TURRET.BASKET_TARGET_HEIGHT - Configs.TURRET.TURRET_HEIGHT)
//
//            if (dif > 0.0)
//                right = middle
//            else
//                left = middle
//        }

        ThreadedTelemetry.LAZY_INSTANCE.log(getHitHeight((left + right) / 2.0).toString()) //infinity

        return (left + right) / 2.0
    }

    override val isBusy: Boolean
        get() = _turretJob != null && !_turretJob!!.isCompleted

    override fun dispose() {
        _turretJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareTurret)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestTurretAtTargetEvent::class, {
            it.atTarget = _hardwareTurret.velocityAtTarget.get()
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(WaitTurretAtTargetEvent::class, {
            _currentTargetProcess = it.targetProcess
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(CurrentlyShooting::class, {
            _isShooting.set(true)
        })

        fun setTurretState(state: TurretState) {
            _currentTurretState.set(state)

            when (_currentTurretState.get()) {
                TurretState.STOP -> _hardwareTurret.targetVelocity = 0.0
                TurretState.SHOOT -> _hardwareTurret.targetVelocity = calculatePulleySpeed()
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