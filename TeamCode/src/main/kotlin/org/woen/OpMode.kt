package org.woen

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.VoltageSensor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign

@TeleOp
class OpMode : LinearOpMode() {
    override fun runOpMode() {
        val leftForwardDrive = hardwareMap.get("leftForwardDrive") as DcMotorEx
        val leftBackDrive = hardwareMap.get("leftBackDrive") as DcMotorEx
        val rightForwardDrive = hardwareMap.get("rightForwardDrive") as DcMotorEx
        val rightBackDrive = hardwareMap.get("rightBackDrive") as DcMotorEx

        leftBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        leftForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        leftBackDrive.direction = DcMotorSimple.Direction.REVERSE
        leftForwardDrive.direction = DcMotorSimple.Direction.REVERSE
        rightForwardDrive.direction = DcMotorSimple.Direction.REVERSE
        rightBackDrive.direction = DcMotorSimple.Direction.REVERSE

        val brushMotor = hardwareMap.get("brushMotor") as DcMotorEx

        brushMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        val beltMotor = hardwareMap.get("beltMotor") as DcMotorEx

        beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        val pulleyMotor = hardwareMap.get("pulleyMotor") as DcMotorEx

        pulleyMotor.direction = DcMotorSimple.Direction.REVERSE

        val gateServo = hardwareMap.get("turretGateServo") as Servo

        val battery = hardwareMap.get(VoltageSensor::class.java, "Control Hub")

        waitForStart()
        resetRuntime()

        while (opModeIsActive()) {
            pulleyMotor.power = battery.voltage / 10.0

            var ly = -gamepad1.left_stick_y.toDouble()
            var lx = -gamepad1.left_stick_x.toDouble()

            var rx = -gamepad1.right_stick_x.toDouble()

            ly = sign(ly) * (4.0 * (abs(ly) - 0.5).pow(3.0) + 0.5)
            lx = sign(lx) * (4.0 * (abs(lx) - 0.5).pow(3.0) + 0.5)
            rx = sign(rx) * (4.0 * (abs(rx) - 0.5).pow(3.0) + 0.5)

            var leftFrontPower = ly - lx - rx
            var rightBackPower = ly - lx + rx
            var leftBackPower = ly + lx - rx
            var rightForwardPower = ly + lx + rx

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

            leftForwardDrive.power = leftFrontPower
            rightBackDrive.power = rightBackPower
            leftBackDrive.power = leftBackPower
            rightForwardDrive.power = rightForwardPower

            if (gamepad1.left_bumper) {
                gateServo.position = 0.73

                brushMotor.power = 0.0
                beltMotor.power = 1.0
            } else {
                gateServo.position = 0.45

                if (gamepad1.right_bumper) {
                    brushMotor.power = 1.0
                    beltMotor.power = 1.0
                } else {
                    brushMotor.power = 0.0
                    beltMotor.power = 0.0
                }
            }
        }
    }
}