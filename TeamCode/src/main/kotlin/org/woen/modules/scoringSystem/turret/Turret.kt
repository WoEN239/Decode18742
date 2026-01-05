package org.woen.modules.scoringSystem.turret


import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.enumerators.Shooting.ShotType
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.RequireOdometryEvent
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Angle


class SetTurretMode(val mode: Turret.TurretMode)

class CurrentlyShooting()
class TurretCurrentPeaked()

class RequestTurretCurrentRotation(var rotation: Angle = Angle.ZERO) : StoppingEvent

class SetTurretShootTypeEvent(val type: ShotType)


class Turret : IModule {
    private val _hardwareTurret = HardwareTurret()
    private val _hardwareTurretServos = HardwareTurretServos()

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

    private var _currentShootType = ShotType.DRUM

    private var _isRotateZeroed = false

    override suspend fun process() {
        _turretJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if (_currentTurretState != TurretState.SHOOT)
                return@launch

//            calcTurretState()

            updateTurret()

            if (_isShooting && _hardwareTurret.shotWasFired) {
                _isShooting = false
                ThreadedEventBus.LAZY_INSTANCE.invoke(TurretCurrentPeaked())

                ThreadedTelemetry.LAZY_INSTANCE.log("\tTurret: current peaked")
            }
        }
    }

//    private fun calcTurretState() {
//        val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())
//
//        val turretPos =
//            odometry.odometryOrientation.pos +
//                    (Configs.TURRET.TURRET_CENTER_POS +
//                            Configs.TURRET.TURRET_SHOOT_POS.turn(_hardwareTurret.currentRotatePosition)).turn(odometry.odometryOrientation.angle)
//
//        val shootDistance =
//            (turretPos - HotRun.LAZY_INSTANCE.currentStartPosition.basketPosition).length()
//
//        val robotRotationBasketErr = Angle(
//            (HotRun.LAZY_INSTANCE.currentStartPosition.basketPosition - turretPos).rot()
//                    - odometry.odometryOrientation.angle
//        ).angle
//
//        _hardwareTurret.targetRotatePosition = robotRotationBasketErr
//
////        val robotXVel = odometry.odometryVelocity.turn(robotRotationBasketErr).x
//
//        fun getHitHeight(startVel: Double, angle: Double): Double {
//            var vecVel = Vec2(
//                startVel * (if (_currentShootType == ShotType.SINGLE) Configs.TURRET.PULLEY_SOLO_U else Configs.TURRET.PULLEY_DRUM_U),
//                0.0
//            ).setRot(angle)
////            vecVel += robotXVel
//            var pos = Vec2.ZERO
//
//            while (pos.x < shootDistance && !Thread.currentThread().isInterrupted && pos.y > -1.0) {
//                vecVel -= (Vec2(
//                    vecVel.length()
//                        .pow(2.0) * (if (_currentShootType == ShotType.SINGLE) Configs.TURRET.SOLO_AIR_FORCE_K else Configs.TURRET.DRUM_AIR_FORCE_K) / Configs.TURRET.BALL_MASS,
//                    0.0
//                ).setRot(vecVel.rot()) +
//                        Vec2(0.0, Configs.TURRET.CALCULATING_G)) *
//                        Configs.TURRET.TIME_STEP
//
//                pos += vecVel * Configs.TURRET.TIME_STEP
//            }
//
//            return pos.y
//        }
//
//        val targetAngle =
//            ((turretPos - HotRun.LAZY_INSTANCE.currentStartPosition.basketPosition).length()
//                    / Configs.TURRET.MAX_SHOOTING_DISTANCE) * (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) +
//                    Configs.TURRET.MIN_TURRET_ANGLE
//
//        val calcVel = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
//            _hardwareTurret.targetVelocity = approximation(
//                Configs.TURRET.MINIMAL_PULLEY_VELOCITY,
//                Configs.TURRET.MAX_MOTOR_RPS * 2.0 * PI * Configs.TURRET.PULLEY_RADIUS
//            ) { getHitHeight(it, targetAngle) }
//        }

//        val calcAngle = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
//            val turretVel = _hardwareTurret.targetVelocity
//
//            _hardwareTurret.anglePosition =
//                approximation(Configs.TURRET.MIN_TURRET_ANGLE, Configs.TURRET.MAX_TURRET_ANGLE)
//                { getHitHeight(turretVel, it) }
//        }
//
//        _hardwareTurret.anglePosition = targetAngle
//
//        runBlocking {
//            calcVel.join()
////            calcAngle.join()
//        }
//    }

//    private fun approximation(min: Double, max: Double, func: (Double) -> Double): Double {
//        var left = min
//        var right = max
//
//        var iterations = 0
//
//        while (iterations < Configs.TURRET.APPROXIMATION_MAX_ITERATIONS && !Thread.currentThread().isInterrupted) {
//            iterations++
//
//            val middle = (left + right) / 2.0
//
//            val dif =
//                func(middle) - (Configs.TURRET.BASKET_TARGET_HEIGHT - Configs.TURRET.TURRET_HEIGHT)
//
//            if (dif > 0.0)
//                right = middle
//            else
//                left = middle
//        }
//
//        return (left + right) / 2.0
//    }

    fun updateTurret() {
        if (_currentShootType == ShotType.DRUM)
            _hardwareTurret.targetVelocity = Configs.TURRET.SHOOTING_DRUM_PULLEY_VELOCITY
        else
            _hardwareTurret.targetVelocity = Configs.TURRET.SHOOTING_SINGLE_PULLEY_VELOCITY

        _hardwareTurretServos.rawAnglePosition = Configs.TURRET.SHOOTING_ANGLE_POSITION

        if (_isRotateZeroed) {
            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            _hardwareTurretServos.targetRotatePosition = Angle(
                (HotRun.LAZY_INSTANCE.currentStartPosition.basketPosition - Configs.TURRET.TURRET_CENTER_POS.turn(
                    odometry.odometryOrientation.angle
                )).rot()
                        - odometry.odometryOrientation.angle
            ).angle
        }
    }

    override val isBusy: Boolean
        get() = _turretJob != null && !_turretJob!!.isCompleted

    private fun setTurretState(state: TurretState) {
        _currentTurretState = state

        when (_currentTurretState) {
            TurretState.STOP -> _hardwareTurret.targetVelocity = 0.0
            TurretState.SHOOT -> updateTurret()
        }
    }

    override fun opModeStart() {
        setTurretState(TurretState.SHOOT)

        if (!_isRotateZeroed) {
            ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                _hardwareTurretServos.rawRotatePosition = Configs.TURRET.ZERO_ROTATE_POS

                delay((Configs.TURRET.ZEROING_TIME * 1000.0).toLong())

                _hardwareTurret.resetRotateEncoder()
                _isRotateZeroed = true
            }
        }
    }

    override fun opModeStop() {
        setTurretState(TurretState.STOP)
    }

    override fun dispose() {
        _turretJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareTurret)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareTurretServos)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(CurrentlyShooting::class, {
            _isShooting = true
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetTurretMode::class, {
            _currentMode = it.mode
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestTurretCurrentRotation::class, {
            it.rotation = Angle(if (_isRotateZeroed) _hardwareTurret.currentRotatePosition else 0.0)
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetTurretShootTypeEvent::class, {
            _currentShootType = it.type
        })
    }
}