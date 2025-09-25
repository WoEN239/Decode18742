package org.woen.modules.scoringSystem.turret

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.odometry.RequireOdometryEvent
import org.woen.telemetry.Configs
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.process.Process
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI

data class RequestTurretAtTargetEvent(var atTarget: Boolean = false) : StoppingEvent
data class RequestCurrentTurretStateEvent(var state: Turret.TurretState = Turret.TurretState.STOP) :
    StoppingEvent

class SetCurrentTurretStateEvent(
    val state: Turret.TurretState,
    val targetProcess: Process = Process()
)

class Turret : IModule {
    private val _hardwareTurret = HardwareTurret("turretMotor")

    enum class TurretState {
        STOP,
        SHOOT,
        WAITING
    }

    private var _turretJob: Job? = null

    private var _currentTurretState = AtomicReference(TurretState.STOP)

    private var _targetProcess = AtomicReference(Process())

    override suspend fun process() {
        _turretJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
                return@launch

            if (_hardwareTurret.velocityAtTarget.get())
                _targetProcess.get().close()

            if (_currentTurretState.get() != TurretState.SHOOT)
                return@launch

            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            val shootDistance =
                (odometry.odometryOrientation.pos - HotRun.LAZY_INSTANCE.currentRunColor.get()
                    .getBasketPosition()).length()

            fun getHitHeight(startVel: Double): Double {
                var vecVel = Vec2(startVel, 0.0).setRot(Configs.TURRET.TURRET_ANGLE)
                var pos = Vec2.ZERO

                while (pos.x < shootDistance) {
                    vecVel -= (Vec2(
                        vecVel.x * vecVel.x,
                        vecVel.y * vecVel.y
                    ) * Vec2(
                        Configs.TURRET.AIR_FORCE_K / Configs.TURRET.BALL_MASS
                    ) + Vec2(0.0, Configs.TURRET.CALCULATING_G)) * Configs.TURRET.TIME_STEP

                    pos += vecVel * Vec2(Configs.TURRET.TIME_STEP)
                }

                return pos.y
            }

            var left = Configs.TURRET.MIN_APPROXIMATION
            var right = Configs.TURRET.MAX_MOTOR_RPS * (2.0 * PI * Configs.TURRET.PULLEY_RADIUS)

            var iterations = 0

            while (iterations < Configs.TURRET.APPROXIMATION_MAX_ITERATIONS) {
                iterations++

                val middle = (left + right) / 2.0

                val dif =
                    getHitHeight(middle) - (Configs.TURRET.BASKET_TARGET_HEIGHT - Configs.TURRET.TURRET_HEIGHT)

                if (dif > 0.0)
                    right = middle
                else
                    left = middle
            }

            _hardwareTurret.targetVelocity = (left + right) / 2.0
        }
    }

    override val isBusy: Boolean
        get() = _turretJob == null || _turretJob!!.isCompleted

    override fun dispose() {
        _turretJob?.cancel()
    }

    init {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareTurret)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestTurretAtTargetEvent::class, {
            it.atTarget = _hardwareTurret.velocityAtTarget.get()
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestCurrentTurretStateEvent::class, {
            it.state = _currentTurretState.get()
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetCurrentTurretStateEvent::class, {
            _currentTurretState.set(it.state)

            _targetProcess.set(it.targetProcess)

            when (_currentTurretState.get()) {
                TurretState.STOP -> _hardwareTurret.targetVelocity = 0.0
                TurretState.SHOOT -> return@subscribe
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