package org.woen.modules.scoringSystem.storage.sorting.hardware


import woen239.FixColorSensor.fixSensor
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import java.util.concurrent.atomic.AtomicReference

import org.woen.threading.ThreadedEventBus
import org.woen.modules.scoringSystem.storage.BottomOpticPareSeesSomethingEvent
import org.woen.modules.scoringSystem.storage.MobileOutOpticPareSeesSomethingEvent

import org.woen.telemetry.Configs.STORAGE.SORTING_BELT_MOTOR_DIRECTION
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.OPTIC_PARE_SEES_NOT_BLACK



class HwSorting (private val _deviceName: String) : IHardwareDevice
{
    private lateinit var _bottomOpticPare1 : AnalogInput
    private lateinit var _bottomOpticPare2 : AnalogInput

    private lateinit var _mobileOutOpticPare1 : AnalogInput
    private lateinit var _mobileOutOpticPare2 : AnalogInput


    private lateinit var _intakeColorSensor1 : AdafruitI2cColorSensor
    private lateinit var _intakeColorSensor2 : AdafruitI2cColorSensor


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
        _bottomOpticPare1 = hardwareMap.get("bottom optic-pare 1") as AnalogInput
        _bottomOpticPare2 = hardwareMap.get("bottom optic-pare 2") as AnalogInput

        _mobileOutOpticPare1 = hardwareMap.get("mobile-out optic-pare 1") as AnalogInput
        _mobileOutOpticPare2 = hardwareMap.get("mobile-out optic-pare 2") as AnalogInput

        _intakeColorSensor1 = fixSensor(hardwareMap.get("intake color 1") as AdafruitI2cColorSensor)
        _intakeColorSensor2 = fixSensor(hardwareMap.get("intake color 2") as AdafruitI2cColorSensor)



        _beltMotor = hardwareMap.get(_deviceName) as DcMotorEx

        _beltMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _beltMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _beltMotor.direction = SORTING_BELT_MOTOR_DIRECTION
    }
}