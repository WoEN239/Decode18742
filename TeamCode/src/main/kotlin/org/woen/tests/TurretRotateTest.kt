package org.woen.tests

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo

@Config
internal object TURRET_ROTATE_TEST{
    @JvmField
    var POSITION = 0.6
}

@TeleOp(group = "tests")
class TurretRotateTest: LinearOpMode() {
    override fun runOpMode() {
        val serv = hardwareMap.get("turretRotateServo") as Servo

        (serv as PwmControl).pwmRange = PwmControl.PwmRange(500.0, 2500.0)

        waitForStart()
        resetRuntime()

        while (opModeIsActive()){
            serv.position = TURRET_ROTATE_TEST.POSITION
        }
    }
}