package org.woen.modules.driveTrain

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.modules.IModule
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Angle
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class OdometryUpdateEvent(
    val odometryRotate: Angle, val odometryPos: Vec2,
    val odometryRotateVelocity: Double, val odometryVelocity: Vec2
)

data class RequireOdometryEvent(
    var odometryRotate: Angle = Angle(0.0), var odometryPos: Vec2 = Vec2.ZERO,
    var odometryRotateVelocity: Double = 0.0, var odometryVelocity: Vec2 = Vec2.ZERO
)


class Odometry : IModule {
    private val _hardwareOdometry = HardwareOdometry("", "")
    private val _threeOdometry = HardwareThreeOdometry("")

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareOdometry)
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_threeOdometry)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            RequireOdometryEvent::class,
            {
                runBlocking {
                    _odometryMutex.withLock {
                        it.odometryPos = _position
                        it.odometryVelocity = _currentVelocity
                        it.odometryRotate = _currentRotation
                        it.odometryRotateVelocity = _currentRotationVelocity
                    }
                }
            })
    }

    private var _odometryJob: Job? = null

    private var _oldLeftPosition = 0.0
    private var _oldRightPosition = 0.0
    private var _oldSidePosition = 0.0
    private var _oldRotation = Angle(0.0)

    private val _odometryMutex = Mutex()

    private var _position = Vec2.ZERO
    private var _currentVelocity = Vec2.ZERO
    private var _currentRotation = Angle(0.0)
    private var _currentRotationVelocity = 0.0

    override fun process() {
        _odometryJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val leftPos = _hardwareOdometry.leftPosition.get()
            val rightPos = _hardwareOdometry.rightPosition.get()
            val sidePos = _threeOdometry.odometerPosition.get()

            val leftVelocity = _hardwareOdometry.leftVelocity.get()
            val rightVelocity = _hardwareOdometry.rightVelocity.get()
            val sideVelocity = _threeOdometry.odometerVelocity.get()

            _odometryMutex.withLock {
                _currentRotation = Angle(
                    rightPos / ThreadedConfigs.ODOMETER_RIGHT_RADIUS.get()
                            - leftPos / ThreadedConfigs.ODOMETER_LEFT_RADIUS.get()
                )

                _currentRotationVelocity =
                    rightVelocity / ThreadedConfigs.ODOMETER_RIGHT_RADIUS.get()
                -leftVelocity / ThreadedConfigs.ODOMETER_RIGHT_RADIUS.get()

                val deltaLeft = leftPos - _oldLeftPosition
                val deltaRight = rightPos - _oldRightPosition
                val deltaSide = sidePos - _oldSidePosition
                val deltaRotation = (_currentRotation - _oldRotation).angle

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

                _position += Vec2(deltaXCorrected, deltaYCorrected)

                _currentVelocity = Vec2(
                    (leftVelocity + rightVelocity) / 2.0,
                    sideVelocity - ThreadedConfigs.ODOMETER_SIDE_RADIUS.get() * _currentRotationVelocity
                )

                _oldLeftPosition = leftPos
                _oldRightPosition = rightPos
                _oldSidePosition = sidePos
                _oldRotation = _currentRotation

                ThreadedEventBus.LAZY_INSTANCE.invoke(
                    OdometryUpdateEvent(
                        _currentRotation,
                        _position,
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