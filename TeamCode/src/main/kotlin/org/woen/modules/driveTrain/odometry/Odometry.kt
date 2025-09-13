package org.woen.modules.driveTrain.odometry

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.camera.Camera
import org.woen.modules.driveTrain.HardwareGyro
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class OdometryUpdateEvent(
    val odometryOrientation: Orientation,
    val odometryRotateVelocity: Double, val odometryVelocity: Vec2
)

data class RequireOdometryEvent(
    var odometryOrientation: Orientation = Orientation.ZERO,
    var odometryRotateVelocity: Double = 0.0, var odometryVelocity: Vec2 = Vec2.ZERO
)


class Odometry : IModule {
    private val _hardwareOdometry = HardwareOdometry("", "")
    private val _threeOdometry = HardwareThreeOdometry("")
    private val _gyro = HardwareGyro()

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareOdometry)
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_threeOdometry)
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_gyro)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            RequireOdometryEvent::class,
            {
                _odometryMutex.withLock {
                    it.odometryOrientation = Orientation(_currentPosition, _mergeRotation)
                    it.odometryVelocity = _currentVelocity
                    it.odometryRotateVelocity = _currentRotationVelocity
                }
            })


        _gyro.gyroUpdateEvent += {
            runBlocking {
                _gyroMutex.withLock {
                    _odometryMutex.withLock {
                        _mergeRotation = Angle(
                            _gyroFilter.updateRaw(
                                _mergeRotation.angle,
                                (it - _mergeRotation).angle
                            )
                        )
                    }
                }
            }
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            runBlocking {
                _gyroMutex.withLock {
                    _gyroFilter.start()
                }

                _mergePositionMutex.withLock {
                    _positionXFilter.start()
                    _positionYFilter.start()
                }
            }
        }

        ThreadedConfigs.ODOMETRY_MERGE_COEF.onSet += {
            runBlocking {
                _mergePositionMutex.withLock {
                    _positionXFilter.coef = it
                    _positionYFilter.coef = it
                }
            }
        }

        Camera.LAZY_INSTANCE.cameraPositionUpdateEvent += {
            runBlocking {
                _mergePositionMutex.withLock {
                    _odometryMutex.withLock {
                        _currentPosition = Vec2(
                            _positionXFilter.updateRaw(
                                _currentPosition.x,
                                it.x - _currentPosition.x
                            ),
                            _positionYFilter.updateRaw(
                                _currentPosition.y,
                                it.y - _currentPosition.y
                            )
                        )
                    }
                }
            }
        }
    }

    private val _gyroMutex = Mutex()

    private val _mergePositionMutex = Mutex()

    private val _gyroFilter = ExponentialFilter(ThreadedConfigs.GYRO_MERGE_COEF.get())
    private val _positionXFilter = ExponentialFilter(ThreadedConfigs.ODOMETRY_MERGE_COEF.get())
    private val _positionYFilter = ExponentialFilter(ThreadedConfigs.ODOMETRY_MERGE_COEF.get())

    private var _odometryJob: Job? = null

    private var _oldLeftPosition = 0.0
    private var _oldRightPosition = 0.0
    private var _oldSidePosition = 0.0

    private var _oldRotation = Angle.ZERO
    private var _oldOdometerRotation = Angle.ZERO

    private var _mergeRotation = Angle.ZERO

    private val _odometryMutex = Mutex()

    private var _currentPosition = Vec2.ZERO
    private var _currentVelocity = Vec2.ZERO
    private var _currentRotationVelocity = 0.0

    override suspend fun process() {
        _odometryJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val leftPos = _hardwareOdometry.leftPosition.get()
            val rightPos = _hardwareOdometry.rightPosition.get()
            val sidePos = _threeOdometry.odometerPosition.get()

            val leftVelocity = _hardwareOdometry.leftVelocity.get()
            val rightVelocity = _hardwareOdometry.rightVelocity.get()
            val sideVelocity = _threeOdometry.odometerVelocity.get()

            _odometryMutex.withLock {
                val odometryRotation = Angle(
                    rightPos / ThreadedConfigs.ODOMETER_RIGHT_RADIUS.get()
                            - leftPos / ThreadedConfigs.ODOMETER_LEFT_RADIUS.get()
                )

                _mergeRotation += odometryRotation - _oldOdometerRotation

                _oldOdometerRotation = odometryRotation

                _currentRotationVelocity =
                    rightVelocity / ThreadedConfigs.ODOMETER_RIGHT_RADIUS.get()
                -leftVelocity / ThreadedConfigs.ODOMETER_RIGHT_RADIUS.get()

                val deltaLeft = leftPos - _oldLeftPosition
                val deltaRight = rightPos - _oldRightPosition
                val deltaSide = sidePos - _oldSidePosition
                val deltaRotation = (_mergeRotation - _oldRotation).angle

                val deltaX = (deltaLeft + deltaRight) / 2.0
                val deltaY =
                    deltaSide - (ThreadedConfigs.ODOMETER_SIDE_RADIUS.get() * deltaRotation)

                val deltaXCorrected: Double
                val deltaYCorrected: Double

                if (abs(deltaRotation) < ThreadedConfigs.ODOMETER_ROTATE_SENS.get()) {
                    deltaXCorrected = deltaX
                    deltaYCorrected = deltaY
                } else {
                    deltaXCorrected =
                        deltaX * sin(deltaRotation) / deltaRotation + deltaY * (cos(deltaRotation) - 1.0) / deltaRotation
                    deltaYCorrected =
                        deltaX * (1.0 - cos(deltaRotation)) / deltaRotation + deltaY * sin(
                            deltaRotation
                        ) / deltaRotation
                }

                _currentPosition += Vec2(deltaXCorrected, deltaYCorrected)

                _currentVelocity = Vec2(
                    (leftVelocity + rightVelocity) / 2.0,
                    sideVelocity - ThreadedConfigs.ODOMETER_SIDE_RADIUS.get() * _currentRotationVelocity
                )

                _oldLeftPosition = leftPos
                _oldRightPosition = rightPos
                _oldSidePosition = sidePos
                _oldRotation = _mergeRotation

                ThreadedEventBus.LAZY_INSTANCE.invoke(
                    OdometryUpdateEvent(
                        Orientation(_currentPosition, _mergeRotation),
                        _currentRotationVelocity,
                        _currentVelocity
                    )
                )
            }
        }
    }

    override val isBusy: Boolean
        get() = _odometryJob == null || _odometryJob!!.isCompleted

    override fun dispose() {
        _odometryJob?.cancel()
    }
}