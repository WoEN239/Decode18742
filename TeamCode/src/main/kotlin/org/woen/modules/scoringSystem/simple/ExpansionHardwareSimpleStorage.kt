package org.woen.modules.scoringSystem.simple


import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
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
        RUN,
        REVERS,
        STOP,
        SHOOTING
    }

    private lateinit var _beltMotor: MotorOnly

    var beltState = BeltState.STOP
        set(value){
            _beltMotor.power = when (value) {
                BeltState.STOP -> 0.0
                BeltState.SHOOTING -> Configs.SIMPLE_STORAGE.BELTS_POWER
                BeltState.REVERS -> -ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)
                BeltState.RUN -> ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)
            }
            field = value
        }

    private var _fullTriggerTimer = ElapsedTime()

    override fun update() {
//        _beltMotor.power = when (beltState) {
//            BeltState.STOP -> 0.0
//            BeltState.SHOOTING -> Configs.SIMPLE_STORAGE.BELTS_POWER
//            BeltState.REVERS -> -ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)
//            BeltState.RUN -> ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)
//        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _beltMotor =
            MotorOnly(hardwareMap.get(Hardware.DEVICE_NAMES.STORAGE_BELT_MOTOR) as DcMotorEx)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
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