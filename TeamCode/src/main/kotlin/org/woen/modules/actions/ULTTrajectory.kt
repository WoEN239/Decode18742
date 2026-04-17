package org.woen.modules.actions

import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.modules.drivetrain.DriveSegment
import org.woen.modules.drivetrain.MoveSegment
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import java.lang.Math.toRadians

fun ultTrajectory(collector: Collector): Array<IAction> {
    val eventBus = collector.eventBus

    val colorK = if (GameSettings.startOrientation.gameColor == GameColor.BLUE) 1.0 else -1.0

    val actions = arrayListOf<IAction>()

    val shootPosition = Vec2(-0.476, -0.725 * colorK)

    actions.addAll(
        arrayOf(
            WaitAction(1.0),
            DriveAction(eventBus, MoveSegment(shootPosition)),
            ShootAction(eventBus),
            StartEatAction(eventBus),
            DriveAction(
                eventBus,
                DriveSegment(
                    Orientation(Vec2(-0.265, -0.751 * colorK), Angle.ofDeg(-90.0 * colorK)),
                    positionWindow = 0.4
                ),
                MoveSegment(Vec2(-0.289, -1.393 * colorK), 1.2)
            ),
            WaitAction(0.2),
            DriveAction(
                eventBus,
                DriveSegment(
                    Orientation(Vec2(-0.1, -1.0 * colorK), Angle.ofDeg(-90.0 * colorK)),
                    positionWindow = 0.4,
                    headingWindow = toRadians(40.0)
                )
            ),
            DriveAction(
                eventBus,
                DriveSegment(Orientation(Vec2(0.0, -1.45 * colorK), Angle.ofDeg(-90.0 * colorK)))
            ),
            StopEatAction(eventBus),
            WaitAction(1.5),
            DriveAction(eventBus, DriveSegment(Orientation(shootPosition, Angle.ZERO))),
            ShootAction(eventBus),
            DriveAction(
                eventBus,
                DriveSegment(
                    Orientation(Vec2(0.386, -0.582 * colorK), Angle.ofDeg(-90.0 * colorK)),
                    positionWindow = 0.4,
                    headingWindow = toRadians(40.0)
                )
            ),
            StartEatAction(eventBus),
            DriveAction(eventBus, MoveSegment(Vec2(0.386, -1.5 * colorK), 1.2)),
            WaitAction(0.3),
            StopEatAction(eventBus),
            DriveAction(
                eventBus,
                DriveSegment(
                    Orientation(Vec2(0.306, -1.0 * colorK), Angle.ofDeg(-90.0 * colorK)),
                    positionWindow = 0.4,
                    headingWindow = toRadians(40.0)
                )
            ),
            DriveAction(
                eventBus,
                DriveSegment(Orientation(Vec2(0.0, -1.45 * colorK), Angle.ofDeg(-90.0 * colorK)))
            ),
            WaitAction(1.5),
            DriveAction(eventBus, DriveSegment(Orientation(shootPosition, Angle.ZERO))),
            ShootAction(eventBus),
            DriveAction(
                    eventBus,
                    DriveSegment(Orientation(Vec2(-0.1, -1.2 * colorK), Angle.ofDeg(-90.0 * colorK)), positionWindow = 0.4),
                MoveSegment(Vec2(-0.1, -1.45 * colorK))
                ),
            WaitAction(2.5),
            DriveAction(
                eventBus,
                MoveSegment(Vec2(-0.1, -1.2 * colorK))
            ),
            StartEatAction(eventBus),
            DriveAction(eventBus, DriveSegment(Orientation(Vec2(1.0, -1.578 * colorK), Angle.ofDeg(-34.767 * colorK)), linearVelocityConstrain = 1.2)),
            WaitAction(0.5),
            StopEatAction(eventBus),
            DriveAction(eventBus, DriveSegment(Orientation(shootPosition, Angle.ZERO))),
            ShootAction(eventBus),
            DriveAction(eventBus, DriveSegment(Orientation(Vec2(0.0, shootPosition.y), Angle.ZERO))),
        )
    )
//
//    repeat(3) {
//        actions.addAll(
//            arrayOf(
//                StartEatAction(eventBus),
//                DriveAction(
//                    eventBus,
//                    DriveSegment(
//                        Orientation(Vec2(0.264, -1.137 * colorK), Angle.ofDeg(-129.679 * colorK)),
//                        positionWindow = 0.4
//                    ),
//                    MoveSegment(Vec2(0.264, -1.597 * colorK))
//                ),
//                WaitAction(2.0),
//                DriveAction(
//                    eventBus,
//                    DriveSegment(Orientation(Vec2(0.0, -1.45 * colorK), Angle.ofDeg(-90.0 * colorK)))
//                ),
//                WaitAction(1.0),
//                StopEatAction(eventBus),
//                DriveAction(eventBus, DriveSegment(Orientation(shootPosition, Angle.ZERO))),
//                ShootAction(eventBus),
//            )
//        )
//    }

    return Array(actions.size) { actions[it] }
}