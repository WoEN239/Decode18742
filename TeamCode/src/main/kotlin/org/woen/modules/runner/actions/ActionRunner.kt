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

import org.woen.hotRun.HotRun
import org.woen.utils.smartMutex.SmartMutex

import org.woen.telemetry.Configs
import org.woen.telemetry.Configs.DELAY

import org.woen.threading.ThreadManager
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI

import org.woen.modules.runner.segment.RRTrajectorySegment
import org.woen.modules.runner.segment.RequireRRBuilderEvent
import org.woen.modules.runner.segment.RunSegmentEvent

import org.woen.modules.scoringSystem.DefaultFireEvent
import org.woen.modules.scoringSystem.simple.SimpleShootEvent
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.FullFinishedIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageGiveStreamDrumRequest
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent
import org.woen.modules.scoringSystem.turret.SetRotateStateEvent
import org.woen.modules.scoringSystem.turret.SetTurretMode
import org.woen.modules.scoringSystem.turret.Turret


class ActionRunner private constructor() : DisposableHandle
{
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
        get() = if (HotRun.LAZY_INSTANCE.currentStartPosition.color == HotRun.RunColor.BLUE) 1.0 else -1.0

    private val _hColorMultiplier
        get() = if (HotRun.LAZY_INSTANCE.currentStartPosition.color == HotRun.RunColor.BLUE) 1.0 else -1.0

    private val _eatVelConstant = MinVelConstraint(
        listOf(
            TranslationalVelConstraint(0.5),
            AngularVelConstraint(5.0)
        )
    )

    private val _shootingOrientation
        get() = Configs.TURRET.SHOOTING_BLUE_ORIENTATION
    
    
    private val _doneShooting       = AtomicBoolean(false)
    private val _ballsInStorage     = AtomicInteger(3)
    private val _activeBallsInCycle = AtomicInteger(3)

    
    
    
    
    private suspend fun simpleCloseAuto() {
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

        EventBusLI.invoke(SimpleShootEvent()).process.wait()

        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(-0.314, -0.716 * _yColorMultiplier),
                        -PI / 2.0 * _hColorMultiplier
                    ).strafeTo(Vector2d(-0.314, -1.25 * _yColorMultiplier), _eatVelConstant)
                        .setReversed(true)
                        .splineTo(
                            Vector2d(-0.05, -1.35 * _yColorMultiplier),
                            -PI / 2.0 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()

        Thread.sleep(900)

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

        EventBusLI.invoke(SimpleShootEvent()).process.wait()

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
                        .strafeTo(Vector2d(0.353, -1.25 * _yColorMultiplier), _eatVelConstant)
                        .strafeToLinearHeading(
                            _shootingOrientation.pos.rrVec(),
                            _shootingOrientation.angle
                        )
                        .build()
                )
            )
        ).process.wait()

        EventBusLI.invoke(SimpleShootEvent()).process.wait()

        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeToLinearHeading(
                            Vector2d(0.95, -0.712 * _yColorMultiplier),
                            -PI / 2.0 * _hColorMultiplier
                        )
                        .strafeTo(Vector2d(1.0, -1.25 * _yColorMultiplier), _eatVelConstant)
                        .strafeToLinearHeading(
                            _shootingOrientation.pos.rrVec(),
                            _shootingOrientation.angle
                        )
                        .build()
                )
            )
        ).process.wait()

        EventBusLI.invoke(SimpleShootEvent()).process.wait()

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

    private suspend fun closeAuto12()
    {
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


        EventBusLI.invoke(SetRotateStateEvent(Turret.RotateState.TO_OBELISK))
        //  Attempt to get pattern



        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(-0.314, -0.716 * _yColorMultiplier),
                        -PI / 2.0 * _hColorMultiplier
                    ).strafeTo(Vector2d(-0.314, -1.25 * _yColorMultiplier), _eatVelConstant)
                        .setReversed(true)
                        .splineTo(
                            Vector2d(-0.05, -1.35 * _yColorMultiplier),
                            -PI / 2.0 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()

        EventBusLI.invoke(SetRotateStateEvent(Turret.RotateState.TO_BASKET))
        Thread.sleep(900)

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
        handleCustomisableShooting()
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
                        .strafeTo(Vector2d(0.353, -1.25 * _yColorMultiplier), _eatVelConstant)
                        .strafeToLinearHeading(
                            _shootingOrientation.pos.rrVec(),
                            _shootingOrientation.angle
                        )
                        .build()
                )
            )
        ).process.wait()



        //------------------------------
        handleCustomisableShooting()
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
                        )
                        .strafeTo(Vector2d(1.0, -1.25 * _yColorMultiplier), _eatVelConstant)
                        .strafeToLinearHeading(
                            _shootingOrientation.pos.rrVec(),
                            _shootingOrientation.angle
                        )
                        .build()
                )
            )
        ).process.wait()



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
    private suspend fun closeAuto24()
    {
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


        EventBusLI.invoke(SetRotateStateEvent(Turret.RotateState.TO_OBELISK))
        //  Attempt to get pattern


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(
                            0.3,
                            -0.79 * _yColorMultiplier
                        ), -PI / 2.0 * _hColorMultiplier
                    )
                        .strafeTo(Vector2d(0.3, -1.15 * _yColorMultiplier), _eatVelConstant)
                        .strafeTo(Vector2d(0.1, -1.35 * _yColorMultiplier))
                        .build()
                )
            )
        ).process.wait()


        EventBusLI.invoke(SetRotateStateEvent(Turret.RotateState.TO_BASKET))


        Thread.sleep(800)


        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeTo(Vector2d(0.1, -0.9 * _yColorMultiplier))
                        .setTangent(PI / 2.0 * _hColorMultiplier)
                        .strafeToLinearHeading(
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
        
        

        for (i in 1..3)
        {
            EventBusLI.invoke(
                RunSegmentEvent(
                    RRTrajectorySegment(
                        EventBusLI.invoke(
                            RequireRRBuilderEvent()
                        ).trajectoryBuilder!!
                            .setTangent(0.4 * _hColorMultiplier)
                            .splineToLinearHeading(
                                Pose2d(
                                    0.2,
                                    -1.4 * _yColorMultiplier,
                                    -PI * 0.7 * _hColorMultiplier
                                ),  -PI * 0.5 * _hColorMultiplier
                            )
                            .build()
                    )
                )
            ).process.wait()



            //------------------------------
            handleSmartIntake()
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
                                    _shootingOrientation.x,
                                    _shootingOrientation.y,
                                    _shootingOrientation.angle
                                ), -PI * 0.9 * _hColorMultiplier
                            )
                            .build()
                    )
                )
            ).process.wait()



            //------------------------------
            handleStreamShooting()
            //------------------------------
        }

        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .setTangent(0.4 * _hColorMultiplier)
                        .splineToLinearHeading(
                            Pose2d(
                                0.2,
                                -1.4 * _yColorMultiplier,
                                -PI * 0.7 * _hColorMultiplier
                            ),  -PI * 0.5 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()

        
        
        //------------------------------
        handleSmartIntake()
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
                                -0.776,
                                -0.656 * _yColorMultiplier,
                                -PI * 0.75 * _hColorMultiplier
                            ), -PI * 0.9 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()



        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeToLinearHeading(
                            Vector2d(-0.314, -0.79 * _yColorMultiplier),
                            -PI / 2.0 * _hColorMultiplier
                        )
                        .strafeTo(Vector2d(-0.314, -1.15 * _yColorMultiplier), _eatVelConstant)
                        .strafeToLinearHeading(
                            _shootingOrientation.pos.rrVec(),
                            _shootingOrientation.angle
                        )
                        .build()
                )
            )
        ).process.wait()



        //------------------------------
        handleCustomisableShooting()
        //------------------------------



        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeToLinearHeading(
                            Vector2d(0.9, -0.79 * _yColorMultiplier),
                            -PI / 2.0 * _hColorMultiplier
                        )
                        .strafeTo(Vector2d(0.9, -1.15 * _yColorMultiplier), _eatVelConstant)
                        .strafeToLinearHeading(
                            _shootingOrientation.pos.rrVec(),
                            _shootingOrientation.angle
                        )
                        .build()
                )
            )
        ).process.wait()



        //------------------------------
        handleCustomisableShooting()
        //------------------------------



        EventBusLI.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    EventBusLI.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeTo(Vector2d(-1.2, -0.656 * _yColorMultiplier))
                        .build()
                )
            )
        ).process.wait()
    }


    private suspend fun handleSmartIntake()
    {
        var waitingForIntakeFinishing: Long = 0
        while(_ballsInStorage.get() < _activeBallsInCycle.get()
            && waitingForIntakeFinishing < 5000)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            waitingForIntakeFinishing += DELAY.EVENT_AWAITING_MS
        }

        val eatenBallCount = _ballsInStorage.get()
        if  (eatenBallCount < _activeBallsInCycle.get())
            _activeBallsInCycle.set(eatenBallCount)
    }
    private suspend fun handleStreamShooting()
    {
        //--  Cycle STREAM Drum shooting (for optimisation see Configs.SORTING_SETTINGS)
        //--  USE_LAZY_DRUM = false   >>>   Maximizes speed, but is more risky

        _doneShooting.set(false)
        EventBusLI.invoke(StorageGiveStreamDrumRequest())

        var waitingForSecondStreamMS: Long = 0
        while(!_doneShooting.get() && waitingForSecondStreamMS < 2000)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            waitingForSecondStreamMS += DELAY.EVENT_AWAITING_MS
        }

        if (waitingForSecondStreamMS > 2000)
            EventBusLI.invoke(TerminateRequestEvent())
        else _ballsInStorage.set(0)
        //------------------------------------------------------------------------------------
    }
    private suspend fun handleCustomisableShooting()
    {
        _doneShooting.set(false)
        EventBusLI.invoke(DefaultFireEvent())

        var waitingForCustomisableDrumMS: Long = 0
        while(!_doneShooting.get() && waitingForCustomisableDrumMS < 3333)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            waitingForCustomisableDrumMS += DELAY.EVENT_AWAITING_MS
        }

        if (waitingForCustomisableDrumMS > 3333)
            EventBusLI.invoke(TerminateRequestEvent())
        else _ballsInStorage.set(0)
    }





    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = false) {
        runBlocking {
            if (HotRun.LAZY_INSTANCE.currentStartPosition.position == HotRun.RunPosition.CLOSE) {
//                simpleCloseAuto()
                closeAuto12()
            }
        }
    })


    fun init() {
        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.AUTO)
                _thread.start()
            
            EventBusLI.subscribe(FullFinishedFiringEvent::class, {
                    _doneShooting.set(true)
            }   )
            EventBusLI.subscribe(FullFinishedIntakeEvent::class, {
                    _ballsInStorage.set(it.ballCountInStorage)
            }   )
        }

        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.AUTO)
                _thread.interrupt()
        }
    }

    override fun dispose() {}
}