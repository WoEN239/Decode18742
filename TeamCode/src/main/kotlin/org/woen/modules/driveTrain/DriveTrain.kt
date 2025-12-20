package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.process.Process
import org.woen.utils.regulator.Regulator
import org.woen.utils.units.Angle
import org.woen.utils.units.Color
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

data class SetDriveTargetVelocityEvent(val translateVelocity: Vec2, val rotationVelocity: Double)

data class RequestDriveModeEvent(var mode: DriveTrain.DriveMode = DriveTrain.DriveMode.DRIVE) :
    StoppingEvent

data class SetDriveModeEvent(val mode: DriveTrain.DriveMode, val process: Process = Process())

class DriveTrain : IModule {
    private val _hardwareDriveTrain = HardwareDriveTrain()

    private var _targetTranslateVelocity = Vec2.ZERO
    private var _targetRotateVelocity = 0.0

    private var _driveJob: Job? = null

    enum class DriveMode {
        DRIVE,
        SHOOTING,
        PARKING
    }

    private var _targetOrientation = Orientation.ZERO

    private var _currentMode = DriveMode.DRIVE
    private var _currentProcess = Process()

    private var _xRegulator = Regulator(Configs.DRIVE_TRAIN.X_REGULATOR_PARAMETERS)
    private var _yRegulator = Regulator(Configs.DRIVE_TRAIN.Y_REGULATOR_PARAMETERS)
    private var _hRegulator = Regulator(Configs.DRIVE_TRAIN.H_REGULATOR_PARAMETERS)

    private var _targetTimer = ElapsedTime()
    private var _currentRobotRotation = Angle.ZERO

    override suspend fun process() {
        _driveJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            _currentRobotRotation = odometry.odometryOrientation.angl

            when (_currentMode) {
                DriveMode.DRIVE -> {
                    if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.MANUAL)
                        _hardwareDriveTrain.drivePowered(
                            _targetTranslateVelocity,
                            _targetRotateVelocity
                        )
                    else
                        _hardwareDriveTrain.drive(_targetTranslateVelocity, _targetRotateVelocity)
                }

                DriveMode.SHOOTING -> {
                    _targetOrientation.angl =
                        Angle(
                            (HotRun.LAZY_INSTANCE.currentRunColor.basketPosition -
                                    odometry.odometryOrientation.pos).rot()
                        )

                    _hardwareDriveTrain.drive(
                        _targetTranslateVelocity,
                        _hRegulator.update((_targetOrientation.angl - odometry.odometryOrientation.angl).angle)
                    )
                }

                DriveMode.PARKING -> {
                    _hardwareDriveTrain.drive(
                        Vec2(
                            _xRegulator.update(_targetOrientation.x - odometry.odometryOrientation.x),
                            _yRegulator.update(_targetOrientation.y - odometry.odometryOrientation.y)
                        ).turn(-odometry.odometryOrientation.angle),
                        _hRegulator.update((_targetOrientation.angl - odometry.odometryOrientation.angl).angle)
                    )
                }
            }

            if (((abs(_targetOrientation.x - odometry.odometryOrientation.x) < Configs.DRIVE_TRAIN.POS_SENS &&
                        abs(_targetOrientation.y - odometry.odometryOrientation.y) < Configs.DRIVE_TRAIN.POS_SENS) || _currentMode == DriveMode.SHOOTING) &&
                abs((_targetOrientation.angl - odometry.odometryOrientation.angl).angle) < Configs.DRIVE_TRAIN.H_SENS
            ) {
                if (_targetTimer.seconds() > Configs.DRIVE_TRAIN.TARGET_TIMER)
                    _currentProcess.close()
            } else
                _targetTimer.reset()
        }
    }

    override val isBusy: Boolean
        get() = _driveJob != null && !_driveJob!!.isCompleted

    override fun opModeStart() {

    }

    override fun opModeStop() {

    }

    override fun dispose() {
        _driveJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareDriveTrain)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _hardwareDriveTrain.currentMode =
                if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.MANUAL) HardwareDriveTrain.DriveTrainMode.POWER
                else HardwareDriveTrain.DriveTrainMode.REGULATOR

            _currentMode = DriveMode.DRIVE
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetDriveTargetVelocityEvent::class, {
            _targetTranslateVelocity = it.translateVelocity
            _targetRotateVelocity = it.rotationVelocity
        })

        ThreadedGamepad.LAZY_INSTANCE.addListener(object : ThreadedGamepad.IListener {
            override suspend fun update(gamepadData: Gamepad) {
                var ly = -gamepadData.left_stick_y.toDouble()
                var lx = -gamepadData.left_stick_x.toDouble()

                var rx = -gamepadData.right_stick_x.toDouble()

                fun calcDeathZone(value: Double, death: Double) = if (abs(value) < death) 0.0 else
                    (value - sign(value) * death) / (1.0 - death)

                ly = calcDeathZone(ly, Configs.DRIVE_TRAIN.X_DEATH_ZONE)
                lx = calcDeathZone(lx, Configs.DRIVE_TRAIN.Y_DEATH_ZONE)
                rx = calcDeathZone(rx, Configs.DRIVE_TRAIN.H_DEATH_ZONE)

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
                        ) * Vec2(if (_currentMode == DriveMode.SHOOTING) Configs.DRIVE_TRAIN.MAX_DRIVE_VELOCITY else 1.0),
                        rx
                    )
                )
            }
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetDriveModeEvent::class, {
            _currentMode = it.mode
            _currentProcess = it.process

            if (it.mode != DriveMode.DRIVE) {
                _targetTimer.reset()

                _hardwareDriveTrain.currentMode = HardwareDriveTrain.DriveTrainMode.REGULATOR

                if (it.mode == DriveMode.PARKING)
                    _targetOrientation = HotRun.LAZY_INSTANCE.currentRunColor.parkingOrientation

                _xRegulator.start()
                _xRegulator.resetIntegral()
                _yRegulator.start()
                _yRegulator.resetIntegral()
                _hRegulator.start()
                _hRegulator.resetIntegral()
            } else {
                _hardwareDriveTrain.currentMode =
                    if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.MANUAL) HardwareDriveTrain.DriveTrainMode.POWER
                    else HardwareDriveTrain.DriveTrainMode.REGULATOR
            }
        })

        ThreadedGamepad.LAZY_INSTANCE.addListener(
            ThreadedGamepad.createClickDownListener(
                { it.circle },
                {
                    if (_currentMode == DriveMode.SHOOTING)
                        ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveModeEvent(DriveMode.DRIVE))
                })
        )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
            ThreadedGamepad.createClickDownListener(
                { it.dpad_down },
                {
                    if (_currentMode == DriveMode.DRIVE)
                        ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveModeEvent(DriveMode.PARKING))
                    else if (_currentMode == DriveMode.PARKING)
                        ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveModeEvent(DriveMode.DRIVE))
                }
            ))

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestDriveModeEvent::class, {
            it.mode = _currentMode
        })

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.drawCircle(
                HotRun.LAZY_INSTANCE.currentRunColor.basketPosition,
                0.05,
                Color.ORANGE
            )

            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            it.addData("targetOrientation", _targetOrientation)
            it.addData("currentOrientation", odometry.odometryOrientation)
        }
    }
}