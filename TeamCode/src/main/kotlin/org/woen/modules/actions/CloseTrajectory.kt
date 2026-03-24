package org.woen.modules.actions

import com.acmerobotics.roadrunner.Vector2d
import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.modules.BallColor
import org.woen.modules.TurretState
import org.woen.modules.drivetrain.GetTrajectoryBuilderEvent
import kotlin.math.PI

fun closeTrajectory(collector: Collector): Array<IAction> {
    val eventBus = collector.eventBus

    val colorK = if (GameSettings.startOrientation.gameColor == GameColor.BLUE) 1.0 else -1.0

    val shootPosition = Vector2d(-1.032, -0.830 * colorK)

    return arrayOf(
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!.strafeToConstantHeading(
                shootPosition
            ).build()
        ),
        WaitAction(0.5),
        ShootAction(eventBus),
        TurretStateSwapAction(eventBus, TurretState.TO_OBELISK),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToLinearHeading(Vector2d(-0.2, -0.5 * colorK), -PI / 2.0 * colorK).build()
        ),
        StartEatAction(eventBus),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToConstantHeading(Vector2d(-0.2, -1.33 * colorK)).build()
        ),
        WaitAction(1.2),
        StopEatAction(eventBus),
        TurretStateSwapAction(eventBus, TurretState.TO_BASKET),
        ParallelActions(
            arrayOf(
                arrayListOf(
                    SortingAction(
                        eventBus,
                        BallColor.PURPLE,
                        BallColor.PURPLE,
                        BallColor.GREEN
                    )
                ),
                arrayListOf(
                    TrajectoryAction(
                        eventBus, eventBus.invoke(
                            GetTrajectoryBuilderEvent()
                        ).builder!!
                            .setReversed(true)
                            .strafeToConstantHeading(Vector2d(-0.314, -0.8 * colorK))
                            .setTangent(0.0)
                            .splineToConstantHeading(
                                Vector2d(0.0, -1.5 * colorK),
                                PI / 2.0 * colorK
                            ).build()
                    ),
                    WaitAction(0.85),
                    TrajectoryAction(
                        eventBus,
                        eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                            .strafeToLinearHeading(shootPosition, 0.0).build()
                    )
                ),
            ), ParallelActions.ExitType.AND
        ),
        WaitAction(0.15),
        SlowShootAction(eventBus),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToLinearHeading(Vector2d(0.35, -0.6 * colorK), -PI / 2.0)
                .build()
        ),
        StartEatAction(eventBus),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToConstantHeading(Vector2d(0.35, -1.6 * colorK))
                .build()
        ),
        WaitAction(0.8),
        StopEatAction(eventBus),
        ParallelActions(
            arrayOf(
                arrayListOf(
                    TrajectoryAction(
                        eventBus,
                        eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                            .setReversed(true)
                            .strafeToConstantHeading(Vector2d(0.3, -0.5 * colorK))
                            .setReversed(false)
                            .strafeToLinearHeading(shootPosition, 0.0)
                            .build()
                    )
                ),
                arrayListOf(
                    SortingAction(
                        eventBus,
                        BallColor.PURPLE,
                        BallColor.GREEN,
                        BallColor.PURPLE
                    )
                )
            ), ParallelActions.ExitType.AND
        ),
        WaitAction(0.15),
        SlowShootAction(eventBus),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToLinearHeading(Vector2d(-0.2, -0.65 * colorK), -PI / 2.0 * colorK)
                .strafeToConstantHeading(Vector2d(0.9, -0.65 * colorK))
                .build()
        ),
        StartEatAction(eventBus),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToConstantHeading(Vector2d(0.9, -1.6 * colorK))
                .build()
        ),
        WaitAction(1.0),
        StopEatAction(eventBus),
        ParallelActions(
            arrayOf(
                arrayListOf(
                    TrajectoryAction(
                        eventBus,
                        eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                            .strafeToLinearHeading(shootPosition, 0.0)
                            .build()
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
        WaitAction(0.15),
        SlowShootAction(eventBus),
        SlowShootAction(eventBus),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToConstantHeading(Vector2d(shootPosition.x - 0.4, shootPosition.y))
                .build()
        )
    )
}