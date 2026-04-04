package org.woen.modules

import androidx.core.math.MathUtils.clamp
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoImplEx
import org.woen.collector.Collector
import org.woen.collector.GAME_CONFIGS
import org.woen.collector.GameColor
import org.woen.collector.GamePosition
import org.woen.collector.GameSettings
import org.woen.collector.RunMode
import org.woen.modules.drivetrain.GetRobotOdometry
import org.woen.utils.units.Angle
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2
import java.lang.Math.toRadians
import kotlin.math.PI

@Config
internal object TURRET_CONFIG {
    @JvmField
    var MAX_ANGLE_SERVO = 0.45

    @JvmField
    var MIN_ANGLE_SERVO = 0.3

    @JvmField
    var MAX_HEADING = toRadians(90.0)

    @JvmField
    var MIN_HEADING = toRadians(-90.0)

    @JvmField
    var HEADING_SERVO_MAX_ANGLE = PI * 1.5

    @JvmField
    var TURRET_CENTER_POS = Vec2(0.0, -0.035)

    @JvmField
    var ZERO_HEADING_POS = Angle.ofDeg(270.0 / 2.0 / 2.0)

    @JvmField
    var HEADING_SERVO_RATIO = 0.5

    @JvmField
    var PULLEY_RADIUS = 0.075 / 2.0

    @JvmField
    var PULLEY_TICKS_REVOLUTION = 28.0

    @JvmField
    var PULLEY_REGULATOR = PIDFCoefficients(195.0, 11.0, 0.0, 15.5)

    @JvmField
    var PULLEY_RATION = 37.0 / 33.0

    @JvmField
    var VELOCITY_PULLEY_COMPENSATE_K = 0.3

    @JvmField
    var LINEAR_VELOCITY_HEADING_COMPENSATE_K = 0.35

    @JvmField
    var ANGULAR_VELOCITY_HEADING_COMPENSATE_K = 0.11

    @JvmField
    var CLOSE_PULLEY_VELOCITY = 10.5

    @JvmField
    var CLOSE_ANGLE_POSITION = 0.305

    @JvmField
    var SHORT_FAR_PULLEY_VELOCITY = 13.9

    @JvmField
    var SHORT_FAR_ANGLE_POSITION = 0.42

    @JvmField
    var SHORT_FAR_DISTANCE = 2.58

    @JvmField
    var CLOSE_DISTANCE = 1.1

    @JvmField
    var FAR_PULLEY_VELOCITY = 18.7

    @JvmField
    var FAR_ANGLE_POSITION = 0.45

    @JvmField
    var LONG_CLOSE_PULLEY_VELOCITY = 16.35

    @JvmField
    var LONG_CLOSE_ANGLE_POSITION = 0.428

    @JvmField
    var LONG_CLOSE_DISTANCE = 3.0

    @JvmField
    var FAR_DISTANCE = 4.1
//
//    @JvmField
//    var ANGLE_POSITION = 0.5
//
//    @JvmField
//    var PULLEY_VELOCITY = 10.0
}

enum class TurretState {
    TO_OBELISK,
    TO_BASKET,
    CALIBRATE_ODOMETRY
}

data class SetTurretStateEvent(val state: TurretState)
data class GetTurretStateEvent(var state: TurretState = TurretState.TO_BASKET)
data class GetTurretHeadingEvent(var heading: Angle = Angle.ZERO)
data class GetTurretHeadingIsNormalEvent(var normal: Boolean = false)

fun attachTurret(collector: Collector) {
    val angleServo = collector.hardwareMap.get("turretAngleServo") as Servo
    val pulleyMotor = collector.hardwareMap.get("pulleyMotor") as DcMotorEx
    val headingServo = collector.hardwareMap.get("turretRotateServo") as ServoImplEx

    pulleyMotor.direction = DcMotorSimple.Direction.REVERSE

    pulleyMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    pulleyMotor.mode = DcMotor.RunMode.RUN_USING_ENCODER

    var state = TurretState.TO_BASKET

    headingServo.pwmRange = PwmControl.PwmRange(500.0, 2500.0)
    headingServo.direction = Servo.Direction.REVERSE

    var turretHeading = 0.0

    collector.eventBus.subscribe(GetTurretStateEvent::class) {
        it.state = state
    }

    collector.eventBus.subscribe(SetTurretStateEvent::class) {
        state = it.state
    }

    collector.eventBus.subscribe(GetTurretHeadingEvent::class) {
        it.heading =
            Angle(clamp(turretHeading, TURRET_CONFIG.MIN_HEADING, TURRET_CONFIG.MAX_HEADING))
    }

    collector.eventBus.subscribe(GetTurretHeadingIsNormalEvent::class) {
        it.normal = turretHeading in TURRET_CONFIG.MIN_HEADING..TURRET_CONFIG.MAX_HEADING
    }

    collector.updateEvent += {
        pulleyMotor.setPIDFCoefficients(
            DcMotor.RunMode.RUN_USING_ENCODER,
            TURRET_CONFIG.PULLEY_REGULATOR
        )

        val odometry = collector.eventBus.invoke(GetRobotOdometry())

        val basketErrPulley =
            ((if (odometry.orientation.x < 0.5) GameSettings.startOrientation.basketPosition else GameSettings.startOrientation.farBasketPosition).invoke() -
                    odometry.linearVelocity.turn(odometry.orientation.angle) * TURRET_CONFIG.VELOCITY_PULLEY_COMPENSATE_K) - (odometry.orientation.pos + TURRET_CONFIG.TURRET_CENTER_POS
                .turn(odometry.orientation.angle))

        var l = basketErrPulley.length()

        val targetPulleyVelocity: Double
        val anglePosition: Double

        if (collector.runMode == RunMode.MANUAL || GameSettings.startOrientation.gamePosition == GamePosition.FAR) {
            if (odometry.orientation.x < 0.5) {
                l = clamp(l, TURRET_CONFIG.CLOSE_DISTANCE, TURRET_CONFIG.SHORT_FAR_DISTANCE)

                targetPulleyVelocity = (TURRET_CONFIG.CLOSE_PULLEY_VELOCITY + clamp(
                    (l - TURRET_CONFIG.CLOSE_DISTANCE) /
                            (TURRET_CONFIG.SHORT_FAR_DISTANCE - TURRET_CONFIG.CLOSE_DISTANCE),
                    0.0,
                    1.0
                ) * (TURRET_CONFIG.SHORT_FAR_PULLEY_VELOCITY - TURRET_CONFIG.CLOSE_PULLEY_VELOCITY))

                anglePosition = TURRET_CONFIG.CLOSE_ANGLE_POSITION + clamp(
                    (l - TURRET_CONFIG.CLOSE_DISTANCE) /
                            (TURRET_CONFIG.SHORT_FAR_DISTANCE - TURRET_CONFIG.CLOSE_DISTANCE),
                    0.0,
                    1.0
                ) * (TURRET_CONFIG.SHORT_FAR_ANGLE_POSITION - TURRET_CONFIG.CLOSE_ANGLE_POSITION)
            } else {
                l = clamp(l, TURRET_CONFIG.LONG_CLOSE_DISTANCE, TURRET_CONFIG.FAR_DISTANCE)

                targetPulleyVelocity = (TURRET_CONFIG.LONG_CLOSE_PULLEY_VELOCITY + clamp(
                    (l - TURRET_CONFIG.LONG_CLOSE_DISTANCE) /
                            (TURRET_CONFIG.FAR_DISTANCE - TURRET_CONFIG.LONG_CLOSE_DISTANCE),
                    0.0,
                    1.0
                ) * (TURRET_CONFIG.FAR_PULLEY_VELOCITY - TURRET_CONFIG.LONG_CLOSE_PULLEY_VELOCITY))

                anglePosition = TURRET_CONFIG.LONG_CLOSE_ANGLE_POSITION + clamp(
                    (l - TURRET_CONFIG.LONG_CLOSE_DISTANCE) /
                            (TURRET_CONFIG.FAR_DISTANCE - TURRET_CONFIG.LONG_CLOSE_DISTANCE),
                    0.0,
                    1.0
                ) * (TURRET_CONFIG.FAR_ANGLE_POSITION - TURRET_CONFIG.LONG_CLOSE_ANGLE_POSITION)
            }
        } else {
            anglePosition = 0.3//TURRET_CONFIG.ANGLE_POSITION// 0.3
            targetPulleyVelocity = 10.7//TURRET_CONFIG.PULLEY_VELOCITY//10.4
        }

        pulleyMotor.velocity =
            targetPulleyVelocity / (2.0 * PI * TURRET_CONFIG.PULLEY_RADIUS) * TURRET_CONFIG.PULLEY_TICKS_REVOLUTION / TURRET_CONFIG.PULLEY_RATION

        angleServo.position = clamp(
            anglePosition,
            TURRET_CONFIG.MIN_ANGLE_SERVO,
            TURRET_CONFIG.MAX_ANGLE_SERVO
        )

        val basketErrHeading =
            ((if (odometry.orientation.x < 0.5) GameSettings.startOrientation.basketPosition else GameSettings.startOrientation.farBasketPosition).invoke() - odometry.linearVelocity.turn(
                odometry.orientation.angle
            ) * if(collector.runMode == RunMode.MANUAL) TURRET_CONFIG.LINEAR_VELOCITY_HEADING_COMPENSATE_K else 0.0) - (odometry.orientation.pos + TURRET_CONFIG.TURRET_CENTER_POS
                .turn(odometry.orientation.angle))

        turretHeading = if (state != TurretState.CALIBRATE_ODOMETRY) {
            (when (state) {
                TurretState.TO_OBELISK -> Angle(
                    ((if(GameSettings.startOrientation.gameColor == GameColor.RED) GAME_CONFIGS.RED_OBELISK_POSITION else GAME_CONFIGS.BLUE_OBELISK_POSITION) - (odometry.orientation.pos +
                            TURRET_CONFIG.TURRET_CENTER_POS.turn(odometry.orientation.angle))).rot() + PI
                )

                TurretState.TO_BASKET -> Angle(basketErrHeading.rot() + PI - odometry.headingVelocity * TURRET_CONFIG.ANGULAR_VELOCITY_HEADING_COMPENSATE_K)

                else -> Angle.ZERO
            } - odometry.orientation.angl).angle
        } else
            0.0

        headingServo.position = (clamp(
            turretHeading, TURRET_CONFIG.MIN_HEADING, TURRET_CONFIG.MAX_HEADING
        ) + TURRET_CONFIG.ZERO_HEADING_POS.angle) / TURRET_CONFIG.HEADING_SERVO_RATIO / TURRET_CONFIG.HEADING_SERVO_MAX_ANGLE

        collector.telemetry.addData("length", l)
        collector.telemetry.addData("target pulley velocity", targetPulleyVelocity)
        collector.telemetry.addData("anglePos", angleServo.position)
        collector.telemetry.addData(
            "current pulley velocity",
            pulleyMotor.velocity / TURRET_CONFIG.PULLEY_TICKS_REVOLUTION * 2.0 * PI * TURRET_CONFIG.PULLEY_RADIUS * TURRET_CONFIG.PULLEY_RATION
        )
        collector.telemetry.drawCircle(
            (if (odometry.orientation.x < 0.5) GameSettings.startOrientation.basketPosition else GameSettings.startOrientation.farBasketPosition).invoke(), 0.1, Color.ORANGE
        )
    }
}