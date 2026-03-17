package org.woen.modules.drivetrain

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.collector.RunMode
import org.woen.utils.units.Angle
import org.woen.utils.units.Color
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

@Config
internal object ODOMETRY_CONFIG {
    @JvmField
    var X_ODOMETER_POSITION = -0.0995

    @JvmField
    var Y_ODOMETER_POSITION = -0.0895

    @JvmField
    var ROBOT_SIZE = Vec2(0.38, 0.38)
}

class GetRobotOdometry(
    var orientation: Orientation = Orientation.ZERO,
    var linearVelocity: Vec2 = Vec2.ZERO,
    var headingVelocity: Double = 0.0
)

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

    var orientation = Orientation()

    var linearVelocity = Vec2.ZERO
    var headingVelocity = 0.0

    collector.eventBus.subscribe(GetRobotOdometry::class) {
        it.orientation = orientation
        it.linearVelocity = linearVelocity
        it.headingVelocity = headingVelocity
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
    }
}