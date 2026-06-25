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

    val shootPosition = Vec2(-0.506, -0.659 * colorK)

    val actions = arrayListOf(
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(shootPosition, Angle.ofDeg(45.0 * colorK)),
                linearVelocityConstrain = 2.0
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
                Vec2(-0.342, -1.4 * colorK),
                velocityConstrain = 0.5,
                positionWindow = 0.2
            )
        ),
        WaitAction(0.2),
        StopEatAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(shootPosition, Angle.ofDeg(-45.0 * colorK)),
                linearVelocityConstrain = 2.2,
                positionWindow = 0.3
            )
        ),
        ShootAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(0.271, -0.641 * colorK), Angle.ofDeg(-90.0 * colorK)),
                linearVelocityConstrain = 2.2,
                positionWindow = 0.2
            )
        ),
        StartEatAction(eventBus),
        DriveAction(
            eventBus,
            MoveSegment(
                Vec2(0.271, -1.285 * colorK),
                velocityConstrain = 0.5,
                positionWindow = 0.2
            )
        ),
        WaitAction(0.2),
        StopEatAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(0.236, -1.354 * colorK), Angle.ofDeg(-90.0 * colorK)),
                linearVelocityConstrain = 2.2
            )
        ),
        WaitAction(0.2),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(shootPosition, Angle.ofDeg(-45.0 * colorK)),
                linearVelocityConstrain = 2.2,
                positionWindow = 0.3
            )
        ),
        ShootAction(eventBus),
    )

    repeat(3) {
        actions.addAll(
            arrayOf(
                ParallelActions(
                    arrayOf(
                        arrayListOf(
                            DriveAction(
                                eventBus,
                                MoveSegment(
                                    Vec2(0.341, -1.093 * colorK),
//                                    velocityConstrain = 2.2,
                                    positionWindow = 0.2
                                )
                            ),
                            StartEatAction(eventBus),
                            DriveAction(
                                eventBus,
                                MoveSegment(Vec2(0.331, -1.493 * colorK), velocityConstrain = 2.2)
                            )
                        ),
                        arrayListOf(
                            DriveAction(eventBus, TurnSegment(Angle.ofDeg(-118.099 * colorK)))
                        )
                    )
                ),
                WaitAction(2.05),
                DriveAction(
                    eventBus,
                    DriveSegment(
                        Orientation(Vec2(0.216, -1.354 * colorK), Angle.ofDeg(-90.0 * colorK)),
                        linearVelocityConstrain = 2.2
                    )
                ),
                DriveAction(
                    eventBus,
                    DriveSegment(
                        Orientation(shootPosition, Angle.ofDeg(-45.0 * colorK)),
                        linearVelocityConstrain = 2.2,
                        positionWindow = 0.3
                    )
                ),
                ShootAction(eventBus)
            )
        )
    }

    actions.add(
        DriveAction(
            eventBus,
            MoveSegment(
                Vec2(shootPosition.x + 0.5, shootPosition.y),
                positionWindow = 0.2
            )
        ),
    )

    return Array(actions.size) { actions[it] }
}