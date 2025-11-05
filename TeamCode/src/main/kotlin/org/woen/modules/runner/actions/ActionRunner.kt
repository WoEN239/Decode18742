package org.woen.modules.runner.actions

import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.Vector2d
import kotlinx.coroutines.DisposableHandle
import org.woen.hotRun.HotRun
import org.woen.modules.runner.segment.RRTrajectorySegment
import org.woen.modules.runner.segment.RequireRRBuilderEvent
import org.woen.modules.runner.segment.RunSegmentEvent
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
                var isInited = false

                _instanceMutex.smartLock {
                    if (_nullableInstance == null) {
                        _nullableInstance = ActionRunner()
                        isInited = true
                    }
                }

                if (isInited)
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
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            RunSegmentEvent(
                RRTrajectorySegment(
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequireRRBuilderEvent()
                    ).trajectoryBuilder!!.splineToLinearHeading(
                        Pose2d(
                            Vector2d(0.6097, 0.6097),
                            PI
                        ), PI / 2.0
                    ).build()
                )
            )
        )
    })

    fun init() {
        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            if (HotRun.LAZY_INSTANCE.currentRunMode.get() == HotRun.RunMode.AUTO)
                _thread.start()
        }

        HotRun.LAZY_INSTANCE.opModeStopEvent += {
            _thread.interrupt()
        }
    }

    override fun dispose() {

    }
}