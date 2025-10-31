package org.woen.simple

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign

@Config
internal object SimpleConfig {
    @JvmField
    var BRUSH_REVERSE_TIME = 1.0

    @JvmField
    var START_SERVO_OPEN = 1.0

    @JvmField
    var START_SERVO_CLOSE = 0.0

    @JvmField
    var SHOOT_TIME = 1.0

    @JvmField
    var PUSH_SERVO_OPEN = 1.0

    @JvmField
    var PUSH_SERVO_CLOSE = 0.0

    @JvmField
    var PUSH_TIME = 1.0

    @JvmField
    var LONG_PULLEY_SPEED = 1.0

    @JvmField
    var SHORT_PULLEY_SPEED = 0.5

    @JvmField
    var IDLE_PULLEY_SPEED = 0.2
}

@TeleOp
class SimpleTeleop : LinearOpMode() {
    lateinit var leftForwardDrive: DcMotorEx
    lateinit var leftBackDrive: DcMotorEx
    lateinit var rightForwardDrive: DcMotorEx
    lateinit var rightBackDrive: DcMotorEx
    lateinit var brushMotor: DcMotorEx
    lateinit var startServo: Servo
    lateinit var pushServo: Servo
    lateinit var pulleyMotor: DcMotorEx

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

        val beltLeftMotor = hardwareMap.get("beltLeftMotor") as DcMotorEx
        val beltRightMotor = hardwareMap.get("beltRightMotor") as DcMotorEx

        beltLeftMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        beltRightMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        beltLeftMotor.power = 1.0
        beltRightMotor.power = -1.0

        startServo = hardwareMap.get("") as Servo
        pushServo = hardwareMap.get("") as Servo

        pulleyMotor = hardwareMap.get("pulleyMotor") as DcMotorEx

        pulleyMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        while (!isStarted()){
            if(gamepad1.left_bumper || gamepad1.square || gamepad1.right_bumper || gamepad1.circle ||
                abs(gamepad1.left_stick_y.toDouble()) > 0.01 || abs(gamepad1.left_stick_x.toDouble()) > 0.01
                || abs(gamepad1.right_stick_x.toDouble()) > 0.01)
                OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).startActiveOpMode()
        }

        resetRuntime()

        while (opModeIsActive()) {
            updateDriveTrain()
            updateBrush()
            updateCannon()
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

    var oldBrushButton = false
    var reverseTimer = ElapsedTime()
    var brushState = true

    fun updateBrush() {
        val brushButton = gamepad1.circle

        if (brushButton != oldBrushButton && brushButton) {
            brushState = !brushState

            if (!brushState)
                reverseTimer.reset()
        }

        oldBrushButton = brushButton

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

    fun updateCannon(){
        val shootLongButton = gamepad1.left_bumper
        val shootShortButton = gamepad1.square
        val shootButton = gamepad1.right_bumper

        if(shootLongButton != oldShootButton && shootLongButton)
            pulleyMotor.power = SimpleConfig.LONG_PULLEY_SPEED

        if(shootShortButton != oldShootShortButton && shootShortButton)
            pulleyMotor.power = SimpleConfig.SHORT_PULLEY_SPEED

        if(shootButton != oldShootButton) {
            startState = true
            shootTimer.reset()
        }

        oldShootLongButton = shootLongButton
        oldShootShortButton = shootShortButton
        oldShootButton = shootButton

        if(startState && shootTimer.seconds() > SimpleConfig.SHOOT_TIME)
            pushState = true

        if(pushState && shootTimer.seconds() > SimpleConfig.SHOOT_TIME + SimpleConfig.PUSH_TIME){
            startState = false
            pushState = false

            pulleyMotor.power = SimpleConfig.IDLE_PULLEY_SPEED
        }

        pushServo.position = if(pushState) SimpleConfig.PUSH_SERVO_OPEN else SimpleConfig.PUSH_SERVO_CLOSE
        startServo.position = if(startState) SimpleConfig.START_SERVO_OPEN else SimpleConfig.START_SERVO_CLOSE
    }
}