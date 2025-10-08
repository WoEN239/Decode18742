package org.woen.modules.driveTrain

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.IMU
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.events.SimpleEvent
import org.woen.utils.units.Angle

class HardwareGyro : IHardwareDevice {
    private lateinit var _imu: IMU

    private val _gyroUpdateTimer = ElapsedTime()

    val gyroUpdateEvent = SimpleEvent<Angle>()

    override fun update() {
        if (_gyroUpdateTimer.seconds() > 1.0 / Configs.GYRO.GYRO_UPDATE_HZ) {
            _gyroUpdateTimer.reset()

            val angles = _imu.robotYawPitchRollAngles

            gyroUpdateEvent.invoke(Angle(angles.getYaw(AngleUnit.RADIANS)))
        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _imu = hardwareMap.get("imu") as IMU

        _imu.initialize(
            IMU.Parameters(
                RevHubOrientationOnRobot(
                    RevHubOrientationOnRobot.LogoFacingDirection.UP,
                    RevHubOrientationOnRobot.UsbFacingDirection.BACKWARD
                )
            )
        )

        _imu.resetYaw()

        _gyroUpdateTimer.reset()
    }

    override fun dispose() {

    }
}