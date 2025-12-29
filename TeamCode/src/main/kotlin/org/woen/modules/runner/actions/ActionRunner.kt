package org.woen.modules.runner.actions


import com.acmerobotics.roadrunner.AngularVelConstraint
import com.acmerobotics.roadrunner.MinVelConstraint
import com.acmerobotics.roadrunner.TranslationalVelConstraint
import com.acmerobotics.roadrunner.Vector2d
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.woen.enumerators.Ball
import org.woen.hotRun.HotRun
import org.woen.modules.runner.segment.RRTrajectorySegment
import org.woen.modules.runner.segment.RequireRRBuilderEvent
import org.woen.modules.runner.segment.RunSegmentEvent
import org.woen.modules.scoringSystem.DefaultFireEvent
import org.woen.modules.scoringSystem.simple.SimpleShootEvent
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StopLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageGiveStreamDrumRequest
import org.woen.modules.scoringSystem.storage.StorageUpdateAfterLazyIntakeEvent
import org.woen.telemetry.Configs.DELAY
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.PI


class ActionRunner private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: ActionRunner? = null

        private val _instanceMutex = SmartMutex()
        private val _ssmFinishedFiring = AtomicBoolean(false)


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
        get() = if (HotRun.LAZY_INSTANCE.currentRunColor == HotRun.RunColor.BLUE) 1.0 else -1.0

    private val _hColorMultiplier
        get() = if (HotRun.LAZY_INSTANCE.currentRunColor == HotRun.RunColor.BLUE) 1.0 else -1.0

    private val _isULT = false

    private val _eatVelConstant = MinVelConstraint(
        listOf(
            TranslationalVelConstraint(0.8),
            AngularVelConstraint(5.0)
        )
    )

    private suspend fun sortingAuto() {
        moveToCloseShootingZone()
        fireStreamDrumRequest()

        moveIntakeDoorZonePhase1()
        fireAutoDrum()

        moveIntakeZonePhase2()
        fireAutoDrum()

        moveIntakeZonePhase3()
        fireAutoDrum()

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeTo(
                        Vector2d(-0.2, -0.656 * _yColorMultiplier)
                    )
                        .build()
                )
            )
        ).process.wait()
    }

    private suspend fun simpleAuto() {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(-0.776, -0.656 * _yColorMultiplier),
                        -PI * 0.75 * _hColorMultiplier
                    )
                        .build()
                )
            )
        ).process.wait()

        ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent()).process.wait()

        if (!_isULT) {
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
                                Vector2d(-0.776, -0.656 * _yColorMultiplier),
                                -PI * 0.75 * _hColorMultiplier
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
                                Vector2d(-0.776, -0.656 * _yColorMultiplier),
                                -PI * 0.75 * _hColorMultiplier
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
                                Vector2d(-0.776, -0.656 * _yColorMultiplier),
                                -PI * 0.75 * _hColorMultiplier
                            )
                            .build()
                    )
                )
            ).process.wait()

            ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent()).process.wait()
        }

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


    private suspend fun moveToCloseShootingZone() {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(-0.776, -0.656 * _yColorMultiplier),
                        -PI * 0.75 * _hColorMultiplier
                    )
                        .build()
                )
            )
        ).process.wait()
    }

    private suspend fun moveIntakeDoorZonePhase1() {
        tryStartLazyIntake()

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(-0.314, -0.716 * _yColorMultiplier),
                        -PI / 2.0 * _hColorMultiplier
                    ).strafeTo(Vector2d(-0.314, -1.15 * _yColorMultiplier))
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
                            Vector2d(-0.776, -0.656 * _yColorMultiplier),
                            -PI * 0.75 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()

        stopLazyIntake()
        val inputFromTurretSlotToBottom =
            arrayOf(Ball.Name.PURPLE, Ball.Name.PURPLE, Ball.Name.GREEN)
        updateAfterLazyIntake(inputFromTurretSlotToBottom)
    }

    private suspend fun moveIntakeZonePhase2() {
        tryStartLazyIntake()

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
                        .strafeTo(Vector2d(0.353, -1.15 * _yColorMultiplier))
                        .strafeToLinearHeading(
                            Vector2d(-0.776, -0.656 * _yColorMultiplier),
                            -PI * 0.75 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()

        stopLazyIntake()
        val inputFromTurretSlotToBottom =
            arrayOf(Ball.Name.PURPLE, Ball.Name.GREEN, Ball.Name.PURPLE)
        updateAfterLazyIntake(inputFromTurretSlotToBottom)
    }

    private suspend fun moveIntakeZonePhase3() {
        tryStartLazyIntake()

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!
                        .strafeToLinearHeading(
                            Vector2d(0.9, -0.712 * _yColorMultiplier),
                            -PI / 2.0 * _hColorMultiplier
                        )
                        .strafeTo(Vector2d(0.9, -1.15 * _yColorMultiplier))
                        .strafeToLinearHeading(
                            Vector2d(-0.776, -0.656 * _yColorMultiplier),
                            -PI * 0.75 * _hColorMultiplier
                        )
                        .build()
                )
            )
        ).process.wait()

        stopLazyIntake()
        val inputFromTurretSlotToBottom =
            arrayOf(Ball.Name.GREEN, Ball.Name.PURPLE, Ball.Name.PURPLE)
        updateAfterLazyIntake(inputFromTurretSlotToBottom)
    }

    private suspend fun fireStreamDrumRequest() {
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveStreamDrumRequest())

        while (!_ssmFinishedFiring.get())
            delay(DELAY.EVENT_AWAITING_MS)
    }

    private suspend fun fireAutoDrum() {
        ThreadedEventBus.LAZY_INSTANCE.invoke(DefaultFireEvent())

        while (!_ssmFinishedFiring.get())
            delay(DELAY.EVENT_AWAITING_MS)
    }

    private fun tryStartLazyIntake() = ThreadedEventBus.LAZY_INSTANCE.invoke(StartLazyIntakeEvent())
    private fun stopLazyIntake() = ThreadedEventBus.LAZY_INSTANCE.invoke(StopLazyIntakeEvent())
    private fun updateAfterLazyIntake(inputFromTurretSlotToBottom: Array<Ball.Name>) {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            StorageUpdateAfterLazyIntakeEvent(
                inputFromTurretSlotToBottom
            )
        )
    }


    suspend fun auto21() {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.strafeToLinearHeading(
                        Vector2d(-0.776, -0.656 * _yColorMultiplier),
                        -PI * 0.75 * _hColorMultiplier
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
                        Vector2d(0.3, -0.79 * _yColorMultiplier), -PI / 2.0 * _hColorMultiplier
                    )
                        .strafeTo(Vector2d(0.3, -1.15 * _yColorMultiplier), _eatVelConstant)
                        .strafeTo(Vector2d(0.1, -1.35 * _yColorMultiplier))
                        .build()
                )
            )
        ).process.wait()
    }


    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = false) {
        runBlocking {
            auto21()
//            simpleAuto()
//            sortingAuto()
        }
    })


    fun init() {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(FullFinishedFiringEvent::class, {
//            ThreadedEventBus.LAZY_INSTANCE.invoke(DisableSortingModuleEvent())
        })

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.AUTO)
                _thread.start()
        }

        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            _thread.interrupt()
        }
    }

    override fun dispose() {}
}