package org.woen.modules.scoringSystem.storage.sorting.hardware


//*

import woen239.FixColorSensor.fixSensor
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor

import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.threading.hardware.IHardwareDevice

import org.woen.enumerators.Ball
import org.woen.telemetry.Configs.DEBUG_LEVELS.HARDWARE
import org.woen.telemetry.Configs.DEBUG_LEVELS.HARDWARE_LOW
import org.woen.telemetry.Configs.DEBUG_LEVELS.SENSORS_DEBUG_LEVELS
import org.woen.telemetry.Configs.DEBUG_LEVELS.SENSORS_DEBUG_SETTING

import org.woen.telemetry.LogManager
import org.woen.utils.events.SimpleEvent
import org.woen.utils.events.SimpleEmptyEvent

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.TURRET_OPTIC_1
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.TURRET_OPTIC_2

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_L
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_R

import org.woen.telemetry.Configs.STORAGE_SENSORS.VAR_MAXIMUM_READING
import org.woen.telemetry.Configs.STORAGE_SENSORS.OPTIC_SEES_NOT_BLACK

import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_GREEN_BALL_MAX_R_S1
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_GREEN_BALL_MIN_G_S1
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_GREEN_BALL_MAX_B_S1
//
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_GREEN_BALL_MAX_R_S2
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_GREEN_BALL_MIN_G_S2
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_GREEN_BALL_MAX_B_S2
//------------------------------------------------------------------------------------------
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_PURPLE_BALL_MIN_R_S1
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_PURPLE_BALL_MAX_G_S1
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_PURPLE_BALL_MIN_B_S1
//
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_PURPLE_BALL_MIN_R_S2
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_PURPLE_BALL_MAX_G_S2
import org.woen.telemetry.Configs.STORAGE_SENSORS.THRESHOLD_PURPLE_BALL_MIN_B_S2


enum class SensorsId { NOTHING, LEFT, RIGHT }
class ColorSensorsData(var color: Ball.Name, var sensorsId: SensorsId)



class HwSortingSensors(): IHardwareDevice
{
    private lateinit var _intakeColorSensorL : AdafruitI2cColorSensor
    private lateinit var _intakeColorSensorR : AdafruitI2cColorSensor

    private lateinit var _turretOptic1 : AnalogInput
    private lateinit var _turretOptic2 : AnalogInput
    
    val logM = LogManager(SENSORS_DEBUG_SETTING, 
        SENSORS_DEBUG_LEVELS, "SENSORS")


    val opticDetectedShotFiringEvent    = SimpleEmptyEvent()
    val colorSensorsDetectedIntakeEvent = SimpleEvent<ColorSensorsData>()



    override fun init(hardwareMap : HardwareMap)
    {
        _intakeColorSensorL = fixSensor(
            hardwareMap.get(INTAKE_COLOR_SENSOR_L)
                    as AdafruitI2cColorSensor)
        _intakeColorSensorR = fixSensor(
            hardwareMap.get(INTAKE_COLOR_SENSOR_R)
                    as AdafruitI2cColorSensor)


        _turretOptic1 = hardwareMap.get(TURRET_OPTIC_1) as AnalogInput
        _turretOptic2 = hardwareMap.get(TURRET_OPTIC_2) as AnalogInput
    }



    override fun update()
    {
        if (_turretOptic1.voltage > OPTIC_SEES_NOT_BLACK ||
            _turretOptic2.voltage > OPTIC_SEES_NOT_BLACK)
            opticDetectedShotFiringEvent.invoke()


        val argb1 = _intakeColorSensorL.normalizedColors
        val r1 = argb1.red   * VAR_MAXIMUM_READING
        val g1 = argb1.green * VAR_MAXIMUM_READING
        val b1 = argb1.blue  * VAR_MAXIMUM_READING

        if (r1 < THRESHOLD_GREEN_BALL_MAX_R_S1 &&
            g1 > THRESHOLD_GREEN_BALL_MIN_G_S1 &&
            b1 < THRESHOLD_GREEN_BALL_MAX_B_S1)
        {
            colorSensorsDetectedIntakeEvent.invoke(
                ColorSensorsData(Ball.Name.GREEN, SensorsId.LEFT))

            logM.logTag(
                "GREEN BALL DETECTED",
                "StorageSensors", HARDWARE)
        }
        else if (r1 > THRESHOLD_PURPLE_BALL_MIN_R_S1 &&
                 g1 < THRESHOLD_PURPLE_BALL_MAX_G_S1 &&
                 b1 > THRESHOLD_PURPLE_BALL_MIN_B_S1)
        {
            colorSensorsDetectedIntakeEvent.invoke(
                ColorSensorsData(Ball.Name.PURPLE, SensorsId.LEFT))

            logM.logTag(
                "[!!] - PURPLE BALL DETECTED",
                "StorageSensors", HARDWARE)
        }
        else colorSensorsDetectedIntakeEvent(
            ColorSensorsData(Ball.Name.NONE, SensorsId.LEFT))


        val argb2 = _intakeColorSensorR.normalizedColors
        val r2 = argb2.red   * VAR_MAXIMUM_READING
        val g2 = argb2.green * VAR_MAXIMUM_READING
        val b2 = argb2.blue  * VAR_MAXIMUM_READING

        if (r2 < THRESHOLD_GREEN_BALL_MAX_R_S2 &&
            g2 > THRESHOLD_GREEN_BALL_MIN_G_S2 &&
            b2 < THRESHOLD_GREEN_BALL_MAX_B_S2)
        {
            colorSensorsDetectedIntakeEvent.invoke(
                ColorSensorsData(Ball.Name.GREEN, SensorsId.RIGHT))

            logM.logTag(
                "[!!] - GREEN BALL DETECTED",
                "StorageSensors", HARDWARE)
        }
        else if (r2 > THRESHOLD_PURPLE_BALL_MIN_R_S2 &&
                 g2 < THRESHOLD_PURPLE_BALL_MAX_G_S2 &&
                 b2 > THRESHOLD_PURPLE_BALL_MIN_B_S2)
        {
            colorSensorsDetectedIntakeEvent.invoke(
                ColorSensorsData(Ball.Name.PURPLE, SensorsId.RIGHT))

            logM.logTag(
                "[!!] - PURPLE BALL DETECTED",
                "StorageSensors", HARDWARE)
        }
        else colorSensorsDetectedIntakeEvent(
            ColorSensorsData(Ball.Name.NONE, SensorsId.RIGHT))


//        logM.logTag("---  UPDATED COLORS  ---",     "StorageSensors", HARDWARE_LOW)
//        logM.logTag("r1 = $r1, g1 = $g1, b1 = $b1", "StorageSensors", HARDWARE_LOW)
//        logM.logTag("r2 = $r2, g2 = $g2, b2 = $b2", "StorageSensors", HARDWARE_LOW)
    }



    override fun opModeStart() { }
    override fun opModeStop()  { }

    override fun dispose()     { }
}

//*/