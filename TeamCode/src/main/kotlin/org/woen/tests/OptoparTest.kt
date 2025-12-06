package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.AnalogInput
import org.woen.telemetry.Configs

@TeleOp
class OptoparTest : LinearOpMode() {
    override fun runOpMode() {
        val optopar1 =
            hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.TURRET_OPTIC_PARE_1) as AnalogInput
        val optopar2 =
            hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.TURRET_OPTIC_PARE_2) as AnalogInput

        waitForStart()
        resetRuntime()

        while (opModeIsActive()) {
            val telem = FtcDashboard.getInstance().telemetry

            telem.addData("optoppar1", optopar1.voltage)
            telem.addData("optoppar2", optopar2.voltage)

            telem.update()
        }
    }
}