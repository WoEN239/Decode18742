package org.woen.modules.driveTrain.odometry.wheelOdometry

import org.woen.modules.driveTrain.odometry.IOdometry
import org.woen.modules.driveTrain.odometry.OdometryTick
import org.woen.telemetry.Configs
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Angle
import org.woen.utils.units.Vec2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class WheelOdometry : IOdometry {
    private val _hardwareOdometry = HardwareWheelOdometry("", "", "", "")

    private var _oldLeftForwardPosition = 0.0
    private var _oldRightForwardPosition = 0.0
    private var _oldLeftBackPosition = 0.0
    private var _oldRightBackPosition = 0.0

    override fun update(rotation: Angle): OdometryTick {
        val leftForwardPosition = _hardwareOdometry.leftForwardPosition.get()
        val rightForwardPosition = _hardwareOdometry.rightForwardPosition.get()
        val leftBackPosition = _hardwareOdometry.leftBackPosition.get()
        val rightBackPosition = _hardwareOdometry.rightBackPosition.get()

        val leftForwardVelocity = _hardwareOdometry.leftForwardVelocity.get()
        val rightForwardVelocity = _hardwareOdometry.rightForwardVelocity.get()
        val leftBackVelocity = _hardwareOdometry.leftBackVelocity.get()
        val rightBackVelocity = _hardwareOdometry.rightBackVelocity.get()

        val deltaLeftForwardPosition = leftForwardPosition - _oldLeftForwardPosition
        val deltaRightForwardPosition = rightForwardPosition - _oldRightForwardPosition
        val deltaLeftBackPosition = leftBackPosition - _oldLeftBackPosition
        val deltaRightBackPosition = rightBackPosition - _oldRightBackPosition

        _oldLeftForwardPosition = leftForwardPosition
        _oldRightForwardPosition = rightForwardPosition
        _oldLeftBackPosition = leftBackPosition
        _oldRightBackPosition = rightBackPosition

        val deltaRotation =
            (rightForwardPosition + rightBackPosition + -leftForwardPosition + -leftBackPosition) / Configs.DRIVE_TRAIN.ROBOT_SIZE.y

        val deltaPosition = Vec2(
            (deltaLeftForwardPosition + deltaRightForwardPosition + deltaLeftBackPosition + deltaRightBackPosition) / 4.0,
            (-deltaLeftForwardPosition + deltaRightForwardPosition + deltaLeftBackPosition + -deltaRightBackPosition) / 4.0 * Configs.DRIVE_TRAIN.Y_LAG
        )

        val deltaCorrectedPosition = if (abs(deltaRotation) < Configs.ODOMETRY.ODOMETER_ROTATE_SENS)
            deltaPosition
        else
            Vec2(
                deltaPosition.x * sin(deltaRotation) / deltaRotation +
                        deltaPosition.y * (cos(deltaRotation) - 1.0) / deltaRotation,
                deltaPosition.x * (1.0 - cos(deltaRotation)) / deltaRotation +
                        deltaPosition.y * sin(deltaRotation) / deltaRotation
            )

        return OdometryTick(
            deltaCorrectedPosition.turn(rotation.angle),
            Vec2(
                (leftForwardVelocity + rightForwardVelocity + leftBackVelocity + rightBackVelocity) / 4.0,
                (-leftForwardVelocity + rightForwardVelocity + leftBackVelocity - rightBackVelocity) / 4.0 * Configs.DRIVE_TRAIN.Y_LAG
            ),
            deltaRotation,
            (rightForwardVelocity + rightBackVelocity + -leftForwardVelocity + -leftBackVelocity) / Configs.DRIVE_TRAIN.ROBOT_SIZE.y
        )
    }

    override fun dispose() {

    }

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareOdometry)
    }
}