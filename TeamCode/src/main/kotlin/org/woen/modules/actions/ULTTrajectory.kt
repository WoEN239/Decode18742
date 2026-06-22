package org.woen.modules.actions

import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.modules.drivetrain.DriveSegment
import org.woen.modules.drivetrain.MoveSegment
import org.woen.modules.drivetrain.TurnSegment
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import java.lang.Math.toRadians

fun ultTrajectory(collector: Collector): Array<IAction> {
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
            )
        ),
        StopEatAction(eventBus),
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
        ShootAction(eventBus),
        ParallelActions(
            arrayOf(
                arrayListOf(
                    DriveAction(
                        eventBus,
                        MoveSegment(
                            Vec2(0.341, -1.093 * colorK),
                            velocityConstrain = 1.0,
                            positionWindow = 0.2
                        )
                    ),
                    StartEatAction(eventBus),
                    DriveAction(
                        eventBus,
                        MoveSegment(Vec2(0.341, -1.493 * colorK), velocityConstrain = 1.0)
                    )
                ),
                arrayListOf(
                    DriveAction(eventBus, TurnSegment(Angle.ofDeg(-118.099 * colorK)))
                )
            )
        ),
        WaitAction(4.0),
        DriveAction(
            eventBus,
            DriveSegment(Orientation(Vec2(0.256, -1.354 * colorK), Angle.ofDeg(-90.0 * colorK)))
        ),
        WaitAction(0.5),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(shootPosition, Angle.ofDeg(-45.0 * colorK)),
                linearVelocityConstrain = 1.0
            )
        ),
        ShootAction(eventBus)
    )

    return Array(actions.size) { actions[it] }
}