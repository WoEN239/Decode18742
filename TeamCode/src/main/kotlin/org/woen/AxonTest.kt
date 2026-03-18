package org.woen

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.Servo

@TeleOp
class AxonTest: LinearOpMode() {
    override fun runOpMode() {
        val turretRotateServo = hardwareMap.get("turretRotateServo") as Servo
        val turretRotateEncoder = hardwareMap.get("turretRotateEncoder") as AnalogInput

        waitForStart()
        resetRuntime()

        turretRotateServo.position = 0.5

        while (opModeIsActive()) {
            val telem = FtcDashboard.getInstance().telemetry

            telem.addData("voltage", turretRotateEncoder.voltage)

            telem.update()
        }
    }
}