package org.woen.simple


import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign

import com.acmerobotics.dashboard.config.Config

import com.qualcomm.robotcore.util.ElapsedTime
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.eventloop.opmode.TeleOp

import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.VoltageSensor

import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit



@Config
internal object SimpleConfig {
    @JvmField
    var AUTO_PULLEY_SPEED = 9.1

    @JvmField
    var BRUSH_REVERSE_TIME = 0.6

    @JvmField
    var START_SERVO_OPEN = 0.9

    @JvmField
    var START_SERVO_CLOSE = 0.6

    @JvmField
    var PUSH_SERVO_OPEN = 0.2

    @JvmField
    var PUSH_SERVO_CLOSE = 0.65

    @JvmField
    var LONG_PULLEY_SPEED = 9.7

    @JvmField
    var SHORT_PULLEY_SPEED = 5.0

    @JvmField
    var IDLE_PULLEY_SPEED = 1.0

    @JvmField
    var CURRENT_TRIGER = 0.9

    @JvmField
    var CURRENT_TRIGER_TIME = 0.6

    @JvmField
    var DELAY_AFTER_FIRST_SHOT = 0.5

    @JvmField
    var FIRST_SHOT_DELAY_DURATION = 1.3

    @JvmField
    var PUSH_TIME = 1.2

    @JvmField
    var SHOOT_TIME = 2.2
}

@TeleOp
class SimpleTeleop : LinearOpMode() {
    lateinit var leftForwardDrive: DcMotorEx
    lateinit var leftBackDrive: DcMotorEx
    lateinit var rightForwardDrive: DcMotorEx
    lateinit var rightBackDrive: DcMotorEx

    lateinit var startServo: Servo
    lateinit var pushServo: Servo

    lateinit var brushMotor: DcMotorEx
    lateinit var pulleyMotor: DcMotorEx

    lateinit var battery: VoltageSensor

    lateinit var leftBeltMotor: DcMotorEx
    lateinit var rightBeltMotor: DcMotorEx



    var targetPulleyVoltage = SimpleConfig.IDLE_PULLEY_SPEED

    override fun runOpMode() {
        leftForwardDrive = hardwareMap.get("leftForwardDrive") as DcMotorEx
        leftBackDrive = hardwareMap.get("leftBackDrive") as DcMotorEx
        rightForwardDrive = hardwareMap.get("rightForwardDrive") as DcMotorEx
        rightBackDrive = hardwareMap.get("rightBackDrive") as DcMotorEx

        leftBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        leftForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightForwardDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightBackDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        rightForwardDrive.direction = DcMotorSimple.Direction.REVERSE
        rightBackDrive.direction = DcMotorSimple.Direction.REVERSE

        brushMotor = hardwareMap.get("brushMotor") as DcMotorEx

        brushMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        leftBeltMotor = hardwareMap.get("beltLeftMotor") as DcMotorEx
        rightBeltMotor = hardwareMap.get("beltRightMotor") as DcMotorEx

        leftBeltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        rightBeltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        startServo = hardwareMap.get("starterServo") as Servo
        pushServo = hardwareMap.get("pushServo") as Servo

        pulleyMotor = hardwareMap.get("pulleyMotor") as DcMotorEx

        pulleyMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        pulleyMotor.direction = DcMotorSimple.Direction.REVERSE

        battery = hardwareMap.get(VoltageSensor::class.java, "Control Hub")

        val angleServo = hardwareMap.get("angleServo") as Servo

        while (!isStarted()){
            if(gamepad1.left_bumper || gamepad1.square || gamepad1.right_bumper || gamepad1.circle ||
                abs(gamepad1.left_stick_y.toDouble()) > 0.01 || abs(gamepad1.left_stick_x.toDouble()) > 0.01
                || abs(gamepad1.right_stick_x.toDouble()) > 0.01)
                OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).startActiveOpMode()
        }

        resetRuntime()

        angleServo.position = 0.75

        pulleyMotor.power = SimpleConfig.IDLE_PULLEY_SPEED

        leftBeltMotor.power = 1.0
        rightBeltMotor.power = -1.0

        while (opModeIsActive()) {
            updateDriveTrain()
            updateBrush()
            updateCannon()

            telemetry.update()
        }
    }

    fun updateDriveTrain() {
        var ly = -gamepad1.left_stick_y.toDouble()
        var lx = -gamepad1.left_stick_x.toDouble()

        var rx = -gamepad1.right_stick_x.toDouble()

        ly = sign(ly) * (4.0 * (abs(ly) - 0.5).pow(3.0) + 0.5)
        lx = sign(lx) * (4.0 * (abs(lx) - 0.5).pow(3.0) + 0.5)
        rx = sign(rx) * (4.0 * (abs(rx) - 0.5).pow(3.0) + 0.5)

        var leftFrontPower = ly - lx - rx
        var rightBackPower = ly - lx + rx
        var leftBackPower = ly + lx - rx
        var rightForwardPower = ly + lx + rx

        val absMax = max(
            abs(leftFrontPower),
            max(
                abs(rightBackPower),
                max(
                    abs(leftBackPower),
                    abs(rightForwardPower)
                )
            )
        )

        if (absMax > 1.0) {
            leftFrontPower /= absMax
            rightBackPower /= absMax
            leftBackPower /= absMax
            rightForwardPower /= absMax
        }

        leftForwardDrive.power = leftFrontPower
        rightBackDrive.power = rightBackPower
        leftBackDrive.power = leftBackPower
        rightForwardDrive.power = rightForwardPower
    }

    var reverseTimer = ElapsedTime()
    var brushState = true
    var currentTimer = ElapsedTime()
    var shotDelayTimer = ElapsedTime()


    fun updateBrush() {
        if(brushMotor.getCurrent(CurrentUnit.AMPS) < SimpleConfig.CURRENT_TRIGER || !brushState)
            currentTimer.reset()

        if(currentTimer.seconds() > SimpleConfig.CURRENT_TRIGER_TIME) {
            brushState = false
            currentTimer.reset()
            reverseTimer.reset()
        }

        if (!brushState && reverseTimer.seconds() > SimpleConfig.BRUSH_REVERSE_TIME)
            brushState = true

        brushMotor.power = if (brushState) 1.0 else -1.0
    }

    var oldShootLongButton = false
    var oldShootShortButton = false
    var oldShootButton = false
    val shootTimer = ElapsedTime()
    var pushState = false
    var startState = false
    var isShooting = false
    var isWaitingSecondCharge = false
    var isCharging = false
    var isSorter = false

    fun updateCannon(){
        val shootLongButton = gamepad1.left_bumper
        val shootShortButton = gamepad1.square
        val shootButton = gamepad1.right_bumper

        if(shootLongButton != oldShootButton && shootLongButton) {
            targetPulleyVoltage = SimpleConfig.LONG_PULLEY_SPEED
            isShooting = true
        }

        if(shootShortButton != oldShootShortButton && shootShortButton) {
            targetPulleyVoltage = SimpleConfig.SHORT_PULLEY_SPEED
            isShooting = true
        }

        if(shootButton != oldShootButton && shootButton && isShooting && !isSorter) {
            startState = true
            shootTimer.reset()
            shotDelayTimer.reset()
            isCharging = true
            isSorter = true
        }

        oldShootLongButton = shootLongButton
        oldShootShortButton = shootShortButton
        oldShootButton = shootButton

        if(startState && shootTimer.seconds() > SimpleConfig.SHOOT_TIME + SimpleConfig.FIRST_SHOT_DELAY_DURATION)
            pushState = true

        if(pushState && shootTimer.seconds() > SimpleConfig.SHOOT_TIME + SimpleConfig.FIRST_SHOT_DELAY_DURATION + SimpleConfig.PUSH_TIME){
            startState = false
            pushState = false

            targetPulleyVoltage = SimpleConfig.IDLE_PULLEY_SPEED
            isShooting = false
            isSorter = false
        }

        if (startState && !isWaitingSecondCharge && isCharging &&
            shotDelayTimer.seconds() > SimpleConfig.DELAY_AFTER_FIRST_SHOT)
        {
            isWaitingSecondCharge = true
            isCharging = false
            shotDelayTimer.reset()

            leftBeltMotor.power = 0.0
            rightBeltMotor.power = 0.0
        }
        if (isWaitingSecondCharge &&
            shotDelayTimer.seconds() > SimpleConfig.FIRST_SHOT_DELAY_DURATION)
        {
            leftBeltMotor.power = 1.0
            rightBeltMotor.power = -1.0

            isWaitingSecondCharge = false
            isCharging = false

            shotDelayTimer.reset()
        }

        pushServo.position = if(pushState) SimpleConfig.PUSH_SERVO_OPEN else SimpleConfig.PUSH_SERVO_CLOSE
        startServo.position = if(startState) SimpleConfig.START_SERVO_OPEN else SimpleConfig.START_SERVO_CLOSE
        pulleyMotor.power = targetPulleyVoltage / battery.voltage
    }
}