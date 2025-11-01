package org.woen.modules.scoringSystem.storage.sorting.hardware


import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.HardwareMap

import java.util.concurrent.atomic.AtomicReference

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import org.woen.threading.ThreadedEventBus
import org.woen.modules.scoringSystem.storage.BottomOpticPareSeesSomethingEvent
import org.woen.modules.scoringSystem.storage.MobileOutOpticPareSeesSomethingEvent

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.SORTING_STORAGE_BELT_MOTOR
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.BOTTOM_OPTIC_PARE_1
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.BOTTOM_OPTIC_PARE_2
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_OUT_OPTIC_PARE_1
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_OUT_OPTIC_PARE_2

import org.woen.telemetry.Configs.STORAGE.SORTING_STORAGE_BELT_MOTOR_DIRECTION
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.OPTIC_PARE_SEES_NOT_BLACK



class HwSorting () : IHardwareDevice
{
    private lateinit var _bottomOpticPare1 : AnalogInput
    private lateinit var _bottomOpticPare2 : AnalogInput

    private lateinit var _mobileOutOpticPare1 : AnalogInput
    private lateinit var _mobileOutOpticPare2 : AnalogInput




    private lateinit var _beltMotor : DcMotorEx
    private val _motorPower = AtomicReference(0.0)



    fun startBeltMotor()
    {
        _motorPower.set(12.0)
    }
    fun stopBeltMotor()
    {
        _motorPower.set(0.0)
    }



    override fun dispose() { }

    override fun update()
    {
        _beltMotor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_motorPower.get())


        if (_bottomOpticPare1.voltage > OPTIC_PARE_SEES_NOT_BLACK ||
            _bottomOpticPare2.voltage > OPTIC_PARE_SEES_NOT_BLACK)
            ThreadedEventBus.LAZY_INSTANCE.invoke(BottomOpticPareSeesSomethingEvent())

        if (_mobileOutOpticPare1.voltage > OPTIC_PARE_SEES_NOT_BLACK ||
            _mobileOutOpticPare2.voltage > OPTIC_PARE_SEES_NOT_BLACK)
            ThreadedEventBus.LAZY_INSTANCE.invoke(MobileOutOpticPareSeesSomethingEvent())
    }

    override fun init(hardwareMap : HardwareMap)
    {
        _bottomOpticPare1 = hardwareMap.get(BOTTOM_OPTIC_PARE_1) as AnalogInput
        _bottomOpticPare2 = hardwareMap.get(BOTTOM_OPTIC_PARE_2) as AnalogInput

        _mobileOutOpticPare1 = hardwareMap.get(MOBILE_OUT_OPTIC_PARE_1) as AnalogInput
        _mobileOutOpticPare2 = hardwareMap.get(MOBILE_OUT_OPTIC_PARE_2) as AnalogInput



        _beltMotor = hardwareMap.get(SORTING_STORAGE_BELT_MOTOR) as DcMotorEx

        _beltMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _beltMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _beltMotor.direction = SORTING_STORAGE_BELT_MOTOR_DIRECTION
    }
}