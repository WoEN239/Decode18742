package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.motor.MotorOnly

class HardwareDriveTrain(
    private val _leftForwardMotorName: String,
    private val _leftBackMotorName: String,
    private val _rightForwardMotorName: String,
    private val _rightBackMotorName: String
) : IHardwareDevice {
    private lateinit var _leftForwardMotor: DcMotorEx
    private lateinit var _leftBackMotor: DcMotorEx
    private lateinit var _rightBackMotor: DcMotorEx
    private lateinit var _rightForwardMotor: DcMotorEx

    override fun update() {

    }

    override fun init(hardwareMap: HardwareMap) {
        _leftForwardMotor = MotorOnly(hardwareMap.get(_leftForwardMotorName) as DcMotorEx)
        _leftBackMotor = MotorOnly(hardwareMap.get(_leftBackMotorName) as DcMotorEx)
        _rightBackMotor = MotorOnly(hardwareMap.get(_rightBackMotorName) as DcMotorEx)
        _rightForwardMotor = MotorOnly(hardwareMap.get(_rightForwardMotorName) as DcMotorEx)

        _leftBackMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        _leftForwardMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        _rightForwardMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        _rightBackMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _rightBackMotor.direction = DcMotorSimple.Direction.REVERSE
        _rightForwardMotor.direction = DcMotorSimple.Direction.REVERSE
    }

    override fun dispose() {

    }
}