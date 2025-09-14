package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.Gamepad
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.odometry.RequireOdometryEvent
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Angle
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicBoolean

data class SetDriveTargetVelocityEvent(val translateVelocity: Vec2, val rotationVelocity: Double)

class EnableLookMode()

class DriveTrain : IModule {
    private val _hardwareDriveTrain = HardwareDriveTrain(
        "leftForwardDrive",
        "leftBackDrive",
        "rightForwardDrive",
        "rightBackDrive"
    )

    private var _targetTranslateVelocity = Vec2.ZERO
    private var _targetRotateVelocity = 0.0

    private val _driveMutex = Mutex()

    private var _driveJob: Job? = null

    private var _lookMode = AtomicBoolean(false)

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareDriveTrain)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetDriveTargetVelocityEvent::class, {
            _driveMutex.withLock {
                _targetTranslateVelocity = it.translateVelocity
                _targetRotateVelocity = if (_lookMode.get()) 0.0 else it.rotationVelocity
            }
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(EnableLookMode::class, {
            _lookMode.set(true)
        })

        ThreadedGamepad.LAZY_INSTANCE.addListener(object : ThreadedGamepad.IListener {
            override suspend fun update(gamepadData: Gamepad) {
                ThreadedEventBus.LAZY_INSTANCE.invoke(
                    SetDriveTargetVelocityEvent(
                        Vec2(
                            gamepadData.left_stick_x.toDouble(),
                            gamepadData.left_stick_y.toDouble()
                        ), -gamepadData.right_stick_x.toDouble()
                    )
                )
            }
        })
    }

    override suspend fun process() {
        _driveJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            _driveMutex.withLock {
                _hardwareDriveTrain.drive(
                    _targetTranslateVelocity.turn(-odometry.odometryOrientation.angl.angle),
                    if (_lookMode.get()) {
                        (Angle(
                            (HotRun.LAZY_INSTANCE.currentRunColor.get()
                                .getBasketPosition() - odometry.odometryOrientation.pos).rot()
                        ) - odometry.odometryOrientation.angl).angle * ThreadedConfigs.DRIVE_TRAIN_LOOK_P.get()
                    } else _targetRotateVelocity
                )
            }
        }
    }

    override val isBusy: Boolean
        get() = _driveJob == null || _driveJob!!.isCompleted

    override fun dispose() {
        _driveJob?.cancel()
    }
}