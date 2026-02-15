package org.woen.modules.scoringSystem.simple

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.DriveTrain
import org.woen.modules.driveTrain.SetDriveModeEvent
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent
import org.woen.modules.scoringSystem.storage.Alias.GamepadLI
import org.woen.modules.scoringSystem.turret.WaitRotateAtTarget
import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.Hardware
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
    private val _hardwareExpansionStorage = ExpansionHardwareSimpleStorage()
    private val _hardwareControlStorage = ControlHardwareSimpleStorage()

    private var _storageJob: Job? = null

    private val _gateServo = ThreadedServo(
        Hardware.DEVICE_NAMES.TURRET_GATE_SERVO,
        startPosition = Hardware.VALUES.SERVO.TURRET_GATE_CLOSE
    )

    private val _launchServo = ThreadedServo(
        Hardware.DEVICE_NAMES.LAUNCH_SERVO,
        startPosition = Hardware.VALUES.SERVO.LAUNCH_CLOSE
    )

    private var _currentShootCoroutine: Job? = null
    private var _isShooting = false
    private var _a = false

    private fun terminateShoot() {
        _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.STOP

        ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.STOP))

        _gateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_CLOSE
        ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveModeEvent(DriveTrain.DriveMode.DRIVE))

        _isShooting = false
        _a = false
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hardwareExpansionStorage)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(
            _gateServo,
            _launchServo,
            _hardwareControlStorage
        )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SimpleShootEvent::class, {
            if(!_a){
                _currentShootCoroutine = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                    _a = true
//                    val driveProcess =
//                        ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveModeEvent(DriveTrain.DriveMode.SHOOTING)).process
                    val aimProcess =
                        ThreadedEventBus.LAZY_INSTANCE.invoke(WaitRotateAtTarget()).process

                    _storageJob?.join()

                    _isShooting = true

                    ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.STOP))

                    _hardwareExpansionStorage.beltState =
                        ExpansionHardwareSimpleStorage.BeltState.STOP

                    _gateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_OPEN

                    while (!_gateServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                        delay(5)

//                    driveProcess.wait()
//                aimProcess.wait()

                    _hardwareExpansionStorage.beltState =
                        ExpansionHardwareSimpleStorage.BeltState.SHOOT

                    delay((Configs.SIMPLE_STORAGE.SHOOTING_TIME * 1000.0).toLong())

                    _launchServo.targetPosition = Hardware.VALUES.SERVO.LAUNCH_OPEN

                    while (!_launchServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                        delay(5)

                    _hardwareExpansionStorage.beltState =
                        ExpansionHardwareSimpleStorage.BeltState.STOP

                    ThreadedGamepad.LAZY_INSTANCE.rumble1(0.5)

                    _launchServo.targetPosition = Hardware.VALUES.SERVO.LAUNCH_CLOSE

                    while (!_launchServo.atTargetAngle && !Thread.currentThread().isInterrupted)
                        delay(5)

                    _hardwareExpansionStorage.beltState =
                        ExpansionHardwareSimpleStorage.BeltState.RUN_REVERSE

                    delay((Configs.SIMPLE_STORAGE.REVERS_TIME * 1000.0).toLong())

                    terminateShoot()

                    it.process.close()
                }
            }
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateSimpleShootEvent::class, {
            _currentShootCoroutine?.cancel()

            _hardwareExpansionStorage.beltState =
                ExpansionHardwareSimpleStorage.BeltState.RUN_REVERSE

            delay((Configs.SIMPLE_STORAGE.REVERS_TIME * 1000.0).toLong())

            terminateShoot()
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(StopBeltEvent::class, {
            _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.STOP
        })

//        ThreadedGamepad.LAZY_INSTANCE.addGamepad1Listener(
//            createClickDownListener(
//                { it.left_bumper },
//                {
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateSimpleShootEvent())
//                })
//        )

        ThreadedGamepad.LAZY_INSTANCE.addGamepad1Listener(
            createClickDownListener(
                { it.right_bumper },
                {
//            val located =
//                ThreadedEventBus.LAZY_INSTANCE.invoke(RequireRobotLocatedShootingArea()).isLocated
//
//            if (located)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(SimpleShootEvent())
                })
        )

        GamepadLI.addGamepad1Listener(
            createClickDownListener(
                { it.right_trigger > 0.5 }, {
                    if (!_isShooting) {
                        _hardwareExpansionStorage.beltState =
                            ExpansionHardwareSimpleStorage.BeltState.LAZY_RUN
                        ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.FORWARD))
                    }
                }
            ))
        GamepadLI.addGamepad1Listener(
            createClickDownListener(
                { it.left_trigger > 0.5 }, {
                    if (_hardwareExpansionStorage.beltState ==
                        ExpansionHardwareSimpleStorage.BeltState.LAZY_RUN
                    ) {
                        _hardwareExpansionStorage.beltState =
                            ExpansionHardwareSimpleStorage.BeltState.STOP
                        ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.STOP))
                    }
                }
            ))

//        _hardwareExpansionStorage.currentTriggerEvent += {
//            if (!_isShooting) {
//                ThreadedEventBus.LAZY_INSTANCE.invoke(SwitchBrushStateEvent(Brush.BrushState.REVERSE))
//
//                _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.STOP
//            }
//        }
    }

    override suspend fun process() {
//        if(_hardwareControlStorage.isBall && !_isShooting){
//            _storageJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
//                _isShooting = true
//
//                _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.RUN
//
//                delay((Configs.SIMPLE_STORAGE.PUSH_TIME * 1000.0).toLong())
//
//                _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.STOP
//
//                _isShooting = false
//            }
//        }
    }

    override val isBusy: Boolean
        get() = _storageJob != null && !_storageJob!!.isCompleted

    override fun opModeStart() {
        if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.MANUAL) {
            ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                _isShooting = true

                _hardwareExpansionStorage.beltState =
                    ExpansionHardwareSimpleStorage.BeltState.RUN_REVERSE

                delay((Configs.SIMPLE_STORAGE.REVERS_TIME * 1000.0).toLong())

                _hardwareExpansionStorage.beltState = ExpansionHardwareSimpleStorage.BeltState.STOP

                _isShooting = false
            }
        }
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}