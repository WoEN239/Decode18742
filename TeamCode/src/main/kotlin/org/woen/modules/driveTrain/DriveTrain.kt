package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.Gamepad
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.modules.IModule
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Vec2

data class SetDriveTargetVelocityEvent(val translateVelocity: Vec2, val rotationVelocity: Double)

class DriveTrain : IModule {
    private val _hardwareDriveTrain = HardwareDriveTrain("", "", "", "")

    private var _targetTranslateVelocity = Vec2.ZERO
    private var _targetRotateVelocity = 0.0

    private val _driveMutex = Mutex()

    private var _driveJob: Job? = null

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareDriveTrain)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetDriveTargetVelocityEvent::class, {
            _driveMutex.withLock {
                _targetTranslateVelocity = it.translateVelocity
                _targetRotateVelocity = it.rotationVelocity
            }
        })

        ThreadedGamepad.LAZY_INSTANCE.addListener(object : ThreadedGamepad.IListener {
            override suspend fun update(gamepadData: Gamepad) {
                _driveMutex.withLock {
                    _targetTranslateVelocity = Vec2(
                        gamepadData.left_stick_x.toDouble(),
                        gamepadData.left_stick_y.toDouble()
                    )
                    _targetRotateVelocity = -gamepadData.right_stick_x.toDouble()
                }
            }
        })
    }

    override suspend fun process() {
        _driveJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            _driveMutex.withLock {
                _hardwareDriveTrain.drive(_targetTranslateVelocity, _targetRotateVelocity)
            }
        }
    }

    override val isBusy: Boolean
        get() = _driveJob == null || _driveJob!!.isCompleted

    override fun dispose() {
        _driveJob?.cancel()
    }
}