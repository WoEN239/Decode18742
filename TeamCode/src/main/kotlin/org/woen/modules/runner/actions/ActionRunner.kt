package org.woen.modules.runner.actions


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
import org.woen.utils.smartMutex.SmartMutex
import org.woen.threading.ThreadManager
import org.woen.modules.runner.segment.RRTrajectorySegment
import org.woen.modules.runner.segment.RequireRRBuilderEvent
import org.woen.modules.runner.segment.RunSegmentEvent
import org.woen.modules.scoringSystem.simple.SimpleShootEvent
import org.woen.modules.scoringSystem.turret.Turret
import org.woen.modules.scoringSystem.turret.SetRotateStateEvent
import org.woen.modules.scoringSystem.simple.StartSorting
import org.woen.modules.scoringSystem.turret.WaitRotateAtTarget
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedEventBus

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
        get() = HotRun.LAZY_INSTANCE.currentStartPosition.shootingOrientation

    private suspend fun farAuto(){
        //Vec2(1.631, -0.39), Angle(PI)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeTo(
                        Vector2d(1.031, -0.39 * _yColorMultiplier)
                    )

                        .build()
                )
            )
        ).process.wait()
    }

    private suspend fun closeAuto12()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(SetRotateStateEvent(Turret.RotateState.TO_OBELISK))

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(-0.683, -0.642 * _yColorMultiplier), -PI * 0.75 * _hColorMultiplier
                    )

                        .build()
                )
            )
        ).process.wait()

        Thread.sleep(200)

        ThreadedEventBus.LAZY_INSTANCE.invoke(SetRotateStateEvent(Turret.RotateState.TO_BASKET))
//        ThreadedEventBus.LAZY_INSTANCE.invoke(WaitRotateAtTarget()).process.wait()
        Thread.sleep(1000)

        ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent())

        Thread.sleep(6000)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(-0.314, -0.716 * _yColorMultiplier),
                        -PI / 2.0 * _hColorMultiplier
                    ).strafeTo(Vector2d(-0.314, -1.35 * _yColorMultiplier), _eatVelConstant)
//                        .setReversed(true)
//                        .splineTo(
//                            Vector2d(-0.05, -1.44 * _yColorMultiplier),
//                            -PI / 2.0 * _hColorMultiplier
//                        )
                        .build()
                )
            )
        ).process.wait()

        ThreadedEventBus.LAZY_INSTANCE.invoke(StartSorting(Ball.Name.PURPLE, Ball.Name.PURPLE, Ball.Name.GREEN))

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        Thread.sleep(1000)

        ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent())

        Thread.sleep(6000)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        ThreadedEventBus.LAZY_INSTANCE.invoke(StartSorting(Ball.Name.PURPLE, Ball.Name.GREEN, Ball.Name.PURPLE))

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        Thread.sleep(1000)

        ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent())

        Thread.sleep(6000)
//
//        ThreadedEventBus.LAZY_INSTANCE.invoke(
//            RunSegmentEvent(
//                RRTrajectorySegment(
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(
//                        RequireRRBuilderEvent()
//                    ).trajectoryBuilder!!
//                        .strafeToLinearHeading(
//                            Vector2d(0.95, -0.712 * _yColorMultiplier),
//                            -PI / 2.0 * _hColorMultiplier
//                        )
//                        .strafeTo(Vector2d(1.0, -1.45 * _yColorMultiplier), _eatVelConstant).build()
//                )   )   ).process.wait()
//
//        ThreadedEventBus.LAZY_INSTANCE.invoke(StartSorting(Ball.Name.GREEN, Ball.Name.PURPLE, Ball.Name.PURPLE))
//
//        ThreadedEventBus.LAZY_INSTANCE.invoke(
//            RunSegmentEvent(
//                RRTrajectorySegment(
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(
//                        RequireRRBuilderEvent()
//                    ).trajectoryBuilder!!
//                        .strafeToLinearHeading(
//                            _shootingOrientation.pos.rrVec(),
//                            _shootingOrientation.angle
//                        )
//                        .build()
//                )
//            )
//        ).process.wait()
//
//        delay(500)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeTo(Vector2d(-1.2, -0.656 * _yColorMultiplier))
                        .build()
                )
            )
        ).process.wait()
    }

    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = false) {
        runBlocking {
            if (HotRun.LAZY_INSTANCE.currentStartPosition.position == HotRun.RunPosition.CLOSE) {
//                farAuto()
                closeAuto12()
            }
        }
    })

    fun init() {
        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.AUTO)
                _thread.start()
        }
        
        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.AUTO)
                _thread.interrupt()
        }
    }

    override fun dispose() {}
}