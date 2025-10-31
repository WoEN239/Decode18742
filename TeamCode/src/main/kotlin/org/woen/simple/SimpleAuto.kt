package org.woen.simple

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import org.firstinspires.ftc.robotcore.internal.system.AppUtil

@Autonomous
class SimpleAuto: LinearOpMode() {
    override fun runOpMode() {
        val leftForwardDrive = hardwareMap.get("leftForwardDrive") as DcMotorEx

        leftForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        waitForStart()
        resetRuntime()

        leftForwardDrive.power = 1.0

        sleep(100)

        leftForwardDrive.power = 0.0

        while (opModeIsActive()){

        }

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode("SimpleTeleop")
    }
}