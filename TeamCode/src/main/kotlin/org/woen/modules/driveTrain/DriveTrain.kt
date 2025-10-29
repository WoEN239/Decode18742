package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.Gamepad
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.odometry.RequireOdometryEvent
import org.woen.telemetry.Configs
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.process.Process
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Angle
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

data class SetDriveTargetVelocityEvent(val translateVelocity: Vec2, val rotationVelocity: Double)

data class RequestLookModeEvent(var lookMode: Boolean = false) : StoppingEvent

data class SetLookModeEvent(val lookMode: Boolean, val process: Process)

class DriveTrain : IModule {
    private val _hardwareDriveTrain = HardwareDriveTrain(
        "leftForwardDrive",
        "leftBackDrive",
        "rightForwardDrive",
        "rightBackDrive"
    )

    private var _targetTranslateVelocity = Vec2.ZERO
    private var _targetRotateVelocity = 0.0

    private val _driveMutex = SmartMutex()

    private var _driveJob: Job? = null

    private var _lookMode = AtomicReference(false)
    private var _lookProcess = AtomicReference(Process())

    override suspend fun process() {
        _driveJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
                return@launch

            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            val rotationErr = if (_lookMode.get()) {
                val err = Angle(
                    (HotRun.LAZY_INSTANCE.currentRunColor.get().basketPosition
                            - odometry.odometryOrientation.pos).rot()
                            - odometry.odometryOrientation.angle
                ).angle

                if(abs(err) < Configs.DRIVE_TRAIN.LOOK_SENS)
                    _lookProcess.get().close()

                err
            } else
                0.0

            _driveMutex.smartLock {
                _hardwareDriveTrain.drive(
                    _targetTranslateVelocity/*.turn(-odometry.odometryOrientation.angle)*/,
                    if (_lookMode.get())
                        rotationErr * Configs.DRIVE_TRAIN.LOOK_P
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

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareDriveTrain)

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
                    ly *= abs(ly)
                    lx *= abs(lx)
                    rx *= abs(rx)
                }

                ThreadedEventBus.LAZY_INSTANCE.invoke(
                    SetDriveTargetVelocityEvent(
                        Vec2(
                            ly,
                            lx
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
            _lookMode.set(it.lookMode)
            _lookProcess.set(it.process)
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequestLookModeEvent::class, {
            it.lookMode = _lookMode.get()
        })
    }
}