package org.woen.tests

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.Servo

@Config(value = "HOME_MADE_AXON")
@TeleOp
class HomeMadeAxon: LinearOpMode() {
    companion object{
        @JvmField
        var POWER = 0.0
    }

    override fun runOpMode() {
        val servoLeft = hardwareMap.get("leftServo") as Servo
        val servoCentral = hardwareMap.get("centralServo") as Servo
        val servoRight = hardwareMap.get("rightServo") as Servo

        servoCentral.direction = Servo.Direction.REVERSE

        waitForStart()
        resetRuntime()

        while (opModeIsActive()){
            servoLeft.position = POWER / 2.0 + 0.5
            servoRight.position = servoLeft.position
            servoCentral.position = servoLeft.position
        }
    }
}