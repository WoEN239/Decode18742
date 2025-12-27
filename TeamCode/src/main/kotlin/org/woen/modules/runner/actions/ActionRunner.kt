package org.woen.modules.runner.actions

import com.acmerobotics.roadrunner.Vector2d
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.runBlocking
import org.woen.hotRun.HotRun
import org.woen.modules.runner.segment.RRTrajectorySegment
import org.woen.modules.runner.segment.RequireRRBuilderEvent
import org.woen.modules.runner.segment.RunSegmentEvent
import org.woen.modules.scoringSystem.simple.SimpleShootEvent
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
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

    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = false) {
        runBlocking {
            ThreadedEventBus.LAZY_INSTANCE.invoke(
                RunSegmentEvent(
                    RRTrajectorySegment(
                        ThreadedEventBus.LAZY_INSTANCE.invoke(
                            RequireRRBuilderEvent()
                        ).trajectoryBuilder!!.strafeToLinearHeading(
                            Vector2d(-0.776, -0.656),
                            -PI * 0.75
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
                            Vector2d(-0.314, -0.716),
                            -PI / 2.0
                        ).strafeTo(Vector2d(-0.314, -1.15))
                            .setReversed(true)
                            .splineTo(
                                Vector2d(-0.05, -1.35),
                                -PI / 2.0
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
                            .strafeToLinearHeading(Vector2d(-0.776, -0.656), -PI * 0.75)
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
                            .strafeToLinearHeading(Vector2d(0.353, -0.712), -PI / 2.0)
                            .strafeTo(Vector2d(0.353, -1.15))
                            .strafeToLinearHeading(Vector2d(-0.776, -0.656), -PI * 0.75)
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
                            .strafeToLinearHeading(Vector2d(0.9, -0.712), -PI / 2.0)
                            .strafeTo(Vector2d(0.9, -1.15))
                            .strafeToLinearHeading(Vector2d(-0.776, -0.656), -PI * 0.75)
                            .build()
                    )
                )
            ).process.wait()

            ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent()).process.wait()
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

    override fun dispose() {

    }
}