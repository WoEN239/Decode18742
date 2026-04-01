package org.woen.modules.drivetrain

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.Collector
import org.woen.collector.GameSettings
import org.woen.utils.regulator.Regulator
import org.woen.utils.regulator.RegulatorParameters
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import java.lang.Math.toRadians
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.withSign

@Config
internal object RUNNER_CONFIG {
    @JvmField
    var POSITION_WINDOW = 0.1

    @JvmField
    var ANGLE_WINDOW = toRadians(15.0)

    @JvmField
    var X_REGULATOR = RegulatorParameters()

    @JvmField
    var Y_REGULATOR = RegulatorParameters()

    @JvmField
    var H_REGULATOR = RegulatorParameters()

    @JvmField
    var TARGET_TIMER = 0.05
}

interface ITrajectorySegment {
    fun targetPosition(): Vec2?
    fun targetHeading(): Angle?
    fun linearVelocityConstrain(): Double?
    fun headingVelocityConstrain(): Double?
}

class TurnSegment(private val endHeading: Angle, private val velocityConstrain: Double = -1.0) :
    ITrajectorySegment {
    override fun targetPosition() = null

    override fun targetHeading() = endHeading

    override fun linearVelocityConstrain() = null

    override fun headingVelocityConstrain() =
        if (velocityConstrain < 0.0) null else velocityConstrain
}

class MoveSegment(private val endPoint: Vec2, private val velocityConstrain: Double = -1.0) :
    ITrajectorySegment {
    override fun targetPosition() = endPoint

    override fun targetHeading() = null

    override fun linearVelocityConstrain() =
        if (velocityConstrain < 0.0) null else velocityConstrain

    override fun headingVelocityConstrain() = null
}

class DriveSegment(
    private val endOrientation: Orientation,
    private val linearVelocityConstrain: Double = -1.0,
    private val headingVelocityConstrain: Double = -1.0
) : ITrajectorySegment {
    override fun targetPosition() = endOrientation.pos

    override fun targetHeading() = endOrientation.angl

    override fun linearVelocityConstrain() =
        if (linearVelocityConstrain < 0.0) null else linearVelocityConstrain

    override fun headingVelocityConstrain() =
        if (headingVelocityConstrain < 0.0) null else headingVelocityConstrain
}

class RunSegmentsEvent(val segments: Array<ITrajectorySegment>)
class GetRunnerIsFinishedEvent(var finished: Boolean = true)

fun attachRunner(collector: Collector) {
    val segmentsQueue = ArrayDeque<ITrajectorySegment>()

    var targetOrientation = GameSettings.startOrientation.startOrientation
    var atTarget = true
    val targetTimer = ElapsedTime()

    var linearVelocityConstrain: Double? = null
    var headingVelocityConstrain: Double? = null

    val xRegulator = Regulator(RUNNER_CONFIG.X_REGULATOR)
    val yRegulator = Regulator(RUNNER_CONFIG.Y_REGULATOR)
    val hRegulator = Regulator(RUNNER_CONFIG.H_REGULATOR)

    collector.startEvent += {
        xRegulator.start()
        yRegulator.start()
        hRegulator.start()

        targetTimer.reset()
    }

    fun updateTarget() {
        if (atTarget && segmentsQueue.isNotEmpty()) {
            val segment = segmentsQueue.removeFirst()

            targetOrientation = Orientation(
                segment.targetPosition() ?: targetOrientation.pos,
                segment.targetHeading() ?: targetOrientation.angl
            )

            if (segment.targetPosition() != null)
                linearVelocityConstrain = segment.linearVelocityConstrain()

            if (segment.targetHeading() != null)
                headingVelocityConstrain = segment.headingVelocityConstrain()
        }
    }

    collector.eventBus.subscribe(RunSegmentsEvent::class) {
        segmentsQueue.addAll(it.segments)

        updateTarget()
    }

    collector.eventBus.subscribe(GetRunnerIsFinishedEvent::class) {
        it.finished = segmentsQueue.isEmpty() && atTarget
    }

    collector.updateEvent += {
        val odometry = collector.eventBus.invoke(GetRobotOdometry())

        val err = targetOrientation - odometry.orientation
        val localErr = err.pos.turn(-odometry.orientation.angle)

        val linearVelocity = Vec2(
            xRegulator.update(localErr.x, 0.0, collector.battery.currentVoltage),
            yRegulator.update(localErr.y, 0.0, collector.battery.currentVoltage)
        )

        var headingVelocity = hRegulator.update(err.angle, 0.0, collector.battery.currentVoltage)

        linearVelocityConstrain?.let {
            if (linearVelocity.x.absoluteValue > it)
                linearVelocity.x = it.withSign(linearVelocity.x.sign)

            if (linearVelocity.y.absoluteValue > it)
                linearVelocity.y = it.withSign(linearVelocity.y.sign)
        }

        headingVelocityConstrain?.let {
            if (headingVelocity.absoluteValue > it)
                headingVelocity = it.withSign(headingVelocity.sign)
        }

        collector.eventBus.invoke(SetDriveVelocityEvent(linearVelocity, headingVelocity))

        if (err.pos.length() < RUNNER_CONFIG.POSITION_WINDOW && abs(err.angle) < RUNNER_CONFIG.ANGLE_WINDOW)
            atTarget = targetTimer.seconds() > RUNNER_CONFIG.TARGET_TIMER
        else {
            atTarget = false
            targetTimer.reset()
        }

        updateTarget()
    }
}