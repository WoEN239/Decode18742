package org.woen.modules.drivetrain

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.collector.RunMode
import org.woen.modules.AddGamepad1ListenerEvent
import org.woen.modules.ClickGamepadListener
import org.woen.modules.OnCameraUpdateEvent
import org.woen.modules.SIMPLE_STORAGE_CONFIG
import org.woen.modules.TURRET_CONFIG
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.units.Angle
import org.woen.utils.units.Color
import org.woen.utils.units.Line
import org.woen.utils.units.Orientation
import org.woen.utils.units.Triangle
import org.woen.utils.units.Vec2
import kotlin.math.PI

@Config
internal object ODOMETRY_CONFIG {
    @JvmField
    var X_ODOMETER_POSITION = -0.0995

    @JvmField
    var Y_ODOMETER_POSITION = -0.0895

    @JvmField
    var ROBOT_SIZE = Vec2(0.38, 0.38)

    @JvmField
    var SHOOT_SHORT_TRIANGLE = Triangle(Vec2(-1.83, 1.83), Vec2(0.0, 0.0), Vec2(-1.83, -1.83))

    @JvmField
    var SHOOT_LONG_TRIANGLE = Triangle(Vec2(1.83, 0.61), Vec2(1.22, 0.0), Vec2(1.83, -0.61))

    @JvmField
    var CALIBRATE_ORIENTATION = Orientation(Vec2(-0.794, -0.791), Angle.ofDeg(90.0))
}

class GetRobotOdometry(
    var orientation: Orientation = Orientation.ZERO,
    var linearVelocity: Vec2 = Vec2.ZERO,
    var headingVelocity: Double = 0.0,
    var locateInShootingArea: Boolean = false,
    var locateInLongShootingArea: Boolean = false
)

class RobotEnterShootingAreaEvent()
class RobotExitShootingAreaEvent()

fun attachOdometry(collector: Collector) {
    val pinpoint = collector.hardwareMap.get("odometry") as GoBildaPinpointDriver

    pinpoint.setOffsets(
        ODOMETRY_CONFIG.X_ODOMETER_POSITION, ODOMETRY_CONFIG.Y_ODOMETER_POSITION,
        DistanceUnit.METER
    )

    pinpoint.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
    pinpoint.setEncoderDirections(
        GoBildaPinpointDriver.EncoderDirection.FORWARD,
        GoBildaPinpointDriver.EncoderDirection.FORWARD
    )

    if (collector.runMode == RunMode.AUTO) {
        pinpoint.recalibrateIMU()
        pinpoint.resetPosAndIMU()
    }

    var orientation = GameSettings.startOrientation.startOrientation

    var linearVelocity = Vec2.ZERO
    var headingVelocity = 0.0

    var locateInShootingArea = false
    var oldLocateInShootingArea = false
    var longLocate = false

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.dpad_down }, {
        val orient =
            ODOMETRY_CONFIG.CALIBRATE_ORIENTATION.pos.turn(-GameSettings.startOrientation.startOrientation.angle) - GameSettings.startOrientation.startOrientation.pos

        pinpoint.position =
            Pose2D(
                DistanceUnit.METER,
                orient.x,
                orient.y,
                AngleUnit.RADIANS,
                (ODOMETRY_CONFIG.CALIBRATE_ORIENTATION.angl - GameSettings.startOrientation.startOrientation.angl).angle
            )
    })))

    collector.eventBus.subscribe(GetRobotOdometry::class) {
        it.orientation = orientation
        it.linearVelocity = linearVelocity
        it.headingVelocity = headingVelocity
        it.locateInShootingArea = locateInShootingArea
        it.locateInLongShootingArea = longLocate
    }

    collector.updateEvent += {
        pinpoint.update()

        val pinpointOrientation = pinpoint.position

        orientation = Orientation(
            Vec2(
                pinpointOrientation.getX(DistanceUnit.METER),
                pinpointOrientation.getY(DistanceUnit.METER)
            ).turn(GameSettings.startOrientation.startOrientation.angle),
            Angle(pinpointOrientation.getHeading(AngleUnit.RADIANS))
        ) + GameSettings.startOrientation.startOrientation

        linearVelocity = Vec2(
            pinpoint.getVelX(DistanceUnit.METER),
            pinpoint.getVelY(DistanceUnit.METER)
        ).turn((Angle(GameSettings.startOrientation.startOrientation.angle) - orientation.angl).angle)
        headingVelocity = pinpoint.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS)

        collector.telemetry.drawRect(
            orientation.pos, ODOMETRY_CONFIG.ROBOT_SIZE, orientation.angle,
            if (GameSettings.startOrientation.gameColor == GameColor.BLUE) Color.BLUE else Color.RED
        )

        collector.telemetry.addData("orientation", orientation)
        collector.telemetry.addData("x vel", linearVelocity.x)
        collector.telemetry.addData("y vel", linearVelocity.y)
        collector.telemetry.addData("h vel", headingVelocity)

        fun checkToLocate(triangle: Triangle): Boolean {
            val halfSize = ODOMETRY_CONFIG.ROBOT_SIZE / 2.0

            val cornerLeftForward =
                orientation.pos + Vec2(-halfSize.x, halfSize.y)
                    .turn(orientation.angle)
            val cornerRightForward =
                orientation.pos + Vec2(halfSize.x, halfSize.y)
                    .turn(orientation.angle)
            val cornerRightBack = orientation.pos + Vec2(halfSize.x, -halfSize.y)
                .turn(orientation.angle)
            val cornerLeftBack = orientation.pos + Vec2(-halfSize.x, -halfSize.y)
                .turn(orientation.angle)

            val robotPoints = arrayOf(
                cornerLeftBack, cornerRightBack,
                cornerRightForward, cornerLeftForward
            )

            val robotLines = arrayOf(
                Line(cornerLeftForward, cornerRightForward),
                Line(cornerRightBack, cornerRightForward),
                Line(cornerRightBack, cornerLeftBack),
                Line(cornerLeftForward, cornerLeftBack)
            )

            for (shootLine in triangle.lines) {
                for (l in robotLines) {
                    if (!l.isIntersects(shootLine))
                        continue

                    val intersects = l.getIntersects(shootLine)

                    if (l.isPointOnLine(intersects) && shootLine.isPointOnLine(intersects))
                        return true
                }

                for (robotPoint in robotPoints)
                    if (triangle.isPointLocated(robotPoint))
                        return true
            }

            return false
        }

        val shortLocate = checkToLocate(ODOMETRY_CONFIG.SHOOT_SHORT_TRIANGLE)
        longLocate = checkToLocate(ODOMETRY_CONFIG.SHOOT_LONG_TRIANGLE)

        locateInShootingArea = shortLocate || longLocate

        if (locateInShootingArea != oldLocateInShootingArea)
            collector.eventBus.invoke(if (locateInShootingArea) RobotEnterShootingAreaEvent() else RobotExitShootingAreaEvent())

        oldLocateInShootingArea = locateInShootingArea
    }
}