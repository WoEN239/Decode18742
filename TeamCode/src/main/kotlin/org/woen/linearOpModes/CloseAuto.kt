package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.robotcore.internal.system.AppUtil

@Autonomous
class CloseAuto: LinearOpMode() {
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

        waitForStart()
        resetRuntime()

        leftForwardDrive.power = -1.0
        leftBackDrive.power = -1.0
        rightForwardDrive.power = -1.0
        rightBackDrive.power = -1.0

        sleep(500)

        leftForwardDrive.power = 0.0
        leftBackDrive.power = 0.0
        rightForwardDrive.power = 0.0
        rightBackDrive.power = 0.0

        while (opModeIsActive()){}

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode(TeleOp::class.simpleName)
    }
}