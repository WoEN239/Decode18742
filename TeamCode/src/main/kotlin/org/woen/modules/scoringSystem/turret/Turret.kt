package org.woen.modules.scoringSystem.turret


import com.acmerobotics.roadrunner.clamp
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.RequireOdometryEvent
import org.woen.telemetry.configs.Configs
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.process.Process
import org.woen.utils.units.Angle
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan


class RequestTurretCurrentRotation(var rotation: Angle = Angle.ZERO) : StoppingEvent

class SetRotateStateEvent(val rotateState: Turret.RotateState)

class RequestRotateStateEvent(var state: Turret.RotateState) : StoppingEvent

data class RequestTurretAtTarget(var atTarget: Boolean = false): StoppingEvent
data class WaitTurretAtTarget(val process: Process = Process())

data class RequestPulleyAtTarget(var atTarget: Boolean = false): StoppingEvent
data class WaitPulleyAtTarget(val process: Process = Process())

data class RequestRotateAtTarget(var atTarget: Boolean = false): StoppingEvent
data class WaitRotateAtTarget(val process: Process = Process())

class Turret : IModule {
    private val _hardwareTurret = HardwareTurret()
    private val _hardwareTurretServos = HardwareTurretServos()

    enum class RotateState {
        TO_BASKET,
        TO_OBELISK,
        CONSTANT
    }

    private var _turretJob: Job? = null

    private var _currentRotateState = RotateState.TO_BASKET

    private val _timer = ElapsedTime()

    override suspend fun process() {
        _turretJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            val basketErr =
                HotRun.LAZY_INSTANCE.currentStartPosition.basketPosition - (odometry.odometryOrientation.pos + Configs.TURRET.TURRET_CENTER_POS.turn(
                    odometry.odometryOrientation.angle
                ))

            _hardwareTurretServos.targetRotatePosition = Angle(
                when (_currentRotateState) {
                    RotateState.TO_BASKET ->
                        (basketErr.rot() - odometry.odometryOrientation.angle)

                    RotateState.CONSTANT -> 0.0

                    RotateState.TO_OBELISK -> ((Configs.TURRET.OBELISK_POSITION - (odometry.odometryOrientation.pos + Configs.TURRET.TURRET_CENTER_POS.turn(
                        odometry.odometryOrientation.angle
                    ))).rot()
                            - odometry.odometryOrientation.angle)
                }
            ).angle

            val y = Configs.TURRET.SCORE_HEIGHT - Configs.TURRET.TURRET_HEIGHT
            val x = basketErr.length()

            val alpha = atan((2 * y / x) - tan(Configs.TURRET.SCORE_ANGLE))

            val v0 =
                sqrt((Configs.TURRET.GRAVITY_G * x.pow(2)) / (2 * cos(alpha).pow(2) * (x * tan(alpha) - y)))

            val t = x / (v0 * cos(alpha))

            val robotGlobalVelocity =
                odometry.odometryVelocity.turn(odometry.odometryOrientation.angle)

            val robotV = robotGlobalVelocity.length()
            val difH = robotGlobalVelocity.rot() - basketErr.rot()

            val vR = -cos(difH) * robotV
            val vT = sin(difH) * robotV

            val vxComp = x / t + vR

            val vXNew = sqrt(vxComp.pow(2) + vT.pow(2))
            val vY = v0 * sin(alpha)

            val newX = vXNew * t

            _hardwareTurretServos.anglePosition = atan(vY / vXNew)
            _hardwareTurret.targetVelocity = clamp(sqrt(
                (Configs.TURRET.GRAVITY_G * newX.pow(2)) / (2.0 * cos(_hardwareTurretServos.anglePosition)
                    .pow(2)
                        * (newX * tan(_hardwareTurretServos.anglePosition) - y))
            ) / Configs.TURRET.PULLEY_U, 0.0, 17.0)
        }
    }

    override val isBusy: Boolean
        get() = _turretJob != null && !_turretJob!!.isCompleted

    override fun opModeStart() {
        _currentRotateState = RotateState.TO_BASKET

        _timer.reset()
    }

    override fun opModeStop() {

    }

    override fun dispose() {
        _turretJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareTurret)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareTurretServos)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestTurretCurrentRotation::class, {
            it.rotation = Angle(_hardwareTurretServos.currentRotatePosition)
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetRotateStateEvent::class, {
            _currentRotateState = it.rotateState
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestRotateStateEvent::class, {
            it.state = _currentRotateState
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestTurretAtTarget::class, {
            it.atTarget = _hardwareTurret.pulleyAtTarget && _hardwareTurretServos.rotateAtTarget
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(WaitTurretAtTarget::class, {
            while (!_hardwareTurret.pulleyAtTarget || !_hardwareTurretServos.rotateAtTarget)
                delay(5)

            it.process.close()
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestRotateAtTarget::class, {
            it.atTarget = _hardwareTurretServos.rotateAtTarget
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(WaitRotateAtTarget::class, {
            while (!_hardwareTurretServos.rotateAtTarget)
                delay(5)

            it.process.wait()
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestPulleyAtTarget::class, {
            it.atTarget = _hardwareTurret.pulleyAtTarget
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(WaitPulleyAtTarget::class, {
            while (!_hardwareTurret.pulleyAtTarget)
                delay(5)

            it.process.close()
        })
    }
}