package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.scoringSystem.turret.RequestTurretCurrentRotation
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.process.Process
import org.woen.utils.regulator.Regulator
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Angle
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

data class SetDriveTargetVelocityEvent(val translateVelocity: Vec2, val rotationVelocity: Double)

data class RequestLookModeEvent(var lookMode: Boolean = false) : StoppingEvent

data class SetLookModeEvent(val lookMode: Boolean, val process: Process = Process())

class DriveTrain : IModule {
    private val _hardwareDriveTrain = HardwareDriveTrain()

    private var _targetTranslateVelocity = Vec2.ZERO
    private var _targetRotateVelocity = 0.0

    private val _driveMutex = SmartMutex()

    private var _driveJob: Job? = null

    private var _lookMode = false
    private var _lookProcess = Process()
    private var _lookRegulator = Regulator(Configs.DRIVE_TRAIN.LOOK_REGULATOR_PARAMETERS)
    private var _targetAngle = Angle.ZERO
    private var _lookTargetTimer = ElapsedTime()
    private var _currentRobotRotation = Angle.ZERO

    override suspend fun process() {
        _driveJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            _currentRobotRotation = odometry.odometryOrientation.angl

            val rotationErr = if (_lookMode) {
                _targetAngle = Angle(
                    (HotRun.LAZY_INSTANCE.currentRunColor.basketPosition -
                            (odometry.odometryOrientation.pos + Configs.TURRET.TURRET_CENTER_POS.turn(
                                odometry.odometryOrientation.angle
                            ))).rot()
                )

                val err = (_targetAngle - odometry.odometryOrientation.angl).angle

                if (abs(err) < Configs.DRIVE_TRAIN.LOOK_SENS) {
                    if (_lookTargetTimer.seconds() > Configs.DRIVE_TRAIN.LOOK_TARGET_TIMER)
                        _lookProcess.close()
                } else
                    _lookTargetTimer.reset()

                err
            } else
                0.0

            _driveMutex.smartLock {
                _hardwareDriveTrain.drive(
                    _targetTranslateVelocity,
                    if (_lookMode )
                        _lookRegulator.update(rotationErr)
                    else _targetRotateVelocity
                )
            }
        }
    }

    override val isBusy: Boolean
        get() = _driveJob != null && !_driveJob!!.isCompleted

    override fun dispose() {
        _driveJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareDriveTrain)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _lookMode = false
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetDriveTargetVelocityEvent::class, {
            _driveMutex.smartLock {
                _targetTranslateVelocity = it.translateVelocity
                _targetRotateVelocity = it.rotationVelocity
            }
        })

        ThreadedGamepad.LAZY_INSTANCE.addListener(object : ThreadedGamepad.IListener {
            override suspend fun update(gamepadData: Gamepad) {
                var ly = -gamepadData.left_stick_y.toDouble()
                var lx = -gamepadData.left_stick_x.toDouble()

                var rx = -gamepadData.right_stick_x.toDouble()

                if (Configs.DRIVE_TRAIN.POW_MOVE_ENABLED) {
                    ly = sign(ly) * (4.0 * (abs(ly) - 0.5).pow(3.0) + 0.5)
                    lx = sign(lx) * (4.0 * (abs(lx) - 0.5).pow(3.0) + 0.5)
                    rx = sign(rx) * (4.0 * (abs(rx) - 0.5).pow(3.0) + 0.5)
                }

                val currentRunColor = HotRun.LAZY_INSTANCE.currentRunColor

                ThreadedEventBus.LAZY_INSTANCE.invoke(
                    SetDriveTargetVelocityEvent(
                        Vec2(
                            ly,
                            lx
                        ).turn(
                            if (currentRunColor == HotRun.RunColor.BLUE) (_currentRobotRotation * -1.0 -
                                    Angle.ofDeg(90.0)).angle
                            else
                                (_currentRobotRotation * -1.0 + Angle.ofDeg(90.0)).angle
                        ) * Vec2(
                            Configs.DRIVE_TRAIN.DRIVE_VEC_MULTIPLIER,
                            Configs.DRIVE_TRAIN.DRIVE_VEC_MULTIPLIER
                        ),
                        rx * Configs.DRIVE_TRAIN.DRIVE_ANGLE_MULTIPLIER
                    )
                )
            }
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetLookModeEvent::class, {
            _lookMode = it.lookMode
            _lookProcess = it.process

            if (it.lookMode) {
                _lookRegulator.start()
                _lookTargetTimer.reset()
            }
        })

        ThreadedGamepad.LAZY_INSTANCE.addListener(ThreadedGamepad.createClickDownListener({it.circle}, {
            if(_lookMode)
                ThreadedEventBus.LAZY_INSTANCE.invoke(SetLookModeEvent(false))
        }))

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestLookModeEvent::class, {
            it.lookMode = _lookMode
        })

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.drawCircle(
                HotRun.LAZY_INSTANCE.currentRunColor.basketPosition,
                0.05,
                Color.ORANGE
            )

            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            it.addData("targetAngle", _targetAngle)
            it.addData("currentAngle", odometry.odometryOrientation.angl)
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _lookRegulator.start()
        }
    }
}