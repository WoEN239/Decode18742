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
import org.woen.utils.drivers.InfinityAxon
import org.woen.utils.units.Angle
import org.woen.utils.units.Vec2
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

@Config
internal object TURRET_CONFIG {
    @JvmField
    var MAX_ANGLE_SERVO = 1.0 - 0.33

    @JvmField
    var MIN_ANGLE_SERVO = 1.0 - 0.48

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
    var PULLEY_REGULATOR = PIDFCoefficients(100.0, 1.5, 0.0, 15.5)

    @JvmField
    var PULLEY_RATION = 35.0 / 34.0

    @JvmField
    var GRAVITY_G = 9.80665

    @JvmField
    var SCORE_HEIGHT = 1.12

    @JvmField
    var TURRET_HEIGHT = 0.33

    @JvmField
    var SCORE_ANGLE = toRadians(-20.0)

    @JvmField
    var PULLEY_U = 0.38
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
    TO_BASKET
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
    angleServo.direction = Servo.Direction.REVERSE

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
        it.heading = Angle(clamp(turretHeading, TURRET_CONFIG.MIN_HEADING, TURRET_CONFIG.MAX_HEADING))
    }

    collector.eventBus.subscribe(GetTurretHeadingIsNormalEvent::class){
        it.normal = turretHeading in TURRET_CONFIG.MIN_HEADING..TURRET_CONFIG.MAX_HEADING
    }

    collector.updateEvent += {
        pulleyMotor.setPIDFCoefficients(
            DcMotor.RunMode.RUN_USING_ENCODER,
            TURRET_CONFIG.PULLEY_REGULATOR
        )

        val odometry = collector.eventBus.invoke(GetRobotOdometry())

        val basketErr =
            GameSettings.startOrientation.basketPosition - (odometry.orientation.pos + TURRET_CONFIG.TURRET_CENTER_POS
                .turn(odometry.orientation.angle))

        val y = TURRET_CONFIG.SCORE_HEIGHT - TURRET_CONFIG.TURRET_HEIGHT
        val x = basketErr.length()

        val alpha = atan((2 * y / x) - tan(TURRET_CONFIG.SCORE_ANGLE))

        val v0 =
            sqrt((TURRET_CONFIG.GRAVITY_G * x.pow(2)) / (2 * cos(alpha).pow(2) * (x * tan(alpha) - y)))

        val t = x / (v0 * cos(alpha))

        val robotGlobalVelocity =
            odometry.linearVelocity.turn(odometry.orientation.angle)

        val robotV = robotGlobalVelocity.length()
        val difH = robotGlobalVelocity.rot() - basketErr.rot()

        val vR = -cos(difH) * robotV
        val vT = sin(difH) * robotV

        val vxComp = x / t + vR

        val vXNew = sqrt(vxComp.pow(2) + vT.pow(2))
        val vY = v0 * sin(alpha)

        val newX = vXNew * t

        val angle = clamp(atan(vY / vXNew), TURRET_CONFIG.MIN_ANGLE, TURRET_CONFIG.MAX_ANGLE)

        var pulleyVelocity =
            sqrt(
                (TURRET_CONFIG.GRAVITY_G * newX.pow(2)) / (2.0 * cos(angle)
                    .pow(2)
                        * (newX * tan(angle) - y))
            ) / TURRET_CONFIG.PULLEY_U

        if (pulleyVelocity.isNaN() || pulleyVelocity == Double.POSITIVE_INFINITY || pulleyVelocity == Double.NEGATIVE_INFINITY)
            pulleyVelocity = 17.0

        angleServo.position = clamp(
            (angle - TURRET_CONFIG.MIN_ANGLE) /
                    (TURRET_CONFIG.MAX_ANGLE - TURRET_CONFIG.MIN_ANGLE) *
                    (TURRET_CONFIG.MAX_ANGLE_SERVO - TURRET_CONFIG.MIN_ANGLE_SERVO) +
                    TURRET_CONFIG.MIN_ANGLE_SERVO,
            TURRET_CONFIG.MIN_ANGLE_SERVO,
            TURRET_CONFIG.MAX_ANGLE_SERVO
        )

        pulleyMotor.velocity =
            pulleyVelocity / (2.0 * PI * TURRET_CONFIG.PULLEY_RADIUS) * TURRET_CONFIG.PULLEY_TICKS_REVOLUTION / TURRET_CONFIG.PULLEY_RATION

        turretHeading = (when (state) {
            TurretState.TO_OBELISK -> Angle(
                (GAME_CONFIGS.OBELISK_POSITION - (odometry.orientation.pos +
                        TURRET_CONFIG.TURRET_CENTER_POS.turn(odometry.orientation.angle))).rot()
            )
            TurretState.TO_BASKET -> Angle(basketErr.rot() + PI)
        } - odometry.orientation.angl).angle

        headingServo.position = (clamp(
            turretHeading, TURRET_CONFIG.MIN_HEADING, TURRET_CONFIG.MAX_HEADING
        ) + TURRET_CONFIG.ZERO_HEADING_POS.angle) / TURRET_CONFIG.HEADING_SERVO_RATIO / TURRET_CONFIG.HEADING_SERVO_MAX_ANGLE

        collector.telemetry.addData("target pulleyVelocity", pulleyVelocity)
        collector.telemetry.addData(
            "current pulley velocity",
            pulleyMotor.velocity / TURRET_CONFIG.PULLEY_TICKS_REVOLUTION * 2.0 * PI * TURRET_CONFIG.PULLEY_RADIUS
        )
    }
}