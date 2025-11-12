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

import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_BALL_MAX_R_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_BALL_MIN_G_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_BALL_MAX_B_S1
//
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_BALL_MAX_R_S2
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_BALL_MIN_G_S2
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_BALL_MAX_B_S2
//------------------------------------------------------------------------------------------
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_BALL_MIN_R_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_BALL_MAX_G_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_BALL_MIN_B_S1
//
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_BALL_MIN_R_S2
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_BALL_MAX_G_S2
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_BALL_MIN_B_S2



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


//        _bottomOpticPare1 = hardwareMap.get(BOTTOM_OPTIC_PARE_1) as AnalogInput
//        _bottomOpticPare2 = hardwareMap.get(BOTTOM_OPTIC_PARE_2) as AnalogInput
//
//        _mobileOutOpticPare1 = hardwareMap.get(MOBILE_OUT_OPTIC_PARE_1) as AnalogInput
//        _mobileOutOpticPare2 = hardwareMap.get(MOBILE_OUT_OPTIC_PARE_2) as AnalogInput
    }

    override fun update()
    {
//        if (_bottomOpticPare1.voltage > OPTIC_PARE_SEES_NOT_BLACK ||
//            _bottomOpticPare2.voltage > OPTIC_PARE_SEES_NOT_BLACK)
//            bottomOpticPareSeesSomethingEvent.invoke(0)
//
//        if (_mobileOutOpticPare1.voltage > OPTIC_PARE_SEES_NOT_BLACK ||
//            _mobileOutOpticPare2.voltage > OPTIC_PARE_SEES_NOT_BLACK)
//            mobileOutOpticPareSeesSomethingEvent.invoke(0)



        val argb1 = _intakeColorSensor1.argb()
        val r1 = (argb1 shr 16) and 0xFF
        val g1 = (argb1 shr  8) and 0xFF
        val b1 =  argb1         and 0xFF

        if (r1 < THRESHOLD_GREEN_BALL_MAX_R_S1 &&
            g1 > THRESHOLD_GREEN_BALL_MIN_G_S1 &&
            b1 < THRESHOLD_GREEN_BALL_MAX_B_S1)
        {
            colorSensorsTriggerAutoIntakeEvent.invoke(Ball.Name.GREEN)

//            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
//                "[!!] - GREEN BALL DETECTED",
//                "ColorSensors")
        }
        else if (r1 > THRESHOLD_PURPLE_BALL_MIN_R_S1 &&
                 g1 < THRESHOLD_PURPLE_BALL_MAX_G_S1 &&
                 b1 > THRESHOLD_PURPLE_BALL_MIN_B_S1)
        {
            colorSensorsTriggerAutoIntakeEvent.invoke(Ball.Name.PURPLE)

//            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
//                "[!!] - PURPLE BALL DETECTED",
//                "ColorSensors")
        }


        val argb2 = _intakeColorSensor2.argb()
        val r2 = (argb2 shr 16) and 0xFF
        val g2 = (argb2 shr 8)  and 0xFF
        val b2 =  argb2         and 0xFF

        if (r2 < THRESHOLD_GREEN_BALL_MAX_R_S2 &&
            g2 > THRESHOLD_GREEN_BALL_MIN_G_S2 &&
            b2 < THRESHOLD_GREEN_BALL_MAX_B_S2)
        {
            colorSensorsTriggerAutoIntakeEvent.invoke(Ball.Name.GREEN)

//            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
//                "[!!] - GREEN BALL DETECTED",
//                "ColorSensors")
        }
        else if (r2 > THRESHOLD_PURPLE_BALL_MIN_R_S2 &&
                 g2 < THRESHOLD_PURPLE_BALL_MAX_G_S2 &&
                 b2 > THRESHOLD_PURPLE_BALL_MIN_B_S2)
        {
            colorSensorsTriggerAutoIntakeEvent.invoke(Ball.Name.PURPLE)

//            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
//                "[!!] - PURPLE BALL DETECTED",
//                "ColorSensors")
        }


//        ThreadedTelemetry.LAZY_INSTANCE.logWithTag("---  UPDATED COLORS  ---", "ColorSensors")
//        ThreadedTelemetry.LAZY_INSTANCE.logWithTag("r1 = $r1, g1 = $g1, b1 = $b1", "ColorSensors")
//        ThreadedTelemetry.LAZY_INSTANCE.logWithTag("r2 = $r2, g2 = $g2, b2 = $b2", "ColorSensors")
    }



    override fun dispose() { }
}

//*/