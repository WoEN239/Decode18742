package org.woen.modules.scoringSystem.simple

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.SetLookModeEvent
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.brush.SwitchBrush
import org.woen.modules.scoringSystem.turret.RequestTurretAtTargetEvent
import org.woen.modules.scoringSystem.turret.SetCurrentTurretStateEvent
import org.woen.modules.scoringSystem.turret.Turret
import org.woen.telemetry.Configs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.ThreadedServo
import org.woen.utils.process.Process
import kotlin.math.PI

data class SimpleShootEvent(val process: Process = Process())
class TerminateSimpleShootEvent()

class SimpleStorage : IModule {
    private val _hardwareStorage = HardwareSimpleStorage()

    private val _gateServo = ThreadedServo(
        Configs.HARDWARE_DEVICES_NAMES.TURRET_GATE_SERVO,
        _startAngle = Configs.STORAGE.TURRET_GATE_SERVO_CLOSE_VALUE * PI * 1.5
    )

    private var _currentShootCoroutine: Job? = null

    private fun terminateShoot() {
        ThreadedEventBus.LAZY_INSTANCE.invoke(SetLookModeEvent(false))

        _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN

        ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrush(Brush.AcktBrush.ACKT))

        ThreadedEventBus.LAZY_INSTANCE.invoke(SetCurrentTurretStateEvent(Turret.TurretState.WAITING))

        _gateServo.targetAngle = Configs.STORAGE.TURRET_GATE_SERVO_CLOSE_VALUE * PI * 1.5
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareStorage, _gateServo)

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SimpleShootEvent::class, {
            _currentShootCoroutine = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                val process = ThreadedEventBus.LAZY_INSTANCE.invoke(SetLookModeEvent(true)).process

                ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrush(Brush.AcktBrush.NOT_ACKT))

                _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.STOP

                _gateServo.targetAngle = Configs.STORAGE.TURRET_GATE_SERVO_OPEN_VALUE * PI * 1.5

                ThreadedEventBus.LAZY_INSTANCE.invoke(SetCurrentTurretStateEvent(Turret.TurretState.SHOOT)).targetProcess.wait()

                process.wait()

                while (!_gateServo.atTargetAngle)
                    delay(5)

                repeat(3) {
                    _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN

                    delay((Configs.SIMPLE_STORAGE.BELT_PUSH_TIME * 1000.0).toLong())

                    _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.STOP

                    var turretAtTarget = ThreadedEventBus.LAZY_INSTANCE.invoke(
                        RequestTurretAtTargetEvent()
                    ).atTarget

                    while (!turretAtTarget) {
                        delay(5)

                        turretAtTarget = ThreadedEventBus.LAZY_INSTANCE.invoke(
                            RequestTurretAtTargetEvent()
                        ).atTarget
                    }
                }

                _hardwareStorage.beltState = HardwareSimpleStorage.BeltState.RUN

                delay((Configs.SIMPLE_STORAGE.END_TIME * 1000.0).toLong())

                terminateShoot()
            }
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateSimpleShootEvent::class, {
            if (_currentShootCoroutine != null) {
                _currentShootCoroutine?.cancel()
                terminateShoot()
            }
        })

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({it.left_bumper}, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent())
        }))

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({it.right_bumper}, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateSimpleShootEvent())
        }))
    }

    override suspend fun process() {

    }

    override val isBusy: Boolean
        get() = true

    override fun dispose() {

    }
}