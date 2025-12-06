package org.woen.modules.runner.actions

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.runBlocking
import org.woen.hotRun.HotRun
import org.woen.modules.driveTrain.SetDriveTargetVelocityEvent
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent
import org.woen.modules.scoringSystem.simple.StopBeltEvent
import org.woen.modules.scoringSystem.turret.WaitTurretAtTargetEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Vec2
import kotlin.concurrent.thread

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
        runBlocking {
            ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.STOP))
            ThreadedEventBus.LAZY_INSTANCE.invoke(StopBeltEvent())

            ThreadedEventBus.LAZY_INSTANCE.invoke(WaitTurretAtTargetEvent()).targetProcess.wait()

            Thread.sleep(15000)

            ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveTargetVelocityEvent(Vec2(12.0), 0.0))

            Thread.sleep(1000)

            ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveTargetVelocityEvent(Vec2.ZERO, 0.0))
        }
    })

    fun init() {
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