package org.woen.modules.scoringSystem.turret


import kotlin.math.PI
import kotlin.math.pow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.woen.utils.units.Vec2
import org.woen.utils.process.Process

import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.RequireOdometryEvent

import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.HardwareThreads

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.TURRET
import org.woen.threading.StoppingEvent
import org.woen.utils.units.Angle


class SetTurretMode(val mode: Turret.TurretMode)
class RequestTurretAtTarget(var atTarget: Boolean = false): StoppingEvent
class WaitTurretAtTarget(var process: Process = Process())

class CurrentlyShooting()
class TurretCurrentPeaked()

class RequestTurretCurrentRotation(var rotation: Double): StoppingEvent


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

            calcTurretState()

            if (_isShooting && _hardwareTurret.shotWasFired) {
                _isShooting = false
                ThreadedEventBus.LAZY_INSTANCE.invoke(TurretCurrentPeaked())

                ThreadedTelemetry.LAZY_INSTANCE.log("\tTurret: current peaked")
            }
        }
    }

    private fun calcTurretState() {
        val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

        val shootDistance =
            (odometry.odometryOrientation.pos + TURRET.TURRET_SHOOT_POS.turn(odometry.odometryOrientation.angle)
                    - HotRun.LAZY_INSTANCE.currentRunColor
                .basketPosition).length()

        val robotRotationBasketErr = Angle(
            (HotRun.LAZY_INSTANCE.currentRunColor.basketPosition
                    - odometry.odometryOrientation.pos).rot()
                    - odometry.odometryOrientation.angle
        ).angle

        _hardwareTurret.targetRotatePosition = robotRotationBasketErr

        ThreadedTelemetry.LAZY_INSTANCE.log(robotRotationBasketErr.toString())

        val robotXVel = odometry.odometryVelocity.turn(robotRotationBasketErr).x

        fun getHitHeight(startVel: Double, angle: Double): Double {
            var vecVel = Vec2(startVel * TURRET.PULLEY_U, 0.0).setRot(angle)
            vecVel += robotXVel
            var pos = Vec2.ZERO

            while (pos.x < shootDistance && !Thread.currentThread().isInterrupted && pos.y > -1.0) {
                vecVel -= (Vec2(
                    vecVel.length()
                        .pow(2.0) * TURRET.AIR_FORCE_K / TURRET.BALL_MASS, 0.0
                ).setRot(vecVel.rot()) +
                        Vec2(0.0, TURRET.CALCULATING_G)) *
                        TURRET.TIME_STEP

                pos += vecVel * TURRET.TIME_STEP
            }

            return pos.y
        }

        val targetAngle =
            if (_currentMode == TurretMode.SHORT) TURRET.SHORT_ANGLE_POSITION else TURRET.LONG_ANGLE_POSITION

        _hardwareTurret.targetVelocity = approximation(
            TURRET.MINIMAL_PULLEY_VELOCITY,
            TURRET.MAX_MOTOR_RPS * 2.0 * PI * TURRET.PULLEY_RADIUS
        ) { getHitHeight(it, targetAngle) }

        val turretVel = _hardwareTurret.targetVelocity

        _hardwareTurret.anglePosition = approximation(TURRET.MIN_TURRET_ANGLE, TURRET.MAX_TURRET_ANGLE)
        { getHitHeight(turretVel, it)}
    }

    private fun approximation(min: Double, max: Double, func: (Double) -> Double): Double {
        var left = min
        var right = max

        var iterations = 0

        while (iterations < TURRET.APPROXIMATION_MAX_ITERATIONS && !Thread.currentThread().isInterrupted) {
            iterations++

            val middle = (left + right) / 2.0

            val dif =
                func(middle) - (TURRET.BASKET_TARGET_HEIGHT - TURRET.TURRET_HEIGHT)

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
                TurretState.STOP -> _hardwareTurret.targetVelocity = 0.0
                TurretState.SHOOT -> calcTurretState()
            }
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            setTurretState(TurretState.SHOOT)
        }

        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            setTurretState(TurretState.STOP)
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestTurretAtTarget::class, {
            it.atTarget = _hardwareTurret.velocityAtTarget
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(WaitTurretAtTarget::class, {
            while (!_hardwareTurret.velocityAtTarget && !Thread.currentThread().isInterrupted)
                delay(DELAY.EVENT_AWAITING_MS)

            it.process.close()
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestTurretCurrentRotation::class, {
            it.rotation = _hardwareTurret.currentRotatePosition
        })
    }
}