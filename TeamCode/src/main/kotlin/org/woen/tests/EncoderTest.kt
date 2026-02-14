package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx

@TeleOp(group = "tests")
@Disabled
class EncoderTest: LinearOpMode() {
    override fun runOpMode() {
        val motor1 = hardwareMap.get("turretRotateEncoder") as DcMotorEx
        val motor2 = hardwareMap.get("pulleyMotor") as DcMotorEx

        waitForStart()
        resetRuntime()

        while (opModeIsActive()){
            val telem = FtcDashboard.getInstance().telemetry

            telem.addData("encoder1", motor1.currentPosition)
            telem.addData("encoder2", motor2.currentPosition)

            telem.update()
        }
    }
}