package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.motor.EncoderOnly

class Odometry(val leftOdometerName: String, val rightOdometerName: String): IHardwareDevice {
    private lateinit var _leftOdometer: DcMotorEx
    private lateinit var _rightOdometer: DcMotorEx

    override fun update() {

    }

    override fun init(hardwareMap: HardwareMap) {
        _leftOdometer = EncoderOnly(hardwareMap.get(leftOdometerName) as DcMotorEx)
        _rightOdometer = EncoderOnly(hardwareMap.get(rightOdometerName) as DcMotorEx)
    }

    override fun dispose() {

    }
}