package org.woen.modules.actions

import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.enumerators.StockPattern
import org.woen.modules.BallColor
import org.woen.modules.drivetrain.DriveSegment
import org.woen.modules.drivetrain.MoveSegment
import org.woen.modules.drivetrain.TurnSegment
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import java.lang.Math.toRadians

fun closeTrajectory(collector: Collector): Array<IAction> {
    val eventBus = collector.eventBus

    val colorK = if (GameSettings.startOrientation.gameColor == GameColor.BLUE) 1.0 else -1.0

    val shootPosition = Vec2(-0.516, -0.649 * colorK)

    val actions = arrayListOf(
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(shootPosition, Angle.ofDeg(45.0 * colorK)),
                linearVelocityConstrain = 1.0,
                positionWindow = 0.1
            )
        ),
        ShootAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(Orientation(Vec2(-0.342, -0.675 * colorK), Angle.ofDeg(-90.0 * colorK)))
        ),
        StartEatAction(eventBus),
        DriveAction(eventBus, MoveSegment(Vec2(-0.342, -1.4 * colorK), velocityConstrain = 0.5)),
        StopEatAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(shootPosition, Angle.ofDeg(45.0 * colorK)),
                linearVelocityConstrain = 1.0,
                positionWindow = 0.1
            )
        )
    )

    return Array(actions.size) { actions[it] }
}