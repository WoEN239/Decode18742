package org.woen.modules.actions

import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.modules.drivetrain.DriveSegment
import org.woen.modules.drivetrain.MoveSegment
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

fun farTrajectory(collector: Collector): Array<IAction> {
    val eventBus = collector.eventBus

    val colorK = if (GameSettings.startOrientation.gameColor == GameColor.BLUE) 1.0 else -1.0

    return arrayOf(
        WaitAction(7.0),
        SlowShootAction(eventBus),
//        DriveAction(
//            eventBus,
//            DriveSegment(
//                Orientation(Vec2(0.874, -0.641 * colorK), Angle.ofDeg(-90.0 * colorK)),
//                linearVelocityConstrain = 2.0,
//                positionWindow = 0.2
//            )
//        ),
//        StartEatAction(eventBus),
//        DriveAction(
//            eventBus,
//            MoveSegment(
//                Vec2(0.874, -1.225 * colorK),
//                velocityConstrain = 0.5,
//                positionWindow = 0.2
//            )
//        ),
//        WaitAction(0.4),
//        StopEatAction(eventBus),
//        DriveAction(eventBus, MoveSegment(Vec2(1.35, -0.325 * colorK))),
//        SlowShootAction(eventBus),
        DriveAction(
            eventBus,
            DriveSegment(
                Orientation(Vec2(1.475, -0.841 * colorK), Angle.ofDeg(0.0 * colorK)),
                linearVelocityConstrain = 2.0,
                positionWindow = 0.2
            )
        )
    )
}