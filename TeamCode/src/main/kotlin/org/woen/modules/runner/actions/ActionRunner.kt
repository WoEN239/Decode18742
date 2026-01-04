package org.woen.modules.runner.actions


import com.acmerobotics.roadrunner.AngularVelConstraint
import com.acmerobotics.roadrunner.MinVelConstraint
import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.TranslationalVelConstraint
import com.acmerobotics.roadrunner.Vector2d
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.runBlocking
import org.woen.hotRun.HotRun
import org.woen.modules.runner.segment.RRTrajectorySegment
import org.woen.modules.runner.segment.RequireRRBuilderEvent
import org.woen.modules.runner.segment.RunSegmentEvent
import org.woen.modules.scoringSystem.simple.SimpleShootEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.PI


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
            TranslationalVelConstraint(0.8),
            AngularVelConstraint(5.0)
        )
    )

    private val _shootingOrientation
        get() = Orientation(
            Vec2(-0.776, -0.656 * _yColorMultiplier),
            Angle(-PI * 0.75 * _hColorMultiplier)
        )

    private suspend fun simpleCloseAuto() {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        _shootingOrientation.pos.rrVec(),
                        _shootingOrientation.angle
                    )
                        .build()
                )
            )
        ).process.wait()

        ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent()).process.wait()

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent()).process.wait()

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
                        .strafeTo(Vector2d(0.353, -1.25 * _yColorMultiplier), _eatVelConstant)
                        .strafeToLinearHeading(
                            _shootingOrientation.pos.rrVec(),
                            _shootingOrientation.angle
                        )
                        .build()
                )
            )
        ).process.wait()

        ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent()).process.wait()

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent()).process.wait()

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

    suspend fun closeAuto() {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        _shootingOrientation.pos.rrVec(),
                        _shootingOrientation.angle
                    )
                        .build()
                )
            )
        ).process.wait()

        //TODO("просто выстрелить все что есть")

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        Thread.sleep(800)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        //TODO("просто выстрелить все что есть")

        for (i in 1..3) {
            ThreadedEventBus.LAZY_INSTANCE.invoke(
                RunSegmentEvent(
                    RRTrajectorySegment(
                        ThreadedEventBus.LAZY_INSTANCE.invoke(
                            RequireRRBuilderEvent()
                        ).trajectoryBuilder!!
                            .setTangent(0.4 * _hColorMultiplier)
                            .splineToLinearHeading(
                                Pose2d(
                                    0.2,
                                    -1.4 * _yColorMultiplier,
                                    -PI * 0.7 * _hColorMultiplier
                                ), -PI * 0.5 * _hColorMultiplier
                            )
                            .build()
                    )
                )
            ).process.wait()

            //TODO("хавать пока не забьется")

            ThreadedEventBus.LAZY_INSTANCE.invoke(
                RunSegmentEvent(
                    RRTrajectorySegment(
                        ThreadedEventBus.LAZY_INSTANCE.invoke(
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

            //TODO("просто выстрелить все что есть")
        }

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .setTangent(0.4 * _hColorMultiplier)
                        .splineToLinearHeading(
                            Pose2d(
                                0.2,
                                -1.4 * _yColorMultiplier,
                                -PI * 0.7 * _hColorMultiplier
                            ), -PI * 0.5 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()

        //TODO("хавать пока не забьется")

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        //TODO("выстрелить в мотив")

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        //TODO("выстрелить в мотив")

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
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

        //TODO("выстрелить в мотив")

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeTo(Vector2d(-1.2, -0.656 * _yColorMultiplier))
                        .build()
                )
            )
        ).process.wait()
    }


    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = false) {
        runBlocking {
            if (HotRun.LAZY_INSTANCE.currentStartPosition.position == HotRun.RunPosition.CLOSE) {
                simpleCloseAuto()
                //closeAuto()
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