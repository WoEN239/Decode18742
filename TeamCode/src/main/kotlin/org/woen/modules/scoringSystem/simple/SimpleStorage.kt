package org.woen.modules.scoringSystem.simple

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.SetLookModeEvent
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent
import org.woen.modules.scoringSystem.turret.WaitTurretAtTargetEvent
import org.woen.telemetry.Configs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.ThreadedServo
import org.woen.utils.process.Process

data class SimpleShootEvent(val process: Process = Process())
class TerminateSimpleShootEvent()
class StopBeltEvent()

class SimpleStorage : IModule {
    private val _hardwareStorage = HardwareSimpleStorage()

    private val _gateServo = ThreadedServo(
        Configs.HARDWARE_DEVICES_NAMES.TURRET_GATE_SERVO,
        startPosition = Configs.STORAGE.TURRET_GATE_SERVO_CLOSE_VALUE
    )

    private var _currentShootCoroutine: Job? = null
    private var _isShooting = false

    private fun terminateShoot() {
        _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN

        ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.FORWARD))

        _gateServo.targetPosition = Configs.STORAGE.TURRET_GATE_SERVO_CLOSE_VALUE
        ThreadedEventBus.LAZY_INSTANCE.invoke(SetLookModeEvent(false))

        _isShooting = false
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareStorage, _gateServo)

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SimpleShootEvent::class, {
            _currentShootCoroutine = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                val process = ThreadedEventBus.LAZY_INSTANCE.invoke(SetLookModeEvent(true)).process

                _isShooting = true

                ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.STOP))

                _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.STOP

                _gateServo.targetPosition = Configs.STORAGE.TURRET_GATE_SERVO_OPEN_VALUE

                ThreadedEventBus.LAZY_INSTANCE.invoke(WaitTurretAtTargetEvent()).targetProcess.wait()

                while (!_gateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                    delay(5)

                process.wait()

                delay(90)

                repeat(3) {
                    _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN_REVERS

                    _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN

                    delay((Configs.SIMPLE_STORAGE.BELT_PUSH_TIME * 1000.0).toLong())

                    _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.STOP

                    ThreadedEventBus.LAZY_INSTANCE.invoke(WaitTurretAtTargetEvent()).targetProcess.wait()
                }

                HotRun.LAZY_INSTANCE.gamepadRumble(0.5)

                _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN_REVERS

                delay((Configs.SIMPLE_STORAGE.REVERS_TIME * 1000.0).toLong())

                terminateShoot()
            }
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateSimpleShootEvent::class, {
            if (_currentShootCoroutine != null) {
                _currentShootCoroutine?.cancel()

                _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN_REVERS

                delay((Configs.SIMPLE_STORAGE.REVERS_TIME * 1000.0).toLong())

                terminateShoot()
            }
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(StopBeltEvent::class, {
            _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.STOP
        })

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.left_bumper }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateSimpleShootEvent())
        }))

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.right_bumper }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent())
        }))

        _hardwareStorage.currentTriggerEvent += {
            if (!_isShooting) {
                ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.REVERS))

                _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.STOP
            }
        }
    }

    override suspend fun process() {

    }

    override val isBusy: Boolean
        get() = true

    override fun dispose() {

    }
}