package org.woen.modules.driveTrain

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.ThreadManager
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Vec2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class OdometerUpdateEvent(val rotate: Double, val pos: Vec2,
                               val rotateVelocity: Double, val velocity: Vec2)

class Odometry: IModule {
    private val _hardwareOdometry = HardwareOdometry("", "")
    private val _threeOdometry = HardwareThreeOdometry("")

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareOdometry)
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_threeOdometry)
    }

    private var _odometryJob: Job? = null

    private var _oldLeftPosition = 0.0
    private var _oldRightPosition = 0.0
    private var _oldSidePosition = 0.0
    private var _oldRotation = 0.0

    private var _position = Vec2.ZERO

    override fun process() {
        _odometryJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val leftPos = _hardwareOdometry.leftPosition.get()
            val rightPos = _hardwareOdometry.rightPosition.get()
            val sidePos = _threeOdometry.odometerPosition.get()

            val leftVelocity = _hardwareOdometry.leftVelocity.get()
            val rightVelocity = _hardwareOdometry.rightVelocity.get()
            val sideVelocity = _threeOdometry.odometerVelocity.get()

            val rotation = rightPos / ThreadedConfigs.ODOMETER_RIGHT_RADIUS.get()
            - leftPos / ThreadedConfigs.ODOMETER_LEFT_RADIUS.get()

            val rotationVelocity = rightVelocity / ThreadedConfigs.ODOMETER_RIGHT_RADIUS.get()
            - leftVelocity / ThreadedConfigs.ODOMETER_RIGHT_RADIUS.get()

            val deltaLeft = leftPos - _oldLeftPosition
            val deltaRight = rightPos - _oldRightPosition
            val deltaSide = sidePos - _oldSidePosition
            val deltaRotation = rotation - _oldRotation

            val deltaX = (deltaLeft + deltaRight) / 2.0
            val deltaY = deltaSide - (ThreadedConfigs.ODOMETER_SIDE_RADIUS.get() * deltaRotation)

            val deltaXCorrected: Double
            val deltaYCorrected: Double

            if(abs(deltaRotation) < ThreadedConfigs.ODOMETER_ROTATE_SENS.get()) {
                deltaXCorrected = deltaX
                deltaYCorrected = deltaY
            }
            else{
                deltaXCorrected = deltaX * sin(deltaRotation) / deltaRotation + deltaY * (cos(deltaRotation) - 1.0) / deltaRotation
                deltaYCorrected = deltaX * (1.0 - cos(deltaRotation)) / deltaRotation + deltaY * sin(deltaRotation) / deltaRotation
            }

            _position += Vec2(deltaXCorrected, deltaYCorrected)

            _oldLeftPosition = leftPos
            _oldRightPosition = rightPos
            _oldSidePosition = sidePos
            _oldRotation = rotation
        }
    }

    override val isBusy: Boolean
        get() = _odometryJob == null || _odometryJob!!.isCompleted

    override fun dispose() {
        _odometryJob?.cancel()
    }
}