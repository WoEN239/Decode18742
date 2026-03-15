package org.woen.linearOpModes

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Servo
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.OpMode
import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.Hardware
import org.woen.utils.motor.MotorOnly



@Autonomous
class FarAuto: LinearOpMode() {
    override fun runOpMode() {
        val _computer = hardwareMap.get("odometry") as GoBildaPinpointDriver

        _computer.recalibrateIMU()
        _computer.resetPosAndIMU()

        val leftForwardDrive = MotorOnly(hardwareMap.get("leftForwardDrive") as DcMotorEx)
        val leftBackDrive = MotorOnly(hardwareMap.get("leftBackDrive") as DcMotorEx)
        val rightForwardDrive = MotorOnly(hardwareMap.get("rightForwardDrive") as DcMotorEx)
        val rightBackDrive = MotorOnly(hardwareMap.get("rightBackDrive") as DcMotorEx)

        leftBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        leftForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        leftBackDrive.direction = DcMotorSimple.Direction.REVERSE
        leftForwardDrive.direction = DcMotorSimple.Direction.REVERSE
        rightForwardDrive.direction = DcMotorSimple.Direction.REVERSE

        waitForStart()
        resetRuntime()

        leftForwardDrive.power = 1.0
        leftBackDrive.power = 1.0
        rightForwardDrive.power = 1.0
        rightBackDrive.power = 1.0

        sleep(400)

        leftForwardDrive.power = 0.0
        leftBackDrive.power = 0.0
        rightForwardDrive.power = 0.0
        rightBackDrive.power = 0.0

        while (opModeIsActive()){}

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode(OpMode::class.simpleName)
    }
}