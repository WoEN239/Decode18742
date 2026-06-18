package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.woen.modules.drivetrain.ODOMETRY_CONFIG
import org.woen.utils.units.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

@TeleOp
class DriveTest: LinearOpMode() {
    override fun runOpMode() {
        val brushMotor = hardwareMap.get("brushMotor") as DcMotorEx
        val beltsMotor = hardwareMap.get("beltsMotor") as DcMotorEx

        brushMotor.direction = DcMotorSimple.Direction.REVERSE

        brushMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        brushMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        beltsMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        beltsMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        brushMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        val pinpoint = hardwareMap.get("odometry") as GoBildaPinpointDriver

        pinpoint.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
        pinpoint.setEncoderDirections(
            GoBildaPinpointDriver.EncoderDirection.FORWARD,
            GoBildaPinpointDriver.EncoderDirection.REVERSED
        )

        pinpoint.setOffsets(
            ODOMETRY_CONFIG.X_ODOMETER_POSITION, ODOMETRY_CONFIG.Y_ODOMETER_POSITION,
            DistanceUnit.METER
        )

        pinpoint.resetPosAndIMU()

        val leftBackDrive = hardwareMap.get("leftBackDrive") as DcMotorEx
        val leftForwardDrive = hardwareMap.get("leftForwardDrive") as DcMotorEx
        val rightBackDrive = hardwareMap.get("rightBackDrive") as DcMotorEx
        val rightForwardDrive = hardwareMap.get("rightForwardDrive") as DcMotorEx

        leftBackDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        leftForwardDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        rightBackDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        rightForwardDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER

        leftBackDrive.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        leftForwardDrive.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        rightBackDrive.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        rightForwardDrive.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        leftBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        leftForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        leftForwardDrive.direction = DcMotorSimple.Direction.REVERSE
        leftBackDrive.direction = DcMotorSimple.Direction.REVERSE

        waitForStart()
        resetRuntime()

        while (opModeIsActive()){
            pinpoint.update()

            FtcDashboard.getInstance().telemetry.addData("vel", Vec2(pinpoint.getVelX(DistanceUnit.METER), pinpoint.getVelY(DistanceUnit.METER)).length())

            val pinpointOrientation = pinpoint.position

            val angle = pinpointOrientation.getHeading(AngleUnit.RADIANS)

            var x = -gamepad1.left_stick_y.toDouble()
            var y = -gamepad1.left_stick_x.toDouble()
            var rotate = -gamepad1.right_stick_x.toDouble()

            x *= abs(x)
            y *= abs(y)

            rotate *= abs(rotate)

            val direction = Vec2(x, y)
                .turn(-angle - PI / 2.0)

            var leftForwardPower = direction.x - direction.y - rotate
            var rightBackPower = direction.x - direction.y + rotate
            var leftBackPower = direction.x + direction.y - rotate
            var rightForwardPower = direction.x + direction.y + rotate

            val absMax = max(
                abs(leftForwardPower),
                max(
                    abs(rightBackPower),
                    max(
                        abs(leftBackPower),
                        abs(rightForwardPower)
                    )
                )
            )

            if (absMax > 1.0) {
                leftForwardPower /= absMax
                rightBackPower /= absMax
                leftBackPower /= absMax
                rightForwardPower /= absMax
            }

            leftBackDrive.power = leftBackPower
            leftForwardDrive.power = leftForwardPower
            rightBackDrive.power = rightBackPower
            rightForwardDrive.power = rightForwardPower

            brushMotor.power = if(gamepad1.right_bumper) -1.0 else 1.0
            beltsMotor.power = if(gamepad1.right_bumper) 1.0 else 0.0

            FtcDashboard.getInstance().telemetry.update()
        }
    }
}