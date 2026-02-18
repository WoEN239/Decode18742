package org.woen.modules.runner.actions


import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.DisposableHandle

import com.acmerobotics.roadrunner.AngularVelConstraint
import com.acmerobotics.roadrunner.MinVelConstraint
import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.TranslationalVelConstraint
import com.acmerobotics.roadrunner.Vector2d
import org.woen.enumerators.Ball

import org.woen.hotRun.HotRun

import org.woen.modules.camera.OnPatternDetectedEvent
import org.woen.utils.smartMutex.SmartMutex

import org.woen.telemetry.configs.Delay

import org.woen.threading.ThreadManager
import org.woen.telemetry.configs.Alias.LogM
import org.woen.telemetry.configs.Alias.HotRunLI
import org.woen.telemetry.configs.Alias.EventBusLI

import org.woen.modules.runner.segment.RRTrajectorySegment
import org.woen.modules.runner.segment.RequireRRBuilderEvent
import org.woen.modules.runner.segment.RunSegmentEvent

import org.woen.modules.scoringSystem.turret.Turret
import org.woen.modules.scoringSystem.turret.SetRotateStateEvent

import org.woen.modules.scoringSystem.DefaultFireEvent
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StopLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageGiveStreamDrumRequest
import org.woen.modules.scoringSystem.storage.StorageInitiatePredictSortEvent
import org.woen.modules.scoringSystem.storage.StorageUpdateAfterLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent
import org.woen.modules.scoringSystem.storage.sorting.DynamicPattern
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2



class ActionRunner private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: ActionRunner? = null

        private val _instanceMutex = SmartMutex()


        @JvmStatic
        val LAZY_INSTANCE: ActionRunner
            get() {
                var isInitialised = false

                _instanceMutex.smartLock {
                    if (_nullableInstance == null) {
                        _nullableInstance = ActionRunner()
                        isInitialised = true
                    }
                }

                if (isInitialised)
                    _nullableInstance?.init()

                return _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance?.dispose()
                _nullableInstance = null
            }
        }
    }

    private val _yColorMultiplier
        get() = if (HotRunLI.currentStartPosition.color == HotRun.RunColor.BLUE) 1.0 else -1.0

    private val _hColorMultiplier
        get() = if (HotRunLI.currentStartPosition.color == HotRun.RunColor.BLUE) 1.0 else -1.0

    private val _eatVelConstant = MinVelConstraint(
        listOf(
            TranslationalVelConstraint(0.5),
            AngularVelConstraint(5.0)
        )
    )

    private val _shootingOrientation
        get() = HotRunLI.currentStartPosition.shootingOrientation

    private val _eatOrientation
        get() = Orientation(Vec2(0.304, -1.1450 * _yColorMultiplier), Angle.ofDeg(-130.223 * _hColorMultiplier))

    private val _pattern = DynamicPattern()
    private val _patternWasDetected = AtomicBoolean(false)

    private val _doneShooting = AtomicBoolean(false)
    private val _waitForSorting = AtomicBoolean(false)
    private val _sortingIsFinished = AtomicBoolean(false)
    private val _ballsInStorage = AtomicInteger(3)
    private val _activeBallsInCycle = AtomicInteger(3)

    private suspend fun closeAuto12()
    {
//        EventBusLI.invoke(SetRotateStateEvent(Turret.RotateState.TO_OBELISK))
        //  Attempt to get pattern


//        EventBusLI.invoke(
//            RunSegmentEvent(
//                RRTrajectorySegment(
//                    EventBusLI.invoke(
//                        RequireRRBuilderEvent()
//                    ).trajectoryBuilder!!.strafeToLinearHeading(
//                        Vector2d(-0.683, -0.642 * _yColorMultiplier), -PI * 0.75 * _hColorMultiplier
//                    )
//
//                        .build()
//                )
//            )
//        ).process.wait()


//        lookForObelisk()


//        EventBusLI.invoke(SetRotateStateEvent(Turret.RotateState.CONSTANT))


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        _shootingOrientation.pos.rrVec(),
                        _shootingOrientation.angle
                    )
                        .build()
                )
            )
        ).process.wait()


        //------------------------------
        handleStreamShooting()
        //------------------------------


        //------------------------------
        EventBusLI.invoke(StartLazyIntakeEvent())
        //------------------------------


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(-0.314, -0.716 * _yColorMultiplier),
                        -PI / 2.0 * _hColorMultiplier
                    ).strafeTo(Vector2d(-0.314, -1.35 * _yColorMultiplier), _eatVelConstant)
                        .setReversed(true)
                        .splineTo(
                            Vector2d(-0.05, -1.44 * _yColorMultiplier),
                            -PI / 2.0 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()


        //------------------------------
        stopIntakeStartSort(
            arrayOf(
                Ball.Name.PURPLE,
                Ball.Name.PURPLE,
                Ball.Name.GREEN
        )   )
        //------------------------------


        waitForDoorOpening()


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeToLinearHeading(
                            _shootingOrientation.pos.rrVec(),
                            _shootingOrientation.angle
                        )
                        .build()
                )
            )
        ).process.wait()


        //------------------------------
        waitForSortingEnd()
        //------------------------------


        //------------------------------
        handleCustomisableShooting()
        //------------------------------


        //------------------------------
        EventBusLI.invoke(StartLazyIntakeEvent())
        //------------------------------


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeToLinearHeading(
                            Vector2d(0.353, -0.712 * _yColorMultiplier),
                            -PI / 2.0 * _hColorMultiplier
                        )
                        .strafeTo(Vector2d(0.353, -1.45 * _yColorMultiplier), _eatVelConstant)
                        .build()
                )
            )
        ).process.wait()


        //------------------------------
        stopIntakeStartSort(
            arrayOf(
                Ball.Name.PURPLE,
                Ball.Name.GREEN,
                Ball.Name.PURPLE
        )   )
        //------------------------------


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .setTangent(PI / 2.0 * _hColorMultiplier)
                        .splineToLinearHeading(
                            Pose2d(
                                _shootingOrientation.x, _shootingOrientation.y,
                                _shootingOrientation.angle
                            ),
                            -PI * 0.9 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()


        //------------------------------
        waitForSortingEnd()
        //------------------------------


        //------------------------------
        handleCustomisableShooting()
        //------------------------------


        //------------------------------
        EventBusLI.invoke(StartLazyIntakeEvent())
        //------------------------------


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeToLinearHeading(
                            Vector2d(0.95, -0.712 * _yColorMultiplier),
                            -PI / 2.0 * _hColorMultiplier
                        ).build()
        )   )   ).process.wait()

        Thread.sleep(50)

        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeTo(Vector2d(1.0, -1.45 * _yColorMultiplier), _eatVelConstant).build()
                )   )   ).process.wait()


        //------------------------------
        stopIntakeStartSort(
            arrayOf(
                Ball.Name.GREEN,
                Ball.Name.PURPLE,
                Ball.Name.PURPLE
        )   )
        //------------------------------


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeToLinearHeading(
                            _shootingOrientation.pos.rrVec(),
                            _shootingOrientation.angle
                        )
                        .build()
                )
            )
        ).process.wait()


        //------------------------------
        waitForSortingEnd()
        //------------------------------


        //------------------------------
        handleCustomisableShooting()
        //------------------------------


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeTo(Vector2d(-1.2, -0.656 * _yColorMultiplier))
                        .build()
                )
            )
        ).process.wait()
    }

    private suspend fun handleSmartIntake(doPredictSortAfterIntake: Boolean)
    {
        var waitingForIntakeFinishing: Long = 0
        while (_ballsInStorage.get() < _activeBallsInCycle.get()
            && waitingForIntakeFinishing < 5000)
        {
            delay(Delay.MS.AWAIT.EVENTS)
            waitingForIntakeFinishing += Delay.MS.AWAIT.EVENTS
        }

        val eatenBallCount = _ballsInStorage.get()
        if (eatenBallCount < _activeBallsInCycle.get())
            _activeBallsInCycle.set(eatenBallCount)


//        if (doPredictSortAfterIntake && _patternWasDetected.get())
//            EventBusLI.invoke(
//                StorageInitiatePredictSortEvent(
//                    DynamicPattern.trimPattern(
//                        _pattern.lastUnfinished(),
//                        _pattern.permanent()
//            )   )   )
    }
    private fun stopIntakeStartSort(inputFromTurretSlotToBottom: Array<Ball.Name>)
    {
        EventBusLI.invoke(StopLazyIntakeEvent())
        EventBusLI.invoke(TerminateIntakeEvent())

        EventBusLI.invoke(
            StorageUpdateAfterLazyIntakeEvent(
                inputFromTurretSlotToBottom
        )   )


        _sortingIsFinished.set(false)
        if (_patternWasDetected.get())
            _waitForSorting.set(
                EventBusLI.invoke(
                    StorageInitiatePredictSortEvent(
                        _pattern.permanent()
                    )
                ).startingResult
            )
    }

    private suspend fun waitForSortingEnd()
    {
        if (!_waitForSorting.get()) return
        _waitForSorting.set(false)

        var waitingForPredictSortFinishing: Long = 0
        while (!_sortingIsFinished.get() && waitingForPredictSortFinishing < 4444)
        {
            delay(Delay.MS.AWAIT.EVENTS)
            waitingForPredictSortFinishing += Delay.MS.AWAIT.EVENTS
        }
    }

    private suspend fun handleStreamShooting()
    {
        //--  Cycle STREAM Drum shooting (for optimisation see Configs.SORTING_SETTINGS)
        //--  USE_LAZY_DRUM = false   >>>   Maximizes speed, but is more risky

        Thread.sleep(200)

        _doneShooting.set(false)
        EventBusLI.invoke(StorageGiveStreamDrumRequest())

        var waitingForStreamMS: Long = 0
        while (!_doneShooting.get() && waitingForStreamMS < 1000)
        {
            delay(Delay.MS.AWAIT.EVENTS)
            waitingForStreamMS += Delay.MS.AWAIT.EVENTS
        }

        if (waitingForStreamMS > 1000)
            EventBusLI.invoke(TerminateRequestEvent())
        else _ballsInStorage.set(0)
        //------------------------------------------------------------------------------------
    }
    private suspend fun handleCustomisableShooting()
    {
        _doneShooting.set(false)
        EventBusLI.invoke(DefaultFireEvent())

        var waitingForCustomisableDrumMS: Long = 0
        while (!_doneShooting.get() && waitingForCustomisableDrumMS < 4444)
        {
            delay(Delay.MS.AWAIT.EVENTS)
            waitingForCustomisableDrumMS += Delay.MS.AWAIT.EVENTS
        }

        if (waitingForCustomisableDrumMS > 4444)
            EventBusLI.invoke(TerminateRequestEvent())
        else
        {
//            repeat (_ballsInStorage.get())
//                { _pattern.addToTemporary() }

            _ballsInStorage.set(0)
        }
    }


    private fun lookForObelisk()
            = Thread.sleep(888)
    private fun waitForDoorOpening()
        = Thread.sleep(444)



    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = false) {
        runBlocking {
            if (HotRunLI.currentStartPosition.position == HotRun.RunPosition.CLOSE) {
                closeAuto12()
            }
        }
    })


    fun init() {
        HotRunLI.opModeStartEvent += {
            if (HotRunLI.currentRunMode == HotRun.RunMode.AUTO)
                _thread.start()
        }


        EventBusLI.subscribe(OnPatternDetectedEvent::class, {
                _pattern.setPermanent(it.pattern.subsequence)
                _patternWasDetected.set(true)
        }   )

        EventBusLI.subscribe(FullFinishedFiringEvent::class, {
                LogM.log("I RECEIVED FIRING EVENT")
                _doneShooting.set(true)
        }   )
//        EventBusLI.subscribe(FullFinishedIntakeEvent::class, {
//              _ballsInStorage.set(it.ballCountInStorage)
//        }   )
//        EventBusLI.subscribe(StorageFinishedPredictSortEvent::class, {
//              _sortingIsFinished.set(true)
//        }   )

        HotRunLI.opModeStopEvent += {
            if (HotRunLI.currentRunMode == HotRun.RunMode.AUTO)
                _thread.interrupt()
        }
    }

    override fun dispose() {}
}