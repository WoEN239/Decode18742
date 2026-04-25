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

    val shootPosition = Vec2(-0.476, -0.725 * colorK)

    val actions = arrayListOf(
        DriveAction(eventBus, MoveSegment(shootPosition)),
        ShootAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(0.265, -0.582 * colorK), Angle.ofDeg(-90.0 * colorK)),
                positionWindow = 0.4,
                headingWindow = toRadians(40.0)
            )
        ),
        StartEatAction(eventBus),
        DriveAction(eventBus, MoveSegment(Vec2(0.436, -1.55 * colorK), 1.2)),
        WaitAction(0.2),
        StopEatAction(eventBus),
        DriveAction(eventBus, MoveSegment(Vec2(0.436, -1.283 * colorK), positionWindow = 0.4)),
        ParallelActions(
            arrayOf(
                arrayListOf(
                    DriveAction(eventBus, TurnSegment(Angle.ZERO))
                ),
                arrayListOf(
                    DriveAction(
                        eventBus, MoveSegment(Vec2(0.206, -0.55 * colorK), positionWindow = 0.4),
                        MoveSegment(shootPosition)
                    )
                )
            ), ParallelActions.ExitType.AND
        ),
        ShootAction(eventBus),
//    )

//    repeat(3) {
//    actions.addAll(
//        arrayOf(
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(0.306, -0.582 * colorK), Angle.ofDeg(-90.0 * colorK)),
                positionWindow = 0.4,
                headingWindow = toRadians(40.0)
            ),
        ),
        StartEatAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(
                    Vec2(0.264, -1.557 * colorK),
                    Angle.ofDeg(-129.679 * colorK)
                ),
                positionWindow = 0.4
            )
        ),
        WaitAction(1.9),
        DriveAction(
            eventBus,
            DriveSegment(Orientation(Vec2(0.0, -1.393 * colorK), Angle.ofDeg(-90.0 * colorK)))
        ),
        WaitAction(0.1),
        StopEatAction(eventBus),
        SortingAction(eventBus, StockPattern.Storage.GPP),
        DriveAction(eventBus, MoveSegment(Vec2(0.252, -1.233 * colorK), positionWindow = 0.4)),
        ParallelActions(
            arrayOf(
                arrayListOf(
                    DriveAction(eventBus, TurnSegment(Angle.ZERO))
                ),
                arrayListOf(
                    DriveAction(
                        eventBus,
                        MoveSegment(Vec2(0.206, -0.55 * colorK), positionWindow = 0.4),
                        MoveSegment(shootPosition)
                    )
                )
            ), ParallelActions.ExitType.AND
        ),
        WaitForSortingEndAction(eventBus),
        ShootAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(0.914, -0.726 * colorK), Angle.ofDeg(-90.0 * colorK)),
                positionWindow = 0.4
            )
        ),
        StartEatAction(eventBus),
        DriveAction(eventBus, MoveSegment(Vec2(0.914, -1.5 * colorK), 1.2)),
        WaitAction(0.2),
        StopEatAction(eventBus),
        SortingAction(eventBus, StockPattern.Storage.GPP),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(0.202, -0.705 * colorK), Angle.ZERO),
                positionWindow = 0.4
            )
        ),
        DriveAction(eventBus, MoveSegment(shootPosition)),
        WaitForSortingEndAction(eventBus),
        ShootAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(-0.265, -0.751 * colorK), Angle.ofDeg(-90.0 * colorK)),
                positionWindow = 0.4
            )
        ),
        StartEatAction(eventBus),
        DriveAction(
            eventBus,
            MoveSegment(Vec2(-0.289, -1.393 * colorK), 1.2)
        ),
        WaitAction(0.2),
        StopEatAction(eventBus),
        SortingAction(eventBus, StockPattern.Storage.PPG),
        DriveAction(eventBus, DriveSegment(Orientation(shootPosition, Angle.ZERO))),
        WaitForSortingEndAction(eventBus),
        ShootAction(eventBus),
        DriveAction(eventBus, MoveSegment(Vec2(shootPosition.x + 0.5, shootPosition.y)))
//        )
    )
//    }

    return Array(actions.size) { actions[it] }
}