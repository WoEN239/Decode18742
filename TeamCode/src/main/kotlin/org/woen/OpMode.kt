package org.woen

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.woen.utils.regulator.Regulator
import org.woen.utils.regulator.RegulatorParameters
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign

@Config
internal object OPMODE_CONFIG{
    @JvmField
    var PULLEY_VELOCITY = 1600.0

    @JvmField
    var PARMETER = RegulatorParameters(kF = 0.0053, kP = 0.03, kI = 0.01, limitU = 12.0)
}

@TeleOp
class OpMode : LinearOpMode() {
    override fun runOpMode() {
        val leftForwardDrive = hardwareMap.get("leftForwardDrive") as DcMotorEx
        val leftBackDrive = hardwareMap.get("leftBackDrive") as DcMotorEx
        val rightForwardDrive = hardwareMap.get("rightForwardDrive") as DcMotorEx
        val rightBackDrive = hardwareMap.get("rightBackDrive") as DcMotorEx

        val turretAngleServo = hardwareMap.get("turretAngleServo") as Servo
        val pushServo = hardwareMap.get("pushServo") as Servo
        val gateServo2 = hardwareMap.get("gateServo") as Servo

        val regulator = Regulator(OPMODE_CONFIG.PARMETER)

        leftBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        leftForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        leftBackDrive.direction = DcMotorSimple.Direction.REVERSE
        leftForwardDrive.direction = DcMotorSimple.Direction.REVERSE
        rightForwardDrive.direction = DcMotorSimple.Direction.REVERSE

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

        regulator.start()

        turretAngleServo.position = 0.47
        pushServo.position = 0.275
        gateServo2.position = 0.0

        while (opModeIsActive()) {
            val telem = FtcDashboard.getInstance().telemetry

            telem.addData("curent", pulleyMotor.velocity)
            telem.addData("target", OPMODE_CONFIG.PULLEY_VELOCITY )

            telem.update()

            pulleyMotor.power = regulator.update(OPMODE_CONFIG.PULLEY_VELOCITY - pulleyMotor.velocity,
                OPMODE_CONFIG.PULLEY_VELOCITY) / battery.voltage

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
                gateServo.position = 0.75

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