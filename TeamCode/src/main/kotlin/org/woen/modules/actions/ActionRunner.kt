package org.woen.modules.actions

import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.Collector
import org.woen.collector.GamePosition
import org.woen.collector.GameSettings
import org.woen.collector.StartOrientation
import org.woen.modules.BallColor
import org.woen.modules.GetCurrentStorageStateEvent
import org.woen.modules.SetTurretStateEvent
import org.woen.modules.ShootEvent
import org.woen.modules.SlowShootEvent
import org.woen.modules.StartEatEvent
import org.woen.modules.StartSortingEvent
import org.woen.modules.StopEatEvent
import org.woen.modules.StorageState
import org.woen.modules.TurretState
import org.woen.modules.drivetrain.DriveSegment
import org.woen.modules.drivetrain.GetRunnerAtTargetAngleEvent
import org.woen.modules.drivetrain.GetRunnerAtTargetPositionEvent
import org.woen.modules.drivetrain.ITrajectorySegment
import org.woen.modules.drivetrain.MoveSegment
import org.woen.modules.drivetrain.RunSegmentsEvent
import org.woen.modules.drivetrain.TurnSegment
import org.woen.utils.events.EventBus
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

interface IAction {
    fun start() {}
    fun update() {}
    fun stop() {}

    fun isEnd(): Boolean = true
}

class DriveAction(val eventBus: EventBus, vararg segments: ITrajectorySegment) : IAction {
    val segments = segments.toMutableList()

    override fun start() {
        eventBus.invoke(RunSegmentsEvent(arrayOf(segments.first())))
    }

    override fun update() {
        if (segments.isNotEmpty()) {
            val first = segments.first()

            val atTargetPosition = eventBus.invoke(GetRunnerAtTargetPositionEvent()).atTarget
            val atTargetAngle = eventBus.invoke(GetRunnerAtTargetAngleEvent()).atTarget

            if ((first is MoveSegment && atTargetPosition) || (first is TurnSegment && atTargetAngle) || (first is DriveSegment && atTargetPosition && atTargetAngle)) {
                segments.removeAt(0)

                if (segments.isNotEmpty())
                    eventBus.invoke(RunSegmentsEvent(arrayOf(segments.first())))
            }
        }
    }

    override fun isEnd() = segments.isEmpty()
}

class WaitAction(val time: Double) : IAction {
    val timer = ElapsedTime()

    override fun start() {
        timer.reset()
    }

    override fun isEnd() = timer.seconds() > time
}

class ParallelActions(
    private val _actions: Array<ArrayList<IAction>>,
    private val _exitType: ExitType
) : IAction {
    enum class ExitType {
        AND, OR
    }

    override fun update() {
        for (i in _actions) {
            if (i.isNotEmpty()) {
                i[0].update()

                if (i[0].isEnd()) {
                    i[0].stop()

                    i.removeAt(0)

                    if (i.isNotEmpty())
                        i[0].start()
                }
            }
        }
    }

    override fun isEnd(): Boolean {
        for (i in _actions)
            if (_exitType == ExitType.OR && i.isEmpty())
                return true
            else if (_exitType == ExitType.AND && i.isNotEmpty())
                return false

        return true
    }

    override fun start() {
        for (i in _actions)
            if (i.isNotEmpty())
                i[0].start()
    }
}

class ShootAction(private val _eventBus: EventBus) : IAction {
    val timer = ElapsedTime()

    override fun start() {
        _eventBus.invoke(ShootEvent())
        timer.reset()
    }

    override fun isEnd() =
    _eventBus.invoke(GetCurrentStorageStateEvent()).state == StorageState.STOP
}

class SlowShootAction(private val _eventBus: EventBus) : IAction {
    override fun start() {
        _eventBus.invoke(SlowShootEvent())
    }

    override fun isEnd() =
        _eventBus.invoke(GetCurrentStorageStateEvent()).state == StorageState.STOP
}

class StartEatAction(private val _eventBus: EventBus) : IAction {
    override fun start() {
        _eventBus.invoke(StartEatEvent())
    }
}

class StopEatAction(private val _eventBus: EventBus) : IAction {
    override fun start() {
        _eventBus.invoke(StopEatEvent())
    }
}

class SortingAction(
    private val _eventBus: EventBus,
    private val _bal1: BallColor,
    private val _bal2: BallColor,
    private val _bal3: BallColor
) : IAction {
    override fun start() {
        _eventBus.invoke(StartSortingEvent(_bal1, _bal2, _bal3))
    }

    override fun isEnd() =
        _eventBus.invoke(GetCurrentStorageStateEvent()).state == StorageState.STOP
}

class TurretStateSwapAction(private val _eventBus: EventBus, private val _state: TurretState) :
    IAction {
    override fun start() {
        _eventBus.invoke(SetTurretStateEvent(_state))
    }
}

class RunActionsEvent(val actions: List<IAction>)

fun attachActionRunner(collector: Collector) {
    val actions = ArrayDeque<IAction>()

    var isOpModeStarted = false

    if(GameSettings.startOrientation == StartOrientation.RED_ULT || GameSettings.startOrientation == StartOrientation.BLUE_ULT)
        actions.addAll(ultTrajectory(collector))
    else {
        if (GameSettings.startOrientation.gamePosition == GamePosition.CLOSE)
            actions.addAll(closeTrajectory(collector))
        else
            actions.addAll(farTrajectory(collector))
    }

    collector.eventBus.subscribe(RunActionsEvent::class) {
        val actionsEmpty = actions.isEmpty()

        actions.addAll(it.actions)

        if (actionsEmpty && isOpModeStarted)
            actions.first().start()
    }

    collector.startEvent += {
        isOpModeStarted = true

        if (actions.isNotEmpty())
            actions.first().start()
    }

    collector.updateEvent += {
        if (actions.isNotEmpty()) {
            actions.first().update()

            if (actions.first().isEnd()) {
                actions.removeFirst().stop()

                if (actions.isNotEmpty())
                    actions.first().start()
            }
        }
    }
}