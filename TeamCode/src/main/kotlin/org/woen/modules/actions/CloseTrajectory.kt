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

fun closeTrajectory(collector: Collector): Array<IAction> {
    val eventBus = collector.eventBus

    val colorK = if (GameSettings.startOrientation.gameColor == GameColor.BLUE) 1.0 else -1.0

    val shootPosition = Vec2(-1.032, -0.830 * colorK)

    val actions = arrayListOf(
        DriveAction(eventBus, MoveSegment(shootPosition)),
        WaitAction(0.5),
        ShootAction(eventBus),
        ParallelActions(
            arrayOf(
                arrayListOf(
                    DriveAction(eventBus, TurnSegment(Angle.ofDeg(-90.0 * colorK)))
                ),
                arrayListOf(
                    DriveAction(
                        eventBus,
                        MoveSegment(Vec2(0.306, -0.409 * colorK), isFlyingPoint = true),
                        MoveSegment(Vec2(0.247, -0.615 * colorK), isFlyingPoint = true)
                    )
                )
            ), ParallelActions.ExitType.AND
        ),
        StartEatAction(eventBus),
        DriveAction(eventBus, MoveSegment(Vec2(0.306, -1.5 * colorK), 1.6)),
        WaitAction(0.15),
        StopEatAction(eventBus),
        DriveAction(
            eventBus,
            MoveSegment(Vec2(0.283, -1.228 * colorK), isFlyingPoint = true),
            MoveSegment(Vec2(0.101, -1.226 * colorK), isFlyingPoint = true),
            MoveSegment(Vec2(0.096, -1.440 * colorK))
        ),
        WaitAction(0.5),
        DriveAction(eventBus, DriveSegment(Orientation(Vec2(0.046, -0.805 * colorK), Angle.ofDeg(-45.0 * colorK))),
            DriveSegment(Orientation(shootPosition, Angle.ZERO))),
        ShootAction(eventBus)
    )

    repeat(3) {
        actions.addAll(
            arrayListOf(
                DriveAction(eventBus, MoveSegment(Vec2(-0.036, -0.697 * colorK), isFlyingPoint = true)),
                StartEatAction(eventBus),
                DriveAction(eventBus, DriveSegment(Orientation(Vec2(0.301, -1.515 * colorK), Angle.ofDeg(-126.914 * colorK)))),
                WaitAction(2.0),
                StopEatAction(eventBus),
                DriveAction(eventBus, DriveSegment(Orientation(Vec2(0.046, -0.805 * colorK), Angle.ofDeg(-45.0 * colorK))),
                    DriveSegment(Orientation(shootPosition, Angle.ZERO))),
                ShootAction(eventBus)
            )
        )
    }

    return Array(actions.size){actions[it]}
}