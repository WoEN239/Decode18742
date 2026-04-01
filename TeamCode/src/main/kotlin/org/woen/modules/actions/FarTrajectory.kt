package org.woen.modules.actions

import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.Vector2d
import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.modules.BallColor
import org.woen.modules.drivetrain.GetTrajectoryBuilderEvent
import kotlin.math.PI

fun farTrajectory(collector: Collector): Array<IAction> {
    val eventBus = collector.eventBus

    val colorK = if (GameSettings.startOrientation.gameColor == GameColor.BLUE) 1.0 else -1.0

    return arrayOf(
        WaitAction(3.0),
        SortingAction(eventBus, BallColor.PURPLE, BallColor.PURPLE, BallColor.GREEN),
        ShootAction(eventBus),
        TrajectoryAction(
            eventBus, eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .setReversed(true)
                .splineToLinearHeading(Pose2d(Vector2d(0.974, -0.657 * colorK), -PI / 2.0 * colorK), -PI / 2.0 * colorK)
                .build()
        ),
        StartEatAction(eventBus),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!.strafeToConstantHeading(
                Vector2d(
                    0.974,
                    -1.35 * colorK
                )
            ).build()
        ),
        WaitAction(0.9),
        ParallelActions(
            arrayOf(
                arrayListOf(
                    TrajectoryAction(
                        eventBus,
                        eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!.setReversed(true)
                            .splineToLinearHeading(
                                Pose2d(
                                    Vector2d(
                                        0.01 + 1.35 + 0.38 / 2.0,
                                        (-0.225 - 0.38 / 2.0 - 0.01) * colorK
                                    ), 0.0
                                ), 0.0
                            ).build()
                    )
                ),
                arrayListOf(
                    SortingAction(
                        eventBus,
                        BallColor.GREEN,
                        BallColor.PURPLE,
                        BallColor.PURPLE
                    )
                )
            ), ParallelActions.ExitType.AND
        ),
        WaitAction(0.3),
        ShootAction(eventBus),
        TrajectoryAction(
            eventBus, eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToConstantHeading(
                    Vector2d(
                        0.01 + 1.35 + 0.38 / 2.0 - 0.3,
                        (-0.225 - 0.38 / 2.0 - 0.01) * colorK
                    )
                ).build()
        )
    )
}