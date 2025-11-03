package org.woen.simple

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.VoltageSensor
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.internal.system.AppUtil

@Autonomous
@Disabled
class SimpleAuto : LinearOpMode() {
    override fun runOpMode() {
        val leftForwardDrive = hardwareMap.get("leftForwardDrive") as DcMotorEx
        val leftBackDrive = hardwareMap.get("leftBackDrive") as DcMotorEx
        val rightForwardDrive = hardwareMap.get("rightForwardDrive") as DcMotorEx
        val rightBackDrive = hardwareMap.get("rightBackDrive") as DcMotorEx

        leftBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        leftForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        rightForwardDrive.direction = DcMotorSimple.Direction.REVERSE
        rightBackDrive.direction = DcMotorSimple.Direction.REVERSE

        val pulleyMotor = hardwareMap.get("pulleyMotor") as DcMotorEx

        pulleyMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

//        pulleyMotor.direction = DcMotorSimple.Direction.REVERSE

        val battery = hardwareMap.get(VoltageSensor::class.java, "Control Hub")

        val shootTimer = ElapsedTime()
        var pushState = false
        var startState = false
        var isWaitingSecondCharge = false
        var isCharging = false

        val shotDelayTimer = ElapsedTime()

        val leftBeltMotor = hardwareMap.get("beltLeftMotor") as DcMotorEx
        val rightBeltMotor = hardwareMap.get("beltRightMotor") as DcMotorEx

        leftBeltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightBeltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        val startServo = hardwareMap.get("starterServo") as Servo
        val pushServo = hardwareMap.get("pushServo") as Servo

        val angleServo = hardwareMap.get("angleServo") as Servo

        waitForStart()
        resetRuntime()

        angleServo.position = 0.75

        leftBeltMotor.power = 1.0
        rightBeltMotor.power = -1.0

        pushServo.position = SimpleConfig.PUSH_SERVO_CLOSE
        startServo.position = SimpleConfig.START_SERVO_CLOSE

        val timer = ElapsedTime()
        timer.reset()

        while (opModeIsActive() && timer.seconds() < 2.0)
            pulleyMotor.power = SimpleConfig.AUTO_PULLEY_SPEED / battery.voltage

        shootTimer.reset()
        shotDelayTimer.reset()
        isCharging = true
        startState = true
        timer.reset()
        pushServo.position = SimpleConfig.PUSH_SERVO_CLOSE
        startServo.position = SimpleConfig.START_SERVO_OPEN

        while (opModeIsActive() && timer.seconds() < 10.0) {
            pulleyMotor.power = SimpleConfig.AUTO_PULLEY_SPEED / battery.voltage

            if (startState && shootTimer.seconds() > SimpleConfig.SHOOT_TIME + SimpleConfig.FIRST_SHOT_DELAY_DURATION)
                pushState = true

            if (pushState && shootTimer.seconds() > SimpleConfig.SHOOT_TIME + SimpleConfig.FIRST_SHOT_DELAY_DURATION + SimpleConfig.PUSH_TIME) {
                startState = false
                pushState = false
            }

            if (startState && !isWaitingSecondCharge && isCharging &&
                shotDelayTimer.seconds() > SimpleConfig.DELAY_AFTER_FIRST_SHOT
            ) {
                isWaitingSecondCharge = true
                isCharging = false
                shotDelayTimer.reset()

                leftBeltMotor.power = 0.0
                rightBeltMotor.power = 0.0
            }
            if (isWaitingSecondCharge &&
                shotDelayTimer.seconds() > SimpleConfig.FIRST_SHOT_DELAY_DURATION
            ) {
                leftBeltMotor.power = 1.0
                rightBeltMotor.power = -1.0

                isWaitingSecondCharge = false
                isCharging = false

                shotDelayTimer.reset()
            }

            pushServo.position =
                if (pushState) SimpleConfig.PUSH_SERVO_OPEN else SimpleConfig.PUSH_SERVO_CLOSE
            startServo.position =
                if (startState) SimpleConfig.START_SERVO_OPEN else SimpleConfig.START_SERVO_CLOSE
        }

        leftBackDrive.power = 1.0
        leftForwardDrive.power = 1.0
        rightForwardDrive.power = 1.0
        rightBackDrive.power = 1.0

        sleep(500)

        leftBackDrive.power = 0.0
        leftForwardDrive.power = 0.0
        rightForwardDrive.power = 0.0
        rightBackDrive.power = 0.0

        while (opModeIsActive()) {

        }

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode("SimpleTeleop")
    }
}