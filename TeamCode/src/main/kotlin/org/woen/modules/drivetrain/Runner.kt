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
    var POSITION_WINDOW = 0.15

    @JvmField
    var ANGLE_WINDOW = toRadians(25.0)

    @JvmField
    var X_REGULATOR = RegulatorParameters(kP = 2.5)

    @JvmField
    var Y_REGULATOR = RegulatorParameters(kP = 2.5)

    @JvmField
    var H_REGULATOR = RegulatorParameters(kP = 5.0)

    @JvmField
    var TARGET_TIMER = 0.01
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

class MoveSegment(private val endPoint: Vec2, private val velocityConstrain: Double = -1.0, val isFlyingPoint: Boolean = false) :
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
class GetRunnerAtTargetPositionEvent(var atTarget: Boolean = true)
class GetRunnerAtTargetAngleEvent(var atTarget: Boolean = true)

fun attachRunner(collector: Collector) {
    val segmentsQueue = ArrayDeque<ITrajectorySegment>()

    var targetOrientation = GameSettings.startOrientation.startOrientation
    var atTargetPosition = true
    var atTargetRotation = true
    var isFlyingPoint = false
    val targetPositionTimer = ElapsedTime()
    val targetRotationTimer = ElapsedTime()

    var linearVelocityConstrain: Double? = null
    var headingVelocityConstrain: Double? = null

    val xRegulator = Regulator(RUNNER_CONFIG.X_REGULATOR)
    val yRegulator = Regulator(RUNNER_CONFIG.Y_REGULATOR)
    val hRegulator = Regulator(RUNNER_CONFIG.H_REGULATOR)

    collector.startEvent += {
        xRegulator.start()
        yRegulator.start()
        hRegulator.start()
    }

    fun updateTarget() {
        if(segmentsQueue.isNotEmpty()) {
            val first = segmentsQueue.first()

            if(first is TurnSegment && atTargetRotation){
                atTargetRotation = false
                isFlyingPoint = false

                segmentsQueue.removeFirst()

                targetOrientation = Orientation(targetOrientation.pos, first.targetHeading())
                headingVelocityConstrain = first.headingVelocityConstrain()
            }

            if(first is MoveSegment && atTargetPosition){
                atTargetPosition = false

                segmentsQueue.removeFirst()

                targetOrientation = Orientation(first.targetPosition(), targetOrientation.angl)
                linearVelocityConstrain = first.linearVelocityConstrain()

                isFlyingPoint = first.isFlyingPoint
            }


            if(first is DriveSegment && atTargetPosition && atTargetRotation){
                atTargetRotation = false
                atTargetPosition = false
                isFlyingPoint = false

                segmentsQueue.removeFirst()

                targetOrientation = Orientation(first.targetPosition(), first.targetHeading())

                linearVelocityConstrain = first.linearVelocityConstrain()
                headingVelocityConstrain = first.headingVelocityConstrain()
            }
        }
    }

    collector.eventBus.subscribe(RunSegmentsEvent::class) {
        segmentsQueue.addAll(it.segments)

        updateTarget()
    }

    collector.eventBus.subscribe(GetRunnerIsFinishedEvent::class) {
        it.finished = segmentsQueue.isEmpty() && atTargetPosition && atTargetRotation
    }

    collector.eventBus.subscribe(GetRunnerAtTargetAngleEvent::class){
        it.atTarget = atTargetRotation
    }

    collector.eventBus.subscribe(GetRunnerAtTargetPositionEvent::class){
        it.atTarget = atTargetPosition
    }

    collector.updateEvent += {
        val odometry = collector.eventBus.invoke(GetRobotOdometry())

        val err = targetOrientation - odometry.orientation
        val localErr = err.pos.turn(-odometry.orientation.angle)

        var linearVelocity = Vec2(
            xRegulator.update(localErr.x, 0.0, collector.battery.currentVoltage),
            yRegulator.update(localErr.y, 0.0, collector.battery.currentVoltage)
        )

        var headingVelocity = hRegulator.update(err.angle, 0.0, collector.battery.currentVoltage)

        linearVelocityConstrain?.let {
            if (linearVelocity.length() > it)
                linearVelocity = Vec2(it, 0.0).setRot(linearVelocity.rot())
        }

        if(isFlyingPoint)
            linearVelocity = Vec2(3.0, 0.0).setRot(linearVelocity.rot())

        headingVelocityConstrain?.let {
            if (headingVelocity.absoluteValue > it)
                headingVelocity = it.withSign(headingVelocity.sign)
        }

        collector.eventBus.invoke(SetDriveVelocityEvent(linearVelocity, headingVelocity))

        if (err.pos.length() < RUNNER_CONFIG.POSITION_WINDOW)
            atTargetPosition = targetPositionTimer.seconds() > RUNNER_CONFIG.TARGET_TIMER
        else {
            atTargetPosition = false
            targetPositionTimer.reset()
        }

        if(abs(err.angle) < RUNNER_CONFIG.ANGLE_WINDOW)
            atTargetRotation = targetRotationTimer.seconds() > RUNNER_CONFIG.TARGET_TIMER
        else{
            atTargetRotation = false
            targetRotationTimer.reset()
        }

        updateTarget()
    }
}