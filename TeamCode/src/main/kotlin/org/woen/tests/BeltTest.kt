package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

@TeleOp
class BeltTest: LinearOpMode() {
    override fun runOpMode() {
        val battery = hardwareMap.get(VoltageSensor::class.java, "Control Hub")
        val beltMotor = hardwareMap.get("beltMotor") as DcMotorEx

        waitForStart()
        resetRuntime()

        beltMotor.power = 1.0

        while (opModeIsActive()){
            val telem = FtcDashboard.getInstance().telemetry

            telem.addData("R", battery.voltage / beltMotor.getCurrent(CurrentUnit.AMPS))

            telem.update()
        }
    }
}