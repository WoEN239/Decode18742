package org.woen.tests


import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign

@TeleOp
class SimpleTeleop : LinearOpMode() {
    lateinit var leftForwardDrive: DcMotorEx
    lateinit var leftBackDrive: DcMotorEx
    lateinit var rightForwardDrive: DcMotorEx
    lateinit var rightBackDrive: DcMotorEx

    override fun runOpMode() {
        leftForwardDrive = hardwareMap.get("leftForwardDrive") as DcMotorEx
        leftBackDrive = hardwareMap.get("leftBackDrive") as DcMotorEx
        rightForwardDrive = hardwareMap.get("rightForwardDrive") as DcMotorEx
        rightBackDrive = hardwareMap.get("rightBackDrive") as DcMotorEx

        leftBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        leftForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        leftForwardDrive.direction = DcMotorSimple.Direction.REVERSE
        leftBackDrive.direction = DcMotorSimple.Direction.REVERSE
        rightBackDrive.direction = DcMotorSimple.Direction.REVERSE

        while (!isStarted()) {
            if (gamepad1.left_bumper || gamepad1.square || gamepad1.right_bumper || gamepad1.circle ||
                abs(gamepad1.left_stick_y.toDouble()) > 0.01 || abs(gamepad1.left_stick_x.toDouble()) > 0.01
                || abs(gamepad1.right_stick_x.toDouble()) > 0.01
            )
                OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
                    .startActiveOpMode()
        }

        resetRuntime()

        while (opModeIsActive()) {
            updateDriveTrain()
        }
    }

    fun updateDriveTrain() {
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
    }
}