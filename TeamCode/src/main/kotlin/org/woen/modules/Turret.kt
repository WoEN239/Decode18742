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
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.Collector
import org.woen.collector.GAME_CONFIGS
import org.woen.collector.GameSettings
import org.woen.modules.drivetrain.GetRobotOdometry
import org.woen.utils.units.Angle
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.pow

@Config
internal object TURRET_CONFIG {
    @JvmField
    var MAX_ANGLE_SERVO = 0.43

    @JvmField
    var MIN_ANGLE_SERVO = 0.28

    @JvmField
    var MAX_ANGLE = PI / 2.0 - toRadians(25.0)

    @JvmField
    var MIN_ANGLE = PI / 2.0 - toRadians(45.0)

    @JvmField
    var MAX_HEADING = toRadians(90.0)

    @JvmField
    var MIN_HEADING = toRadians(-90.0)

//    @JvmField
//    var HEADING_TARGET_TIMER = 0.1

    @JvmField
    var HEADING_SERVO_MAX_ANGLE = PI * 1.5

    @JvmField
    var TURRET_CENTER_POS = Vec2(0.0, 0.38 / 2.0 - 0.26)

    @JvmField
    var ZERO_HEADING_POS = Angle.ofDeg(270.0 / 2.0 / 2.0)

    @JvmField
    var HEADING_SERVO_RATIO = 0.5

    @JvmField
    var PULLEY_RADIUS = 0.075 / 2.0

    @JvmField
    var PULLEY_TICKS_REVOLUTION = 28.0

    @JvmField
    var PULLEY_REGULATOR = PIDFCoefficients(110.0, 1.5, 0.0, 15.5)

    @JvmField
    var PULLEY_RATION = 37.0 / 33.0

    @JvmField
    var GRAVITY_G = 9.80665

    @JvmField
    var SCORE_HEIGHT = 1.12

    @JvmField
    var TURRET_HEIGHT = 0.33

    @JvmField
    var SCORE_ANGLE = toRadians(-20.0)

    @JvmField
    var PULLEY_U = 0.415

//    @JvmField
//    var LONG_PULLEY_VELOCITY = 19.0
//
//    @JvmField
//    var LONG_ANGLE_POSITION = toRadians(52.0)

    @JvmField
    var PULLEY_VELOCITY = 10.0

    @JvmField
    var ANGLE_POSITION = 0.0

    @JvmField
    var VELOCITY_HEADING_COMPENSATE_K = 0.0

    @JvmField
    var VELOCITY_PULLEY_COMPENSATE_K = 0.0

    @JvmField
    var VELOCITY_ANGLE_COMPENSATE_K = 0.0

    @JvmField
    var SHORT_PULLEY_VELOCITY = 10.5

    @JvmField
    var SHORT_ANGLE_POSITION = 0.28

    @JvmField
    var LONG_PULLEY_VELOCITY = 14.9

    @JvmField
    var LONG_ANGLE_POSITION = 0.43

    @JvmField
    var LONG_DISTANCE = 2.63

    @JvmField
    var SHORT_DISTANCE = 1.0
}
//
//object PulleyRegulatorConfig {
//    var pulleyRegulator = PIDFCoefficients()
//    val changed = SimpleEmptyEvent()
//
//    @OnCreate
//    @JvmStatic
//    fun start(context: Context?) {
//        FtcDashboard.start(context)
//
//        val dashboard = FtcDashboard.getInstance()
//
//        dashboard.withConfigRoot {
//            dashboard.addConfigVariable(
//                TURRET_CONFIG::class.simpleName,
//                "PULLEY_REGULATOR",
//                object : ValueProvider<PIDFCoefficients> {
//                    override fun get() = pulleyRegulator
//
//                    override fun set(value: PIDFCoefficients?) {
//                        if (value != null) {
//                            pulleyRegulator = value
//                            changed.invoke()
//                        }
//                    }
//                })
//        }
//    }
//}

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
//    angleServo.direction = Servo.Direction.REVERSE

    pulleyMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    pulleyMotor.mode = DcMotor.RunMode.RUN_USING_ENCODER

    var state = TurretState.TO_BASKET

    headingServo.pwmRange = PwmControl.PwmRange(500.0, 2500.0)
    headingServo.direction = Servo.Direction.REVERSE

    var turretHeading = 0.0
    val turretHeadingTargetTimer = ElapsedTime()

    collector.startEvent += {
        turretHeadingTargetTimer.reset()
    }

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

        val basketErrHeading =
            (GameSettings.startOrientation.basketPosition.invoke() - odometry.linearVelocity.turn(
                odometry.orientation.angle
            ) * TURRET_CONFIG.VELOCITY_HEADING_COMPENSATE_K) - (odometry.orientation.pos + TURRET_CONFIG.TURRET_CENTER_POS
                .turn(odometry.orientation.angle))

        val basketErrAngle =
            (GameSettings.startOrientation.basketPosition.invoke() - odometry.linearVelocity.turn(
                odometry.orientation.angle
            ) * TURRET_CONFIG.VELOCITY_ANGLE_COMPENSATE_K) - (odometry.orientation.pos + TURRET_CONFIG.TURRET_CENTER_POS
                .turn(odometry.orientation.angle))

        val basketErrPulley =
            (GameSettings.startOrientation.basketPosition.invoke() - odometry.linearVelocity.turn(
                odometry.orientation.angle
            ) * TURRET_CONFIG.VELOCITY_PULLEY_COMPENSATE_K) - (odometry.orientation.pos + TURRET_CONFIG.TURRET_CENTER_POS
                .turn(odometry.orientation.angle))

        var pulleyVelocity: Double
        var angle: Double

//        if (!odometry.locateInLongShootingArea) {
//            val y = TURRET_CONFIG.SCORE_HEIGHT - TURRET_CONFIG.TURRET_HEIGHT
//            val x = basketErr.length()
//
//            val alpha = atan((2 * y / x) - tan(TURRET_CONFIG.SCORE_ANGLE))
//
//            val v0 =
//                sqrt((TURRET_CONFIG.GRAVITY_G * x.pow(2)) / (2 * cos(alpha).pow(2) * (x * tan(alpha) - y)))
//
//            val t = x / (v0 * cos(alpha))
//
//            val robotGlobalVelocity =
//                odometry.linearVelocity.turn(odometry.orientation.angle)
//
//            val robotV = robotGlobalVelocity.length()
//            val difH = robotGlobalVelocity.rot() - basketErr.rot()
//
//            val vR = -cos(difH) * robotV
//            val vT = sin(difH) * robotV
//
//            val vxComp = x / t + vR
//
//            val vXNew = sqrt(vxComp.pow(2) + vT.pow(2))
//            val vY = v0 * sin(alpha)
//
//            val newX = vXNew * t
//
//            angle = clamp(atan(vY / vXNew), TURRET_CONFIG.MIN_ANGLE, TURRET_CONFIG.MAX_ANGLE)
//
//            pulleyVelocity =
//                sqrt(
//                    (TURRET_CONFIG.GRAVITY_G * newX.pow(2)) / (2.0 * cos(angle)
//                        .pow(2)
//                            * (newX * tan(angle) - y))
//                ) / TURRET_CONFIG.PULLEY_U
//        } else {
//            pulleyVelocity = TURRET_CONFIG.LONG_PULLEY_VELOCITY
//            angle = TURRET_CONFIG.LONG_ANGLE_POSITION
//        }
//
//        if (pulleyVelocity.isNaN() || pulleyVelocity == Double.POSITIVE_INFINITY || pulleyVelocity == Double.NEGATIVE_INFINITY)
//            pulleyVelocity = 17.0

        val lp = clamp(basketErrPulley.length(), 1.0, 2.71)

        pulleyVelocity = -0.229458 * lp.pow(2) + 3.01503 * lp + 8.01443
//        angle = TURRET_CONFIG.ANGLE_POSITION

//        collector.telemetry.addData("lenght", l)

//        angleServo.position = clamp(
//            (angle - TURRET_CONFIG.MIN_ANGLE) /
//                    (TURRET_CONFIG.MAX_ANGLE - TURRET_CONFIG.MIN_ANGLE) *
//                    (TURRET_CONFIG.MAX_ANGLE_SERVO - TURRET_CONFIG.MIN_ANGLE_SERVO) +
//                    TURRET_CONFIG.MIN_ANGLE_SERVO,
//            TURRET_CONFIG.MIN_ANGLE_SERVO,
//            TURRET_CONFIG.MAX_ANGLE_SERVO
//        )

        val la = clamp(basketErrAngle.length(), 1.0, 2.71)

//        angleServo.position = clamp(
//            -0.030783 * la.pow(2) + 0.175608 * la + 0.135175, TURRET_CONFIG.MIN_ANGLE_SERVO,
//            TURRET_CONFIG.MAX_ANGLE_SERVO
//        )
//
//        pulleyMotor.velocity =
//            pulleyVelocity / (2.0 * PI * TURRET_CONFIG.PULLEY_RADIUS) * TURRET_CONFIG.PULLEY_TICKS_REVOLUTION / TURRET_CONFIG.PULLEY_RATION

        val vel = (TURRET_CONFIG.SHORT_PULLEY_VELOCITY + clamp(
            (la - TURRET_CONFIG.SHORT_DISTANCE) / (TURRET_CONFIG.LONG_DISTANCE - TURRET_CONFIG.SHORT_DISTANCE),
            0.0,
            1.0
        ) * (TURRET_CONFIG.LONG_PULLEY_VELOCITY - TURRET_CONFIG.SHORT_PULLEY_VELOCITY))

        pulleyMotor.velocity = vel / (2.0 * PI * TURRET_CONFIG.PULLEY_RADIUS) * TURRET_CONFIG.PULLEY_TICKS_REVOLUTION / TURRET_CONFIG.PULLEY_RATION

        angleServo.position = TURRET_CONFIG.SHORT_ANGLE_POSITION + clamp(
            (la - TURRET_CONFIG.SHORT_DISTANCE) / (TURRET_CONFIG.LONG_DISTANCE - TURRET_CONFIG.SHORT_DISTANCE),
            0.0,
            1.0
        ) * (TURRET_CONFIG.LONG_ANGLE_POSITION - TURRET_CONFIG.SHORT_ANGLE_POSITION)

        turretHeading = if (state != TurretState.CALIBRATE_ODOMETRY) {
            (when (state) {
                TurretState.TO_OBELISK -> Angle(
                    (GAME_CONFIGS.OBELISK_POSITION - (odometry.orientation.pos +
                            TURRET_CONFIG.TURRET_CENTER_POS.turn(odometry.orientation.angle))).rot() + PI
                )

                TurretState.TO_BASKET -> Angle(basketErrHeading.rot() + PI)

                else -> Angle.ZERO
            } - odometry.orientation.angl).angle
        } else
            0.0

        headingServo.position = (clamp(
            turretHeading, TURRET_CONFIG.MIN_HEADING, TURRET_CONFIG.MAX_HEADING
        ) + TURRET_CONFIG.ZERO_HEADING_POS.angle) / TURRET_CONFIG.HEADING_SERVO_RATIO / TURRET_CONFIG.HEADING_SERVO_MAX_ANGLE

        collector.telemetry.addData("lenght", la)
        collector.telemetry.addData("target pulleyVelocity", vel)
        collector.telemetry.addData("anglePos", angleServo.position)
        collector.telemetry.addData(
            "current pulley velocity",
            pulleyMotor.velocity / TURRET_CONFIG.PULLEY_TICKS_REVOLUTION * 2.0 * PI * TURRET_CONFIG.PULLEY_RADIUS * TURRET_CONFIG.PULLEY_RATION
        )
        collector.telemetry.drawCircle(
            GameSettings.startOrientation.basketPosition.invoke(),
            0.1,
            Color.ORANGE
        )
    }
}