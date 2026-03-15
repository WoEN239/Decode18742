package org.woen.modules.scoringSystem.simple

import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.camera.Camera
import org.woen.modules.driveTrain.RequireOdometryEvent
import org.woen.modules.driveTrain.RequireRobotLocatedShootingArea
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent
import org.woen.modules.scoringSystem.turret.RequestRotateAtTarget
import org.woen.telemetry.ThreadedTelemetry
import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.Hardware
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.ThreadedServo

data class StartSorting(val bal1: Ball.Name, val bal2: Ball.Name, val bal3: Ball.Name)

class SimpleShootEvent()

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
        startPosition = Hardware.VALUES.SERVO.PUSH_CLOSE,
        a = Configs.SERVO_ANGLE.DEFAULT_SERVO_A * 2.0
    )

    enum class StorageState {
        SHOOTING,
        EATING,
        STOP,
        SORTING
    }

    private var _currentState = StorageState.STOP

    private var _requiredSwaps = 0

    private val _validTimer = ElapsedTime()

    private var _isGamepadShooting = false

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareExpansionStorage)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(
            _turretGateServo,
            _hardwareControlStorage,
            _gateServo,
            _pushServo
        )


        ThreadedEventBus.LAZY_INSTANCE.subscribe(StartSorting::class, {
//            val currentPattern = Camera.LAZY_INSTANCE.currentPattern
//
//            if (currentPattern != null) {
//                val patternGreenNumber = when (BallRequest.Name.GREEN) {
//                    currentPattern.subsequence[0] -> 1
//                    currentPattern.subsequence[1] -> 2
//                    else -> 3
//                }
//
//                val storageGreenName = when (Ball.Name.GREEN) {
//                    it.bal1 -> 1
//                    it.bal2 -> 2
//                    else -> 3
//                }
//
//                var swaps = patternGreenNumber - storageGreenName
//
//                if (swaps < 0)
//                    swaps += 3
//
//                if (swaps != 0) {
//                    _currentState = StorageState.SORTING
//                    _requiredSwaps = swaps
//                }
//
//                _turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_CLOSE
//                _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.STOP
//            }
        })

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("storage state", _currentState)
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SimpleShootEvent::class,{
            ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                _turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_OPEN

                while (!_turretGateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                    delay(5)

                _hardwareExpansionStorage.beltState =
                    ExpansionHardwareSimpleStorage.BeltState.SHOOTING

                delay(1000)

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

//            while (_currentState != StorageState.STOP && !Thread.currentThread().isInterrupted)
//                delay(5)
//
//            _isGamepadShooting = true
//
//            delay(1000)
//
//            _isGamepadShooting = false
            }
        })

        ThreadedGamepad.LAZY_INSTANCE.addGamepad1Listener(ThreadedGamepad.createClickDownListener({it.right_bumper},{
            ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent())
        }))
    }

    override suspend fun process() {
        _storageJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if(ThreadedGamepad.LAZY_INSTANCE._bumperPresed){
                if (_currentState == StorageState.STOP) {
                    _currentState = StorageState.EATING

                    _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.RUN
                }
            }
            else {
                _currentState = StorageState.STOP
                _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.STOP
            }

//            val robotLocatedShootingArea =
//                ThreadedEventBus.LAZY_INSTANCE.invoke(RequireRobotLocatedShootingArea()).isLocated
//            val turretAtTarget =
//                ThreadedEventBus.LAZY_INSTANCE.invoke(RequestRotateAtTarget()).atTarget
//
//            val currentVelocity = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())
//
//            val valid = (robotLocatedShootingArea && turretAtTarget && currentVelocity.odometryVelocity.length() < Configs.SIMPLE_STORAGE.VELOCITY_THRESHOLD) || _isGamepadShooting
//
//            if(!valid)
//                _validTimer.reset()

//            if ((_currentState == StorageState.STOP || _currentState == StorageState.EATING) && _isGamepadShooting){
//                _currentState = StorageState.SHOOTING
//
//                if (_eatJob?.isCompleted == false)
//                    _eatJob!!.join()
//
//                _turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_OPEN
//
//                while (!_turretGateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
//                    delay(5)
//
//                _hardwareExpansionStorage.beltState =
//                    ExpansionHardwareSimpleStorage.BeltState.SHOOTING
//            } else if (_currentState == StorageState.SHOOTING && !_isGamepadShooting) {
//                _hardwareExpansionStorage.beltState =
//                    ExpansionHardwareSimpleStorage.BeltState.REVERS
//
//                delay((Configs.SIMPLE_STORAGE.REVERS_TIMER * 1000.0).toLong())
//
//                _turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_CLOSE
//
//                while (!_turretGateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
//                    delay(5)
//
//                _hardwareExpansionStorage.beltState =
//                    ExpansionHardwareSimpleStorage.BeltState.RUN
//
//                delay((Configs.SIMPLE_STORAGE.PUSH_TIME * 1000.0).toLong())
//
//                _hardwareExpansionStorage.beltState =
//                    ExpansionHardwareSimpleStorage.BeltState.STOP
//
//                _currentState = StorageState.STOP
//            } else if (_currentState == StorageState.SORTING) {
//                ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.REVERSE))
//
//                _gateServo.targetPosition = Hardware.VALUES.SERVO.GATE_OPEN
//
//                while (!_gateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
//                    delay(5)
//
//                while (_requiredSwaps > 0 && !Thread.currentThread().isInterrupted) {
//                    _hardwareExpansionStorage.beltState =
//                        ExpansionHardwareSimpleStorage.BeltState.REVERS
//
//                    delay((Configs.SIMPLE_STORAGE.SORTING_REVERS_TIME * 1000.0).toLong())
//
//                    _hardwareExpansionStorage.beltState =
//                        ExpansionHardwareSimpleStorage.BeltState.STOP
//
//                    _pushServo.targetPosition = Hardware.VALUES.SERVO.PUSH_OPEN
//
//                    while (!_pushServo.atTargetAngle && !Thread.currentThread().isInterrupted)
//                        delay(5)
//
//                    _pushServo.targetPosition = Hardware.VALUES.SERVO.PUSH_CLOSE
//
//                    while (!_pushServo.atTargetAngle && !Thread.currentThread().isInterrupted)
//                        delay(5)
//
//                    _hardwareExpansionStorage.beltState =
//                        ExpansionHardwareSimpleStorage.BeltState.RUN
//
//                    delay((Configs.SIMPLE_STORAGE.SORTING_PUSH_TIME * 1000.0).toLong())
//
//                    _hardwareExpansionStorage.beltState =
//                        ExpansionHardwareSimpleStorage.BeltState.STOP
//
//                    _requiredSwaps--
//
//                    delay(200)
//                }
//
//                _gateServo.targetPosition = Hardware.VALUES.SERVO.GATE_CLOSE
//
//                while (!_gateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
//                    delay(5)
//
//                ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.FORWARD))
//
//                _requiredSwaps = 0
//
//                _currentState = StorageState.STOP
//            }
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
        _validTimer.reset()
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}