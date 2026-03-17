package org.woen.modules.drivetrain

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.AngularVelConstraint
import com.acmerobotics.roadrunner.MinVelConstraint
import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.Pose2dDual
import com.acmerobotics.roadrunner.ProfileAccelConstraint
import com.acmerobotics.roadrunner.ProfileParams
import com.acmerobotics.roadrunner.Time
import com.acmerobotics.roadrunner.TimeTrajectory
import com.acmerobotics.roadrunner.TimeTurn
import com.acmerobotics.roadrunner.Trajectory
import com.acmerobotics.roadrunner.TrajectoryBuilder
import com.acmerobotics.roadrunner.TrajectoryBuilderParams
import com.acmerobotics.roadrunner.TranslationalVelConstraint
import com.acmerobotics.roadrunner.TurnConstraints
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.Collector
import org.woen.collector.GameSettings
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

@Config
internal object RUNNER_CONFIG {
    @JvmField
    var LINER_VELOCITY = 2.0

    @JvmField
    var HEADING_VELOCITY = 2.0

    @JvmField
    var LINEAR_ACCEL = 1.0

    @JvmField
    var HEADING_ACCEL = 1.0

    @JvmField
    var POSITION_P_X = 0.0

    @JvmField
    var POSITION_P_Y = 0.0

    @JvmField
    var HEADING_P_H = 0.0
}

interface ITrajectorySegment {
    fun isEnd(time: Double): Boolean

    fun linearVelocity(time: Double): Vec2
    fun headingVelocity(time: Double): Double

    fun targetPosition(time: Double): Vec2?
    fun targetHeading(time: Double): Angle

    fun duration(): Double
}

class TurnSegment(angle: Double, private val _startAngle: Angle) : ITrajectorySegment {
    private val _turn = TimeTurn(
        Pose2d(0.0, 0.0, _startAngle.angle), angle,
        TurnConstraints(
            RUNNER_CONFIG.HEADING_VELOCITY,
            -RUNNER_CONFIG.HEADING_VELOCITY,
            RUNNER_CONFIG.HEADING_ACCEL
        )
    )

    override fun isEnd(time: Double) = time > duration()

    override fun linearVelocity(time: Double) = Vec2.ZERO

    override fun headingVelocity(time: Double) = _turn[time].velocity().angVel.value()

    override fun targetPosition(time: Double) = null

    override fun targetHeading(time: Double) = Angle(_turn[time].value().heading.toDouble())

    override fun duration() = _turn.duration
}

class DriveSegment(rawBuildedTrajectory: List<Trajectory>) : ITrajectorySegment {
    private val _trajectory =
        Array(rawBuildedTrajectory.size) { TimeTrajectory(rawBuildedTrajectory[it]) }

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

    override fun linearVelocity(time: Double) = Vec2(getPoseTime(time).velocity().linearVel.value())

    override fun headingVelocity(time: Double) = getPoseTime(time).velocity().angVel.value()

    override fun targetPosition(time: Double) = Vec2(getPoseTime(time).position.value())

    override fun targetHeading(time: Double) = Angle(getPoseTime(time).heading.value().toDouble())

    override fun duration() = _trajectory.sumOf { it.duration }
}

class GetTrajectoryBuilderEvent(
    var startOrientation: Orientation? = null,
    var builder: TrajectoryBuilder? = null
)

class RunSegmentsEvent(val segments: Array<ITrajectorySegment>)

fun attachRunner(collector: Collector) {
    val segmentsQueue = ArrayDeque<ITrajectorySegment>()
    var targetOrientation = GameSettings.startOrientation.startOrientation
    val segmentTimer = ElapsedTime()

    collector.eventBus.subscribe(RunSegmentsEvent::class) {
        if (segmentsQueue.isEmpty())
            segmentTimer.reset()

        segmentsQueue.addAll(it.segments)
    }

    collector.eventBus.subscribe(GetTrajectoryBuilderEvent::class) {
        val startOrientation =
            if (it.startOrientation == null) {
                if (segmentsQueue.isEmpty())
                    targetOrientation
                else {
                    val lastSegment = segmentsQueue.last()

                    fun getLastCorrectPosition(): Vec2 {
                        for (i in segmentsQueue.reversed()) {
                            val pos = i.targetPosition(i.duration())

                            if (pos != null)
                                return pos
                        }

                        return targetOrientation.pos
                    }

                    Orientation(
                        getLastCorrectPosition(),
                        lastSegment.targetHeading(lastSegment.duration())
                    )
                }
            } else
                it.startOrientation

        it.builder = TrajectoryBuilder(
            TrajectoryBuilderParams(1e-6, ProfileParams(0.1, 0.1, 0.01)),
            Pose2d(startOrientation!!.x, startOrientation.y, startOrientation.angl.angle), 0.0,
            MinVelConstraint(
                listOf(
                    TranslationalVelConstraint(RUNNER_CONFIG.LINER_VELOCITY),
                    AngularVelConstraint(RUNNER_CONFIG.HEADING_VELOCITY)
                )
            ),
            ProfileAccelConstraint(-RUNNER_CONFIG.LINEAR_ACCEL, RUNNER_CONFIG.LINEAR_ACCEL)
        )
    }

    collector.startEvent += {
        if (segmentsQueue.isNotEmpty())
            segmentTimer.reset()
    }

    collector.updateEvent += {
        val odometry = collector.eventBus.invoke(GetRobotOdometry())

        val time = segmentTimer.seconds()

        if (segmentsQueue.isNotEmpty()) {
            val currentSegment = segmentsQueue.first()

            if (currentSegment.isEnd(time)) {
                segmentsQueue.removeFirst()
                segmentTimer.reset()
            } else {
                val segmentDuration = currentSegment.duration()

                targetOrientation = Orientation(
                    currentSegment.targetPosition(segmentDuration) ?: targetOrientation.pos,
                    currentSegment.targetHeading(segmentDuration)
                )
                collector.eventBus.invoke(
                    SetDriveVelocityEvent(
                        currentSegment.linearVelocity(time).turn(-odometry.orientation.angle) +
                                (targetOrientation.pos - odometry.orientation.pos).turn(-odometry.orientation.angle) *
                                Vec2(RUNNER_CONFIG.POSITION_P_X, RUNNER_CONFIG.POSITION_P_Y),
                        currentSegment.headingVelocity(time) +
                                (targetOrientation.angl - odometry.orientation.angl).angle * RUNNER_CONFIG.HEADING_P_H
                    )
                )
            }
        } else {
            collector.eventBus.invoke(
                SetDriveVelocityEvent(
                    (targetOrientation.pos - odometry.orientation.pos).turn(-odometry.orientation.angle) *
                            Vec2(RUNNER_CONFIG.POSITION_P_X, RUNNER_CONFIG.POSITION_P_Y),
                    (targetOrientation.angl - odometry.orientation.angl).angle * RUNNER_CONFIG.HEADING_P_H
                )
            )
        }
    }
}