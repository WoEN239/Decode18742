package org.woen.modules.runner.segment

import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.Pose2dDual
import com.acmerobotics.roadrunner.Time
import com.acmerobotics.roadrunner.TimeTrajectory
import com.acmerobotics.roadrunner.TimeTurn
import com.acmerobotics.roadrunner.Trajectory
import com.acmerobotics.roadrunner.TurnConstraints
import org.woen.telemetry.configs.Configs
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

class TurnSegment(angle: Double, private val _startOrientation: Orientation) : ISegment {
    private val _turn = TimeTurn(
        Pose2d(_startOrientation.x, _startOrientation.y, _startOrientation.angl.angle), angle,
        TurnConstraints(
            Configs.ROAD_RUNNER.ROAD_RUNNER_ROTATE_VELOCITY,
            -Configs.ROAD_RUNNER.ROAD_RUNNER_ROTATE_VELOCITY,
            Configs.ROAD_RUNNER.ROAD_RUNNER_ROTATE_ACCEL
        )
    )

    override fun isEnd(time: Double) = time > duration()

    override fun translateVelocity(time: Double) = Vec2.ZERO

    override fun rotateVelocity(time: Double) = _turn[time].velocity().angVel.value()

    override fun targetOrientation(time: Double) =
        Orientation(_startOrientation.pos, Angle(_turn[time].value().heading.toDouble()))

    override fun duration() = _turn.duration
}

class RRTrajectorySegment(rawBuiltTrajectory: List<Trajectory>) : ISegment {
    private val _trajectory =
        Array(rawBuiltTrajectory.size) { TimeTrajectory(rawBuiltTrajectory[it]) }

    private fun getPoseTime(time: Double): Pose2dDual<Time> {
        var sumDuration = 0.0

        for (i in _trajectory) {
            if (i.duration + sumDuration > time)
                return i[time - sumDuration]

            sumDuration += i.duration
        }

        return _trajectory.last()[time]
    }

    override fun isEnd(time: Double) = duration() < time

    override fun translateVelocity(time: Double) =
        Vec2(getPoseTime(time).velocity().linearVel.value())

    override fun rotateVelocity(time: Double) = getPoseTime(time).velocity().angVel.value()

    override fun targetOrientation(time: Double) = Orientation(
        Vec2(getPoseTime(time).position.value()),
        Angle(getPoseTime(time).heading.value().toDouble())
    )

    override fun duration() = _trajectory.sumOf { it.duration }
}