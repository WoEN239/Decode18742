package org.woen.tests

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoImplEx

@TeleOp
@Config(value = "TURRET_ROTATE_TEST")
class TurretRotateTest: LinearOpMode() {
    companion object{
        @JvmField
        var TURRET_POSITION = 0.5

        @JvmField
        var SERVO_DIFFERENCE = 0.0
    }

    override fun runOpMode() {
        val servo1 = hardwareMap.get("turretRotateServo1") as Servo
        val servo2 = hardwareMap.get("turretRotateServo2") as Servo

        (servo1 as ServoImplEx).pwmRange = PwmControl.PwmRange(500.0, 2500.0)
        (servo2 as ServoImplEx).pwmRange = PwmControl.PwmRange(500.0, 2500.0)

        waitForStart()
        resetRuntime()

        while (opModeIsActive()){
            servo1.position = TURRET_POSITION
            servo2.position = TURRET_POSITION + SERVO_DIFFERENCE
        }
    }
}