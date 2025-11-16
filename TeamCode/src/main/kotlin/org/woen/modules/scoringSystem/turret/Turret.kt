package org.woen.modules.scoringSystem.turret

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.telemetry.Configs
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.process.Process
import java.util.concurrent.atomic.AtomicReference

data class RequestTurretAtTargetEvent(var atTarget: Boolean = false) : StoppingEvent
data class RequestCurrentTurretStateEvent(
    var state: Turret.TurretState = Turret.TurretState.STOP,
    var pulleyState: Turret.PulleyState
) :
    StoppingEvent

class SetCurrentTurretStateEvent(
    val state: Turret.TurretState,
    val targetProcess: Process = Process(),
    val pulleyState: Turret.PulleyState = Turret.PulleyState.LONG
)

class Turret : IModule {
    private val _hardwareTurret = HardwareTurret("pulleyMotor", "turretAngleServo")

    enum class TurretState {
        STOP,
        SHOOT,
        WAITING
    }

    enum class PulleyState {
        LONG,
        SHORT
    }

    private var _turretJob: Job? = null

    private var _currentTurretState = AtomicReference(TurretState.STOP)

    private var _targetProcess = AtomicReference(Process())

    private var _currentPulleyState = PulleyState.LONG

    override suspend fun process() {
        _turretJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if (_hardwareTurret.velocityAtTarget.get())
                _targetProcess.get().close()

            if (_currentTurretState.get() != TurretState.SHOOT)
                return@launch

            _hardwareTurret.targetVelocity = calculatePulleySpeed()
        }
    }

    private fun calculatePulleySpeed(): Double {
        if (_currentPulleyState == PulleyState.LONG) {
            _hardwareTurret.anglePosition = Configs.TURRET.LONG_ANGLE

            return Configs.TURRET.LONG_PULLEY_SPEED
        }

        _hardwareTurret.anglePosition = Configs.TURRET.SHORT_ANGLE

        return Configs.TURRET.SHORT_PULLEY_SPEED

//        val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())
//
//        val shootDistance =
//            (odometry.odometryOrientation.pos + Configs.TURRET.TURRET_SHOOT_POS.turn(odometry.odometryOrientation.angle)
//                    - HotRun.LAZY_INSTANCE.currentRunColor.get()
//                .basketPosition).length()
//
//        val shootingAngle = clamp(
//            (shootDistance / Configs.TURRET.MAX_SHOOTING_DISTANCE) * (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) + Configs.TURRET.MIN_TURRET_ANGLE,
//            Configs.TURRET.MIN_TURRET_ANGLE, Configs.TURRET.MAX_TURRET_ANGLE
//        )
//
//        val robotRotationBasketErr = Angle(
//            (HotRun.LAZY_INSTANCE.currentRunColor.get().basketPosition
//                    - odometry.odometryOrientation.pos).rot()
//                    - odometry.odometryOrientation.angle
//        ).angle
//
//        val robotXVel = odometry.odometryVelocity.turn(robotRotationBasketErr).x
//
//        fun getHitHeight(startVel: Double): Double {
//            var vecVel = Vec2(startVel * Configs.TURRET.PULLEY_U, 0.0).setRot(shootingAngle)
//            vecVel += robotXVel
//            var pos = Vec2.ZERO
//
//            while (pos.x < shootDistance) {
//                vecVel -= (Vec2(
//                    vecVel.x * vecVel.x,
//                    vecVel.y * vecVel.y
//                ) * Vec2(
//                    Configs.TURRET.AIR_FORCE_K / Configs.TURRET.BALL_MASS
//                ) + Vec2(0.0, Configs.TURRET.CALCULATING_G)) * Configs.TURRET.TIME_STEP
//
//                pos += vecVel * Vec2(Configs.TURRET.TIME_STEP)
//            }
//
//            return pos.y
//        }
//
//        var left = Configs.TURRET.MIN_APPROXIMATION
//        var right = Configs.TURRET.MAX_MOTOR_RPS * (2.0 * PI * Configs.TURRET.PULLEY_RADIUS)
//
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
//
//        return (left + right) / 2.0
    }

    override val isBusy: Boolean
        get() = _turretJob != null && !_turretJob!!.isCompleted

    override fun dispose() {
        _turretJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareTurret)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestTurretAtTargetEvent::class, {
            it.atTarget = _hardwareTurret.velocityAtTarget.get()
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestCurrentTurretStateEvent::class, {
            it.state = _currentTurretState.get()
            it.pulleyState = _currentPulleyState
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetCurrentTurretStateEvent::class, {
            _currentPulleyState = it.pulleyState
            _currentTurretState.set(it.state)

            _targetProcess.set(it.targetProcess)

            when (_currentTurretState.get()) {
                TurretState.STOP -> _hardwareTurret.targetVelocity = 0.0
                TurretState.SHOOT -> _hardwareTurret.targetVelocity = calculatePulleySpeed()
                TurretState.WAITING -> _hardwareTurret.targetVelocity =
                    Configs.TURRET.WAIT_PULLEY_SPEED
            }
        })

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            ThreadedEventBus.LAZY_INSTANCE.invoke(SetCurrentTurretStateEvent(TurretState.WAITING))
        }

        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            ThreadedEventBus.LAZY_INSTANCE.invoke(SetCurrentTurretStateEvent(TurretState.STOP))
        }
    }
}