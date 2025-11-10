package org.woen.modules.scoringSystem.storage.sorting.hardware

/*
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor

import woen239.enumerators.Ball

import woen239.FixColorSensor.fixSensor

import org.woen.utils.events.SimpleEvent
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_1
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_2

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.BOTTOM_OPTIC_PARE_1
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.BOTTOM_OPTIC_PARE_2
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_OUT_OPTIC_PARE_1
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_OUT_OPTIC_PARE_2

import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.OPTIC_PARE_SEES_NOT_BLACK
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_MAX_C_BLUE
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_MAX_C_RED
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_MIN_C_GREEN
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_MIN_EXTRA_ALPHA
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_MAX_C_GREEN
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_MIN_C_BLUE
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_MIN_C_RED
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_MIN_EXTRA_ALPHA



class HwSortingSensors(): IHardwareDevice
{
    private lateinit var _intakeColorSensor1 : AdafruitI2cColorSensor
    private lateinit var _intakeColorSensor2 : AdafruitI2cColorSensor

    private lateinit var _bottomOpticPare1 : AnalogInput
    private lateinit var _bottomOpticPare2 : AnalogInput

    private lateinit var _mobileOutOpticPare1 : AnalogInput
    private lateinit var _mobileOutOpticPare2 : AnalogInput


    val colorSensorsTriggerAutoIntakeEvent   = SimpleEvent<Ball.Name>()
    val bottomOpticPareSeesSomethingEvent    = SimpleEvent<Int>()
    val mobileOutOpticPareSeesSomethingEvent = SimpleEvent<Int>()



    override fun init(hardwareMap : HardwareMap)
    {
        _intakeColorSensor1 = fixSensor(
            hardwareMap.get(INTAKE_COLOR_SENSOR_1)
                    as AdafruitI2cColorSensor)
        _intakeColorSensor2 = fixSensor(
            hardwareMap.get(INTAKE_COLOR_SENSOR_2)
                    as AdafruitI2cColorSensor)


        _bottomOpticPare1 = hardwareMap.get(BOTTOM_OPTIC_PARE_1) as AnalogInput
        _bottomOpticPare2 = hardwareMap.get(BOTTOM_OPTIC_PARE_2) as AnalogInput

        _mobileOutOpticPare1 = hardwareMap.get(MOBILE_OUT_OPTIC_PARE_1) as AnalogInput
        _mobileOutOpticPare2 = hardwareMap.get(MOBILE_OUT_OPTIC_PARE_2) as AnalogInput
    }

    override fun update()
    {
        if (_bottomOpticPare1.voltage > OPTIC_PARE_SEES_NOT_BLACK ||
            _bottomOpticPare2.voltage > OPTIC_PARE_SEES_NOT_BLACK)
            bottomOpticPareSeesSomethingEvent.invoke(0)

        if (_mobileOutOpticPare1.voltage > OPTIC_PARE_SEES_NOT_BLACK ||
            _mobileOutOpticPare2.voltage > OPTIC_PARE_SEES_NOT_BLACK)
            mobileOutOpticPareSeesSomethingEvent.invoke(0)



        val r1 = _intakeColorSensor1.red()
        val g1 = _intakeColorSensor1.green()
        val b1 = _intakeColorSensor1.blue()
        val a1 = _intakeColorSensor1.alpha()

        val r2 = _intakeColorSensor2.red()
        val g2 = _intakeColorSensor2.green()
        val b2 = _intakeColorSensor2.blue()
        val a2 = _intakeColorSensor2.alpha()


        if (r1 < THRESHOLD_GREEN_MAX_C_RED &&
            g1 > THRESHOLD_GREEN_MIN_C_GREEN &&
            b1 < THRESHOLD_GREEN_MAX_C_BLUE
            ||
            r2 < THRESHOLD_GREEN_MAX_C_RED &&
            g2 > THRESHOLD_GREEN_MIN_C_GREEN &&
            b2 < THRESHOLD_GREEN_MAX_C_BLUE
            ||
            a1 > THRESHOLD_GREEN_MIN_EXTRA_ALPHA
            && g1 > r1
            ||
            a2 > THRESHOLD_GREEN_MIN_EXTRA_ALPHA
            && g2 > r2)
        {
            colorSensorsTriggerAutoIntakeEvent.invoke(Ball.Name.GREEN)

            ThreadedTelemetry.LAZY_INSTANCE.logWithTag("!!! GREEN BALL DETECTED", "IntakeColorSensors")
        }
        else if (r1 > THRESHOLD_PURPLE_MIN_C_RED &&
                 g1 < THRESHOLD_PURPLE_MAX_C_GREEN &&
                 b1 > THRESHOLD_PURPLE_MIN_C_BLUE
                 ||
                 r2 > THRESHOLD_PURPLE_MIN_C_RED &&
                 g2 < THRESHOLD_PURPLE_MAX_C_GREEN &&
                 b2 > THRESHOLD_PURPLE_MIN_C_BLUE
                 ||
                 a1 > THRESHOLD_PURPLE_MIN_EXTRA_ALPHA
                 && g1 > r1
                 ||
                 a2 > THRESHOLD_PURPLE_MIN_EXTRA_ALPHA
                 && g2 > r2)
        {
            colorSensorsTriggerAutoIntakeEvent.invoke(Ball.Name.PURPLE)

            ThreadedTelemetry.LAZY_INSTANCE.logWithTag("!!! PURPLE BALL DETECTED", "IntakeColorSensors")
        }

        ThreadedTelemetry.LAZY_INSTANCE.logWithTag("---  UPDATED COLORS  ---", "IntakeColorSensors")
        ThreadedTelemetry.LAZY_INSTANCE.logWithTag("r1 = $r1, g1 = $g1, b1 = $b1", "IntakeColorSensors")
        ThreadedTelemetry.LAZY_INSTANCE.logWithTag("r2 = $r2, g2 = $g2, b2 = $b2", "IntakeColorSensors")

    }



    override fun dispose() { }
}
*/