package org.woen.tests


import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.AnalogInput
import org.woen.telemetry.configs.Hardware



@TeleOp(group = "tests")
@Disabled
class OpticTest : LinearOpMode() {
    override fun runOpMode() {
        val optic1 =
            hardwareMap.get(Hardware.DEVICE_NAMES.TURRET_OPTIC_1) as AnalogInput
        val optic2 =
            hardwareMap.get(Hardware.DEVICE_NAMES.TURRET_OPTIC_2) as AnalogInput

        waitForStart()
        resetRuntime()

        while (opModeIsActive()) {
            val telem = FtcDashboard.getInstance().telemetry

            telem.addData("optic1", optic1.voltage)
            telem.addData("optic2", optic2.voltage)

            telem.update()
        }
    }
}