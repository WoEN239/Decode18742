package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp

@TeleOp
class PinPointTest: LinearOpMode() {
    override fun runOpMode() {
        val pinpoint = hardwareMap.get("odometry") as GoBildaPinpointDriver

        pinpoint.resetPosAndIMU()

        waitForStart()
        resetRuntime()

        while (opModeIsActive()){
            pinpoint.update()

            val telem = FtcDashboard.getInstance().telemetry

            telem.addData("hz", pinpoint.frequency)
            telem.addData("x odometr", pinpoint.encoderX)
            telem.addData("y odometr", pinpoint.encoderY)

            telem.update()
        }
    }
}