package org.woen.modules.driveTrain.odometry.odometersOdometry

import org.woen.modules.driveTrain.odometry.IOdometry
import org.woen.modules.driveTrain.odometry.OdometryTick
import org.woen.telemetry.Configs
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Angle
import org.woen.utils.units.Vec2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class OdometersOdometry : IOdometry {
    private val _hardwareOdometersOdometry =
        HardwareOdometersOdometry("leftFrowardDrive", "rightBackDrive")
    private val _threeOdometry = HardwareThreeOdometersOdometry("sideOdometer")

    private var _oldLeftPosition = 0.0
    private var _oldRightPosition = 0.0
    private var _oldSidePosition = 0.0

    private var _oldRotation = Angle.ZERO

    override fun update(rotation: Angle): OdometryTick {
        val leftPos = _hardwareOdometersOdometry.leftPosition.get()
        val rightPos = _hardwareOdometersOdometry.rightPosition.get()
        val sidePos = _threeOdometry.odometerPosition.get()

        val leftVelocity = _hardwareOdometersOdometry.leftVelocity.get()
        val rightVelocity = _hardwareOdometersOdometry.rightVelocity.get()
        val sideVelocity = _threeOdometry.odometerVelocity.get()

        val odometryRotation = Angle(
            (rightPos / Configs.ODOMETRY.ODOMETER_RIGHT_RADIUS
                    - leftPos / Configs.ODOMETRY.ODOMETER_LEFT_RADIUS) / 2.0
        )

        val deltaLeft = leftPos - _oldLeftPosition
        val deltaRight = rightPos - _oldRightPosition
        val deltaSide = sidePos - _oldSidePosition
        val deltaRotation = (odometryRotation - _oldRotation).angle

        val deltaX = (deltaLeft + deltaRight) / 2.0
        val deltaY =
            deltaSide - (Configs.ODOMETRY.ODOMETER_SIDE_RADIUS * deltaRotation)

        val deltaXCorrected: Double
        val deltaYCorrected: Double

        if (abs(deltaRotation) < Configs.ODOMETRY.ODOMETER_ROTATE_SENS) {
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

        _oldLeftPosition = leftPos
        _oldRightPosition = rightPos
        _oldSidePosition = sidePos
        _oldRotation = odometryRotation

        val rotationVel =
            (rightVelocity / Configs.ODOMETRY.ODOMETER_RIGHT_RADIUS
                    - leftVelocity / Configs.ODOMETRY.ODOMETER_RIGHT_RADIUS) / 2.0

        return OdometryTick(
            Vec2(deltaXCorrected, deltaYCorrected)
                .turn(rotation.angle), Vec2(
                (leftVelocity + rightVelocity) / 2.0,
                sideVelocity - Configs.ODOMETRY.ODOMETER_SIDE_RADIUS * rotationVel
            ), deltaRotation, rotationVel
        )
    }

    override fun dispose() {

    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareOdometersOdometry, _threeOdometry)
    }
}