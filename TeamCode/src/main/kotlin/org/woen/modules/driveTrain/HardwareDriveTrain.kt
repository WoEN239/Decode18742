package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.motor.MotorOnly
import org.woen.utils.units.Vec2
import kotlin.math.abs
import kotlin.math.max

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

    private fun setVoltage(direction: Vec2, rotate: Double) {
        var leftFrontPower =
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x - direction.y - rotate)
        var rightBackPower =
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x - direction.y + rotate)
        var leftBackPower =
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x + direction.y - rotate)
        var rightForwardPower =
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x + direction.y + rotate)

        val absMax = max(
            abs(leftFrontPower),
            max(
                abs(rightBackPower),
                max(
                    abs(leftBackPower),
                    abs(rightForwardPower)
                )
            )
        )

        if (absMax > 1.0) {
            leftFrontPower /= absMax
            rightBackPower /= absMax
            leftBackPower /= absMax
            rightForwardPower /= absMax
        }

        _leftForwardMotor.power = leftFrontPower
        _rightBackMotor.power = rightBackPower
        _leftBackMotor.power = leftBackPower
        _rightForwardMotor.power = rightForwardPower
    }

    override fun dispose() {

    }
}