package org.woen.modules.runner.actions

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.woen.hotRun.HotRun
import org.woen.modules.driveTrain.SetDriveTargetVelocityEvent
import org.woen.modules.scoringSystem.DefaultFireEvent
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent
import org.woen.modules.scoringSystem.simple.StopBeltEvent
import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StopLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageUpdateAfterLazyIntakeEvent
import org.woen.modules.scoringSystem.turret.WaitTurretAtTargetEvent
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Vec2
import woen239.enumerators.Ball
import woen239.enumerators.IntakeResult
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


            val preloadPattern: Array<Ball.Name> = arrayOf(Ball.Name.GREEN, Ball.Name.PURPLE, Ball.Name.PURPLE)

            ThreadedEventBus.LAZY_INSTANCE.invoke(
                StorageUpdateAfterLazyIntakeEvent(
                    preloadPattern
                ))

            ThreadedTelemetry.LAZY_INSTANCE.log("Auto: Start shooting")
            ThreadedEventBus.LAZY_INSTANCE.invoke(DefaultFireEvent())

            ThreadedTelemetry.LAZY_INSTANCE.log("Auto: FINISHED shooting")


            delay(2000)
            ThreadedTelemetry.LAZY_INSTANCE.log("Auto: Start LazyIntake")
            while (IntakeResult.DidFail(
            ThreadedEventBus.LAZY_INSTANCE.invoke(StartLazyIntakeEvent(
                IntakeResult.Name.FAIL_UNKNOWN)).startingResult))
            {
                delay(DELAY_FOR_EVENT_AWAITING_MS)
            }


            val intakePattern: Array<Ball.Name> = arrayOf(Ball.Name.GREEN, Ball.Name.PURPLE, Ball.Name.PURPLE)


            ThreadedEventBus.LAZY_INSTANCE.invoke(
                StorageUpdateAfterLazyIntakeEvent(
                    intakePattern
                ))
            ThreadedTelemetry.LAZY_INSTANCE.log("Auto: FINISHED LazyIntake")


            ThreadedTelemetry.LAZY_INSTANCE.log("Auto: Start Shooting")
            ThreadedEventBus.LAZY_INSTANCE.invoke(DefaultFireEvent())
            ThreadedTelemetry.LAZY_INSTANCE.log("Auto: FINISHED shooting")



            ThreadedEventBus.LAZY_INSTANCE.invoke(StopLazyIntakeEvent())



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