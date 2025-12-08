package org.woen.modules.runner.actions

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.woen.hotRun.HotRun
import org.woen.modules.camera.OnPatternDetectedEvent
import org.woen.modules.driveTrain.SetDriveTargetVelocityEvent
import org.woen.modules.driveTrain.SetLookModeEvent
import org.woen.modules.scoringSystem.DefaultFireEvent
import org.woen.modules.scoringSystem.simple.SimpleShootEvent
import org.woen.modules.scoringSystem.storage.DisableSortingModuleEvent
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StopLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageUpdateAfterLazyIntakeEvent
import org.woen.modules.scoringSystem.turret.Pattern
import org.woen.telemetry.Configs
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Vec2
import woen239.enumerators.Ball
import woen239.enumerators.BallRequest
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
            sortingAutoLogic()
            //streamAutoLogic()
        }
    })

    suspend fun sortingAutoLogic()
    {
//            val pattern = Pattern(
//                22,
//                arrayOf(BallRequest.Name.PURPLE, BallRequest.Name.GREEN, BallRequest.Name.PURPLE)
//            )
//            ThreadedEventBus.LAZY_INSTANCE.invoke(OnPatternDetectedEvent(pattern))

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SetDriveTargetVelocityEvent(
                Vec2(
                    Configs.DRIVE_TRAIN.DRIVE_VEC_MULTIPLIER,
                    0.0
                ),
                0.0
        )   )

        delay(200)

        ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveTargetVelocityEvent(Vec2.ZERO, 0.0))

        ThreadedEventBus.LAZY_INSTANCE.invoke(SetLookModeEvent(true)).process.wait()


        val preloadPattern: Array<Ball.Name> = arrayOf(Ball.Name.GREEN, Ball.Name.PURPLE, Ball.Name.PURPLE)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            StorageUpdateAfterLazyIntakeEvent(
                preloadPattern))

        ThreadedTelemetry.LAZY_INSTANCE.log("Auto: Start shooting")
        ThreadedEventBus.LAZY_INSTANCE.invoke(DefaultFireEvent())

        ThreadedTelemetry.LAZY_INSTANCE.log("Auto: Send shooting request, awaiting 10 sec")



        ThreadedEventBus.LAZY_INSTANCE.invoke(SetLookModeEvent(false)).process.wait()

        delay(20000)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SetDriveTargetVelocityEvent(
                Vec2(
                    Configs.DRIVE_TRAIN.DRIVE_VEC_MULTIPLIER,
                    0.0
                ), 0.0
        )   )

        delay(500)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SetDriveTargetVelocityEvent(
                Vec2.ZERO, 0.0
        )   )
    }

    suspend fun streamAutoLogic() {
            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SetDriveTargetVelocityEvent(
                    Vec2(
                        Configs.DRIVE_TRAIN.DRIVE_VEC_MULTIPLIER,
                        0.0
                    ), 0.0
                )
            )

            delay(200)

            ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveTargetVelocityEvent(Vec2.ZERO, 0.0))

            ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent())

            delay(15000)

            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SetDriveTargetVelocityEvent(
                    Vec2(
                        Configs.DRIVE_TRAIN.DRIVE_VEC_MULTIPLIER,
                        0.0
                    ), 0.0
                )
            )

            delay(500)

            ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveTargetVelocityEvent(Vec2.ZERO, 0.0))
    }

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