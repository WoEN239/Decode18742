package org.woen.modules

import com.acmerobotics.roadrunner.Trajectory
import org.woen.collector.Collector
import org.woen.modules.drivetrain.DriveSegment
import org.woen.modules.drivetrain.GetEndTrajectoryEvent
import org.woen.modules.drivetrain.GetRunnerIsFinishedEvent
import org.woen.modules.drivetrain.RunSegmentsEvent
import org.woen.modules.drivetrain.TurnSegment
import org.woen.utils.events.EventBus
import org.woen.utils.units.Angle

interface IAction {
    fun start() {}
    fun update() {}
    fun stop() {}

    fun isEnd(): Boolean = true
}

class FollowRRTrajectory(private val _eventBus: EventBus, trajectory: List<Trajectory>) : IAction {
    private val _segment = DriveSegment(trajectory)

    override fun start() {
        _eventBus.invoke(RunSegmentsEvent(arrayOf(_segment)))
    }

    override fun isEnd() = _eventBus.invoke(GetRunnerIsFinishedEvent()).finished
}

class TurnAction : IAction {
    constructor(eventBus: EventBus, endAngle: Angle) {
        _eventBus = eventBus

        val startOrientation = _eventBus.invoke(GetEndTrajectoryEvent()).orientation

        _segment = TurnSegment(endAngle - startOrientation.angl, startOrientation.angl)
    }

    private val _eventBus: EventBus
    private val _segment: TurnSegment

    override fun start() {
        _eventBus.invoke(RunSegmentsEvent(arrayOf(_segment)))
    }

    override fun isEnd() = _eventBus.invoke(GetRunnerIsFinishedEvent()).finished
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
    override fun start() {
        _eventBus.invoke(ShootEvent())
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

class SortingEvent(
    private val _eventBus: EventBus,
    private val _bal1: BallColor,
    private val _bal2: BallColor,
    private val _bal3: BallColor
) : IAction {
    override fun start() {
        _eventBus.invoke(StartSortingEvent(_bal1, _bal2, _bal3))
    }

    override fun isEnd() = _eventBus.invoke(GetCurrentStorageStateEvent()).state == StorageState.STOP
}

class TurretStateSwapAction(private val _eventBus: EventBus, private val _state: TurretState): IAction{
    override fun start() {
        _eventBus.invoke(SetTurretStateEvent(_state))
    }
}

class RunActionsEvent(val actions: List<IAction>)
class GetIsActionsFinishedEvent(var finished: Boolean = true)

fun attachActionRunner(collector: Collector) {
    val actions = ArrayDeque<IAction>()

    var isOpModeStarted = false

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