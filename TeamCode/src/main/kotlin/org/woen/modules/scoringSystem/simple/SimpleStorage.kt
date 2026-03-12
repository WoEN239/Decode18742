package org.woen.modules.scoringSystem.simple

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.enumerators.Ball
import org.woen.modules.IModule
import org.woen.modules.driveTrain.RequireRobotLocatedShootingArea
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent
import org.woen.modules.scoringSystem.turret.RequestRotateAtTarget
import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.Hardware
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.ThreadedServo
import org.woen.utils.smartMutex.SmartMutex

data class StartSorting(val bal1: Ball, val bal2: Ball, val bal3: Ball)

class SimpleStorage : IModule {
    private val _hardwareExpansionStorage = ExpansionHardwareSimpleStorage()
    private val _hardwareControlStorage = ControlHardwareSimpleStorage()

    private var _storageJob: Job? = null
    private var _eatJob: Job? = null

    private val _turretGateServo = ThreadedServo(
        Hardware.DEVICE_NAMES.TURRET_GATE_SERVO,
        startPosition = Hardware.VALUES.SERVO.TURRET_GATE_CLOSE
    )

    private val _gateServo = ThreadedServo(
        Hardware.DEVICE_NAMES.GATE_SERVO,
        startPosition = Hardware.VALUES.SERVO.GATE_CLOSE
    )

    private val _pushServo = ThreadedServo(
        Hardware.DEVICE_NAMES.PUSH_SERVO,
        startPosition = Hardware.VALUES.SERVO.PUSH_CLOSE
    )

    enum class StorageState {
        SHOOTING,
        EATING,
        STOP,
        SORTING
    }

    private var _currentState = StorageState.STOP

    private var _requiredSwaps = 0;

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareExpansionStorage)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(
            _turretGateServo,
            _hardwareControlStorage,
            _gateServo,
            _pushServo
        )

        _hardwareControlStorage.detectEvent += {
            if (_currentState == StorageState.STOP) {
                _currentState = StorageState.EATING

                _eatJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                    _hardwareExpansionStorage.beltState =
                        ExpansionHardwareSimpleStorage.BeltState.RUN

                    delay((Configs.SIMPLE_STORAGE.PUSH_TIME * 1000.0).toLong())

                    _hardwareExpansionStorage.beltState =
                        ExpansionHardwareSimpleStorage.BeltState.STOP

                    _currentState = StorageState.STOP
                }
            }
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(StartSorting::class, {
            _currentState = StorageState.SORTING
            _requiredSwaps = 3
        })
    }

    override suspend fun process() {
        _storageJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val robotLocatedShootingArea =
                ThreadedEventBus.LAZY_INSTANCE.invoke(RequireRobotLocatedShootingArea()).isLocated
            val turretAtTarget =
                ThreadedEventBus.LAZY_INSTANCE.invoke(RequestRotateAtTarget()).atTarget
            if ((_currentState == StorageState.STOP || _currentState == StorageState.EATING) && robotLocatedShootingArea && turretAtTarget) {
                _currentState = StorageState.SHOOTING

                if (_eatJob?.isCompleted == false)
                    _eatJob!!.join()

                _turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_OPEN

                while (!_turretGateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                    delay(5)

                _hardwareExpansionStorage.beltState =
                    ExpansionHardwareSimpleStorage.BeltState.SHOOTING
            } else if (_currentState == StorageState.SHOOTING && (!robotLocatedShootingArea || !turretAtTarget)) {
                _hardwareExpansionStorage.beltState =
                    ExpansionHardwareSimpleStorage.BeltState.REVERS

                delay((Configs.SIMPLE_STORAGE.REVERS_TIMER * 1000.0).toLong())

                _turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_CLOSE

                while (!_turretGateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                    delay(5)

                _hardwareExpansionStorage.beltState =
                    ExpansionHardwareSimpleStorage.BeltState.RUN

                delay((Configs.SIMPLE_STORAGE.PUSH_TIME * 1000.0).toLong())

                _hardwareExpansionStorage.beltState =
                    ExpansionHardwareSimpleStorage.BeltState.STOP

                _currentState = StorageState.STOP
            } else if (_currentState == StorageState.SORTING) {
                ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.REVERSE))

                _turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_OPEN

                while (_turretGateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                    delay(5)

                while (_requiredSwaps > 0 && !Thread.currentThread().isInterrupted) {
                    _pushServo.targetPosition = Hardware.VALUES.SERVO.PUSH_OPEN

                    while (_pushServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                        delay(5)

                    _pushServo.targetPosition = Hardware.VALUES.SERVO.PUSH_CLOSE

                    while (_pushServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                        delay(5)

                    _hardwareExpansionStorage.beltState =
                        ExpansionHardwareSimpleStorage.BeltState.RUN

                    delay((Configs.SIMPLE_STORAGE.SORTING_PUSH_TIME * 1000.0).toLong())

                    _hardwareExpansionStorage.beltState =
                        ExpansionHardwareSimpleStorage.BeltState.STOP

                    _requiredSwaps--
                }

                _turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_CLOSE

                while (_turretGateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                    delay(5)

                ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.FORWARD))

                _currentState = StorageState.STOP
            }
        }
    }

    override val isBusy: Boolean
        get() = _storageJob != null && !_storageJob!!.isCompleted

    override fun opModeStart() {
        _turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_CLOSE
        _gateServo.targetPosition = Hardware.VALUES.SERVO.GATE_CLOSE
        _pushServo.targetPosition = Hardware.VALUES.SERVO.PUSH_CLOSE
        _currentState = StorageState.STOP
        _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.STOP
        _requiredSwaps = 0
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}