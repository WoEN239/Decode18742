package org.woen.modules.actions

import com.acmerobotics.roadrunner.AngularVelConstraint
import com.acmerobotics.roadrunner.MinVelConstraint
import com.acmerobotics.roadrunner.TranslationalVelConstraint
import com.acmerobotics.roadrunner.Vector2d
import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.modules.drivetrain.GetTrajectoryBuilderEvent
import java.lang.Math.toRadians
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
        WaitAction(3.9),
        ShootAction(eventBus),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToLinearHeading(
                    Vector2d(-1.536, -0.349 * colorK),
                    toRadians(54.211 * colorK)
                )
                .build()
        ),
        WaitAction(6.0),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToLinearHeading(Vector2d(0.699, -0.628 * colorK), toRadians(-45.0 * colorK))
                .build()
        ),
        StartEatAction(eventBus),
        WaitAction(2.5),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .splineTo(
                    Vector2d(1.415, -1.451 * colorK), -PI / 2.0 * colorK, MinVelConstraint(
                        listOf(
                            TranslationalVelConstraint(1.0), AngularVelConstraint(5.0)
                        )
                    )
                )
                .build()
        ),
        WaitAction(1.5),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!.strafeToConstantHeading(
                Vector2d(0.858, -1.558 * colorK)
            ).build()
        ),
        WaitAction(2.0),
        StopEatAction(eventBus),
        TrajectoryAction(eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!.setReversed(true).splineToConstantHeading(Vector2d(0.067, -1.379 * colorK), PI / 2.0).build()),
        WaitAction(0.5),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!.strafeToLinearHeading(
                shootPosition, 0.0
            ).build()
        ),
        WaitAction(0.9),
        ShootAction(eventBus),
        TrajectoryAction(
            eventBus,
            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
                .strafeToLinearHeading(
                    Vector2d(-1.536, -0.349 * colorK),
                    toRadians(54.211 * colorK)
                )
                .build()
        ),
//        TurretStateSwapAction(eventBus, TurretState.TO_OBELISK),
//        WaitAction(1.0),
//        TrajectoryAction(
//            eventBus,
//            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                .strafeToLinearHeading(Vector2d(-0.25, -0.5 * colorK), -PI / 2.0 * colorK).build()
//        ),
//        StartEatAction(eventBus),
//        TrajectoryAction(
//            eventBus,
//            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                .strafeToConstantHeading(Vector2d(-0.25, -1.33 * colorK)).build()
//        ),
//        WaitAction(1.0),
//        StopEatAction(eventBus),
//        TurretStateSwapAction(eventBus, TurretState.TO_BASKET),
//        ParallelActions(
//            arrayOf(
//                arrayListOf(
//                    SortingAction(
//                        eventBus,
//                        BallColor.PURPLE,
//                        BallColor.PURPLE,
//                        BallColor.GREEN
//                    )
//                ),
//                arrayListOf(
//                    TrajectoryAction(
//                        eventBus, eventBus.invoke(
//                            GetTrajectoryBuilderEvent()
//                        ).builder!!
//                            .setReversed(true)
//                            .strafeToConstantHeading(Vector2d(-0.314, -0.8 * colorK))
//                            .setTangent(0.0)
//                            .splineToConstantHeading(
//                                Vector2d(0.0, -1.5 * colorK),
//                                PI / 2.0 * colorK
//                            ).build()
//                    ),
//                    WaitAction(0.85),
//                    TrajectoryAction(
//                        eventBus,
//                        eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                            .strafeToLinearHeading(shootPosition, 0.0).build()
//                    )
//                ),
//            ), ParallelActions.ExitType.AND
//        ),
//        WaitAction(0.15),
//        SlowShootAction(eventBus),
//        TrajectoryAction(
//            eventBus,
//            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                .strafeToLinearHeading(Vector2d(0.3, -0.4 * colorK), -PI / 2.0 * colorK)
//                .build()
//        ),
//        StartEatAction(eventBus),
//        TrajectoryAction(
//            eventBus,
//            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                .strafeToConstantHeading(Vector2d(0.3, -1.65 * colorK))
//                .build()
//        ),
//        WaitAction(0.8),
//        StopEatAction(eventBus),
//        ParallelActions(
//            arrayOf(
//                arrayListOf(
//                    TrajectoryAction(
//                        eventBus,
//                        eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                            .setReversed(true)
//                            .strafeToConstantHeading(Vector2d(0.3, -0.5 * colorK))
//                            .setReversed(false)
//                            .strafeToLinearHeading(shootPosition, 0.0)
//                            .build()
//                    )
//                ),
//                arrayListOf(
//                    SortingAction(
//                        eventBus,
//                        BallColor.PURPLE,
//                        BallColor.GREEN,
//                        BallColor.PURPLE
//                    )
//                )
//            ), ParallelActions.ExitType.AND
//        ),
//        WaitAction(0.15),
//        SlowShootAction(eventBus),
//        TrajectoryAction(
//            eventBus,
//            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                .strafeToLinearHeading(Vector2d(-0.2, -0.5 * colorK), -PI / 2.0 * colorK)
//                .strafeToConstantHeading(Vector2d(0.85, -0.5 * colorK))
//                .build()
//        ),
//        StartEatAction(eventBus),
//        TrajectoryAction(
//            eventBus,
//            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                .strafeToConstantHeading(Vector2d(0.85, -1.65 * colorK))
//                .build()
//        ),
//        WaitAction(1.0),
//        StopEatAction(eventBus),
//        ParallelActions(
//            arrayOf(
//                arrayListOf(
//                    TrajectoryAction(
//                        eventBus,
//                        eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                            .strafeToLinearHeading(shootPosition, 0.0)
//                            .build()
//                    )
//                ),
//                arrayListOf(
//                    SortingAction(
//                        eventBus,
//                        BallColor.GREEN,
//                        BallColor.PURPLE,
//                        BallColor.PURPLE
//                    )
//                )
//            ), ParallelActions.ExitType.AND
//        ),
//        WaitAction(0.15),
//        SlowShootAction(eventBus),
//        TrajectoryAction(
//            eventBus,
//            eventBus.invoke(GetTrajectoryBuilderEvent()).builder!!
//                .strafeToConstantHeading(Vector2d(shootPosition.x - 0.4, shootPosition.y))
//                .build()
//        )
    )
}