package org.woen.modules.drivetrain

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Gamepad
import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.collector.RunMode
import org.woen.modules.AddGamepad1ListenerEvent
import org.woen.modules.ClickGamepadListener
import org.woen.modules.IGamepadListener
import org.woen.utils.motor.MotorOnly
import org.woen.utils.regulator.Regulator
import org.woen.utils.regulator.RegulatorParameters
import org.woen.utils.units.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

@Config
internal object DRIVE_TRAIN_CONFIG {
    @JvmField
    var X_DEATH_ZONE = 0.05

    @JvmField
    var Y_DEATH_ZONE = 0.05

    @JvmField
    var H_DEATH_ZONE = 0.05

    @JvmField
    var VELOCITY_FORWARD_REGULATOR = RegulatorParameters(kF = 10.0, kP = 8.0)

    @JvmField
    var VELOCITY_SIDE_REGULATOR = RegulatorParameters(kF = 8.0, kP = 8.0)

    @JvmField
    var VELOCITY_ROTATE_REGULATOR = RegulatorParameters(kF = 2.0, kP = 1.2)

    @JvmField
    var PARKING_X_REGULATOR = RegulatorParameters(kP = 8.0)

    @JvmField
    var PARKING_Y_REGULATOR = RegulatorParameters(kP = 8.0)

    @JvmField
    var PARKING_H_REGULATOR = RegulatorParameters(kP = 1.2)
}

enum class DriveMode {
    REGULATOR,
    POWER
}

class SetDriveVelocityEvent(var linearVelocity: Vec2, var headingVelocity: Double)

fun attachDriveTrain(collector: Collector) {
    val leftForwardMotor = MotorOnly(collector.hardwareMap.get("leftForwardDrive") as DcMotorEx)
    val leftBackMotor = MotorOnly(collector.hardwareMap.get("leftBackDrive") as DcMotorEx)
    val rightBackMotor = MotorOnly(collector.hardwareMap.get("rightBackDrive") as DcMotorEx)
    val rightForwardMotor = MotorOnly(collector.hardwareMap.get("rightForwardDrive") as DcMotorEx)

    leftBackMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    leftForwardMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    rightBackMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    rightForwardMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

    leftBackMotor.direction = DcMotorSimple.Direction.REVERSE
    leftForwardMotor.direction = DcMotorSimple.Direction.REVERSE
    rightForwardMotor.direction = DcMotorSimple.Direction.REVERSE

    fun setPowers(
        leftFrontPower: Double,
        rightBackPower: Double,
        leftBackPower: Double,
        rightForwardPower: Double
    ) {
        var lfPower = leftFrontPower
        var rbPower = rightBackPower
        var lbPower = leftBackPower
        var rfPower = rightForwardPower

        val absMax = max(
            abs(lfPower),
            max(
                abs(rbPower),
                max(
                    abs(lbPower),
                    abs(rfPower)
                )
            )
        )

        if (absMax > 1.0) {
            lfPower /= absMax
            rbPower /= absMax
            lbPower /= absMax
            rfPower /= absMax
        }

        leftForwardMotor.power = lfPower
        rightBackMotor.power = rbPower
        leftBackMotor.power = lbPower
        rightForwardMotor.power = rfPower
    }

    val driveMode =
        if (collector.runMode == RunMode.MANUAL) DriveMode.POWER else DriveMode.REGULATOR

    var isParking = false

    val xRegulator = Regulator(DRIVE_TRAIN_CONFIG.PARKING_X_REGULATOR)
    val yRegulator = Regulator(DRIVE_TRAIN_CONFIG.PARKING_Y_REGULATOR)

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({it.dpad_up}, {
        isParking = !isParking

        if(isParking){
            xRegulator.start()
            yRegulator.start()
        }
    })))

    collector.eventBus.invoke(AddGamepad1ListenerEvent(object : IGamepadListener {
        override fun update(gamepadData: Gamepad) {
            if (driveMode == DriveMode.POWER && !isParking) {
                var ly = -gamepadData.left_stick_y.toDouble()
                var lx = -gamepadData.left_stick_x.toDouble()

                var rx = -gamepadData.right_stick_x.toDouble()

                fun calcDeathZone(value: Double, death: Double) = if (abs(value) < death) 0.0 else
                    (value - sign(value) * death) / (1.0 - death)

                ly = calcDeathZone(ly, DRIVE_TRAIN_CONFIG.X_DEATH_ZONE)
                lx = calcDeathZone(lx, DRIVE_TRAIN_CONFIG.Y_DEATH_ZONE)
                rx = calcDeathZone(rx, DRIVE_TRAIN_CONFIG.H_DEATH_ZONE)

                ly *= abs(ly)
                lx *= abs(lx)
                rx *= abs(rx)

                val odometry = collector.eventBus.invoke(GetRobotOdometry())

                val direction = Vec2(ly, lx)
                    .turn(-odometry.orientation.angle + (if (GameSettings.startOrientation.gameColor == GameColor.BLUE) -PI else PI) / 2.0)

                setPowers(
                    direction.x - direction.y - rx,
                    direction.x - direction.y + rx,
                    direction.x + direction.y - rx,
                    direction.x + direction.y + rx
                )
            }
        }
    }))

    val forwardRegulator = Regulator(DRIVE_TRAIN_CONFIG.VELOCITY_FORWARD_REGULATOR)
    val sideRegulator = Regulator(DRIVE_TRAIN_CONFIG.VELOCITY_SIDE_REGULATOR)
    val rotateRegulator = Regulator(DRIVE_TRAIN_CONFIG.VELOCITY_ROTATE_REGULATOR)

    val battery = collector.battery

    var targetLinearVelocity = Vec2.ZERO
    var targetHeadingVelocity = 0.0

    collector.eventBus.subscribe(SetDriveVelocityEvent::class) {
        targetLinearVelocity = it.linearVelocity
        targetHeadingVelocity = it.headingVelocity
    }

    collector.startEvent += {
        forwardRegulator.start()
        sideRegulator.start()
        rotateRegulator.start()
    }

    collector.updateEvent += {
        if (driveMode == DriveMode.REGULATOR) {
            val odometry = collector.eventBus.invoke(GetRobotOdometry())

            val velocityErr = targetLinearVelocity - odometry.linearVelocity

            val direction = Vec2(
                forwardRegulator.update(
                    velocityErr.x,
                    targetLinearVelocity.x,
                    collector.battery.currentVoltage
                ),
                sideRegulator.update(
                    velocityErr.y,
                    targetLinearVelocity.y,
                    collector.battery.currentVoltage
                )
            )

            val rotate = rotateRegulator.update(
                targetHeadingVelocity - odometry.headingVelocity,
                targetHeadingVelocity,
                collector.battery.currentVoltage
            )

            setPowers(
                battery.voltageToPower(direction.x - direction.y - rotate),
                battery.voltageToPower(direction.x - direction.y + rotate),
                battery.voltageToPower(direction.x + direction.y - rotate),
                battery.voltageToPower(direction.x + direction.y + rotate)
            )

            collector.telemetry.addData("target x vel", targetLinearVelocity.x)
            collector.telemetry.addData("target y vel", targetLinearVelocity.y)
            collector.telemetry.addData("target h vel", targetHeadingVelocity)
        }
        else if(isParking){
            val odometry = collector.eventBus.invoke(GetRobotOdometry())

            val err = (GameSettings.startOrientation.parkingOrientation.pos - odometry.orientation.pos)
                .turn(-odometry.orientation.angle)

            val direction = Vec2(xRegulator.update(err.x), yRegulator.update(err.y))

            var rotation = -collector.opMode.gamepad1.right_stick_x.toDouble()

            rotation = if (abs(rotation) < DRIVE_TRAIN_CONFIG.H_DEATH_ZONE) 0.0 else
                (rotation - sign(rotation) * DRIVE_TRAIN_CONFIG.H_DEATH_ZONE) / (1.0 - DRIVE_TRAIN_CONFIG.H_DEATH_ZONE)

            rotation *= rotation

            setPowers(
                direction.x - direction.y - rotation,
                direction.x - direction.y + rotation,
                direction.x + direction.y - rotation,
                direction.x + direction.y + rotation
            )
        }
    }
}