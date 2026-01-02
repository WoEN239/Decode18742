package org.woen.modules.scoringSystem.turret


import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import org.woen.utils.units.Vec2
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin


class SetTurretMode(val mode: Turret.TurretMode)

class CurrentlyShooting()
class TurretCurrentPeaked()

class RequestTurretCurrentRotation(var rotation: Angle = Angle.ZERO) : StoppingEvent


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

        val turretPos =
            odometry.odometryOrientation.pos +
                    Configs.TURRET.TURRET_CENTER_POS.turn(odometry.odometryOrientation.angle) +
                    Configs.TURRET.TURRET_SHOOT_POS.turn(
                        odometry.odometryOrientation.angle + _hardwareTurret.currentRotatePosition
                    )

        val shootDistance =
            (turretPos - HotRun.LAZY_INSTANCE.currentRunColor.basketPosition).length()

        val robotRotationBasketErr = Angle(
            (HotRun.LAZY_INSTANCE.currentRunColor.basketPosition - turretPos).rot()
                    - odometry.odometryOrientation.angle
        ).angle

        _hardwareTurret.targetRotatePosition = robotRotationBasketErr

//        val robotXVel = odometry.odometryVelocity.turn(robotRotationBasketErr).x

        fun getHitHeight(startVel: Double, angle: Double): Double {
            var vecVel = Vec2(startVel * Configs.TURRET.PULLEY_U, 0.0).setRot(angle)
//            vecVel += robotXVel
            var pos = Vec2.ZERO

            while (pos.x < shootDistance && !Thread.currentThread().isInterrupted && pos.y > -1.0) {
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

        val calcVel = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val targetAngle =
                if (_currentMode == TurretMode.SHORT) Configs.TURRET.SHORT_ANGLE_POSITION else Configs.TURRET.LONG_ANGLE_POSITION

            _hardwareTurret.targetVelocity = approximation(
                Configs.TURRET.MINIMAL_PULLEY_VELOCITY,
                Configs.TURRET.MAX_MOTOR_RPS * 2.0 * PI * Configs.TURRET.PULLEY_RADIUS
            ) { getHitHeight(it, targetAngle) }
        }

        val calcAngle = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val turretVel = _hardwareTurret.targetVelocity

            _hardwareTurret.anglePosition =
                approximation(Configs.TURRET.MIN_TURRET_ANGLE, Configs.TURRET.MAX_TURRET_ANGLE)
                { getHitHeight(turretVel, it) }
        }

        runBlocking {
            calcVel.join()
            calcAngle.join()
        }
    }

    private fun approximation(min: Double, max: Double, func: (Double) -> Double): Double {
        var left = min
        var right = max

        var iterations = 0

        while (iterations < Configs.TURRET.APPROXIMATION_MAX_ITERATIONS && !Thread.currentThread().isInterrupted) {
            iterations++

            val middle = (left + right) / 2.0

            val dif =
                func(middle) - (Configs.TURRET.BASKET_TARGET_HEIGHT - Configs.TURRET.TURRET_HEIGHT)

            if (dif > 0.0)
                right = middle
            else
                left = middle
        }

        return (left + right) / 2.0
    }

    override val isBusy: Boolean
        get() = _turretJob != null && !_turretJob!!.isCompleted

    private fun setTurretState(state: TurretState) {
        _currentTurretState = state

        when (_currentTurretState) {
            TurretState.STOP -> _hardwareTurret.targetVelocity = 0.0
            TurretState.SHOOT -> calcTurretState()
        }
    }

    override fun opModeStart() {
        setTurretState(TurretState.SHOOT)

        _hardwareTurret.targetVelocity = 15.0
    }

    override fun opModeStop() {
        setTurretState(TurretState.STOP)
    }

    override fun dispose() {
        _turretJob?.cancel()
    }

//    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = true) {
//        val timer = ElapsedTime()
//
//        timer.reset()
//
//        while (!Thread.currentThread().isInterrupted){
//            _hardwareTurret.targetVelocity = (sin(timer.seconds() * 2.0) + 1.0) / 2.0 * 10.0 + 10.0
////
////            Thread.sleep(5000)
////
////            _hardwareTurret.targetVelocity = 20.0
////
////            Thread.sleep(5000)
//        }
//    })

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareTurret)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(CurrentlyShooting::class, {
            _isShooting = true
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetTurretMode::class, {
            _currentMode = it.mode
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestTurretCurrentRotation::class, {
            it.rotation = Angle(_hardwareTurret.currentRotatePosition)
        })
    }
}