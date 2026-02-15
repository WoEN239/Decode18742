package org.woen.modules.scoringSystem.simple


import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.launch

import org.woen.hotRun.HotRun
import org.woen.utils.motor.MotorOnly
import org.woen.utils.events.SimpleEvent

import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.Hardware

import org.woen.threading.ThreadManager
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery



class ExpansionHardwareSimpleStorage : IHardwareDevice {
    enum class BeltState {
        LAZY_RUN,
        STOP,
        RUN_REVERSE,
        RUN,
        SHOOT
    }

    private lateinit var _beltMotor: MotorOnly

    private var _isStart = false

    var beltState = BeltState.STOP
        get() {
            return field
        }
        set(value) {
            field = value

            if(value == BeltState.SHOOT) {
                ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                    _isStart = true

                    val timer = ElapsedTime()

                    while (timer.seconds() < 0.15)
                        _beltMotor.power =
                            ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_SHOOT_POWER * (timer.seconds() / 0.15))

                    _isStart = false
                }
            }
        }
    var beltsCurrent = 0.0

    val currentTriggerEvent = SimpleEvent<Int>()

    private var _fullTriggerTimer = ElapsedTime()

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState != HotRun.RunState.RUN)
            return

        if(!_isStart) {
            _beltMotor.power = when (beltState) {
                BeltState.STOP -> 0.0

                BeltState.RUN_REVERSE -> -ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)

                BeltState.RUN -> ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)

                BeltState.LAZY_RUN -> ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)

                BeltState.SHOOT -> 1.0//ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_SHOOT_POWER)
            }
        }

        beltsCurrent = _beltMotor.getCurrent(CurrentUnit.AMPS)

        if (beltsCurrent > Configs.SIMPLE_STORAGE.BELTS_FULL_CURRENT)
            if (_fullTriggerTimer.seconds() > Configs.SIMPLE_STORAGE.BELTS_FULL_TIMER)
                currentTriggerEvent.invoke(0)
        else _fullTriggerTimer.reset()
    }

    override fun init(hardwareMap: HardwareMap) {
        _beltMotor =
            MotorOnly(hardwareMap.get(Hardware.DEVICE_NAMES.STORAGE_BELT_MOTOR) as DcMotorEx)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

            _beltMotor.direction = Hardware.VALUES.BELTS.MOTORS_DIRECTION
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("beltsCurrent", beltsCurrent)
        }
    }

    override fun opModeStart() {
        _fullTriggerTimer.reset()
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}