package org.woen.linearOpModes

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp

@TeleOp
class TestOpMode: LinearOpMode() {
    override fun runOpMode() {
        waitForStart()
        resetRuntime()

        while (opModeIsActive()){
            sleep(20)

            FtcDashboard.getInstance().telemetry.addData("a", System.currentTimeMillis())

            FtcDashboard.getInstance().telemetry.update()
        }
    }
}