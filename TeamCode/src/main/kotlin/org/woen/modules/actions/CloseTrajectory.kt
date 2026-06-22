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
                linearVelocityConstrain = 1.0
            )
        ),
        ShootAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(-0.342, -0.675 * colorK), Angle.ofDeg(-90.0 * colorK)),
                positionWindow = 0.2
            )
        ),
        StartEatAction(eventBus),
        DriveAction(
            eventBus,
            MoveSegment(
                Vec2(-0.342, -1.35 * colorK),
                velocityConstrain = 0.5,
                positionWindow = 0.2
            ),
            MoveSegment(
                Vec2(-0.342, -1.185 * colorK),
                velocityConstrain = 1.0,
                positionWindow = 0.2
            ),
            MoveSegment(Vec2(-0.1, -1.402 * colorK), velocityConstrain = 1.0)
        ),
        StopEatAction(eventBus),
        SortingAction(eventBus, StockPattern.Storage.PPG),
        WaitAction(2.0),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(shootPosition, Angle.ofDeg(-45.0 * colorK)),
                linearVelocityConstrain = 1.0
            )
        ),
        ShootAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(0.241, -0.641 * colorK), Angle.ofDeg(-90.0 * colorK)),
                linearVelocityConstrain = 1.0,
                positionWindow = 0.2
            )
        ),
        StartEatAction(eventBus),
        DriveAction(
            eventBus,
            MoveSegment(
                Vec2(0.241, -1.185 * colorK),
                velocityConstrain = 0.5,
                positionWindow = 0.2
            ),
            DriveSegment(
                Orientation(shootPosition, Angle.ofDeg(-45.0 * colorK)),
                linearVelocityConstrain = 1.0
            )
        ),
        StopEatAction(eventBus),
        SortingAction(eventBus, StockPattern.Storage.PGP),
        ShootAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(0.884, -0.641 * colorK), Angle.ofDeg(-90.0 * colorK)),
                linearVelocityConstrain = 1.0,
                positionWindow = 0.2
            )
        ),
        StartEatAction(eventBus),
        DriveAction(
            eventBus,
            MoveSegment(
                Vec2(0.884, -1.185 * colorK),
                velocityConstrain = 0.5,
                positionWindow = 0.2
            ),
            DriveSegment(
                Orientation(shootPosition, Angle.ofDeg(-45.0 * colorK)),
                linearVelocityConstrain = 1.0
            )
        ),
        StopEatAction(eventBus),
        SortingAction(eventBus, StockPattern.Storage.GPP),
        ShootAction(eventBus)
    )

    return Array(actions.size) { actions[it] }
}