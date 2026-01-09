package org.woen.modules.scoringSystem.storage.sorting.hardware


//*
import kotlin.math.max

import woen239.FixColorSensor.fixSensor
import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor
import com.qualcomm.hardware.rev.RevColorSensorV3

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

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_L
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_R

import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_GREEN_BALL_B_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_GREEN_BALL_G_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_GREEN_BALL_R_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_GREEN_BALL_THRESHOLD

import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_PURPLE_BALL_B_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_PURPLE_BALL_G_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_PURPLE_BALL_R_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_PURPLE_BALL_THRESHOLD

import org.woen.telemetry.Configs.STORAGE_SENSORS.RCS_GREEN_BALL_B_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.RCS_GREEN_BALL_G_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.RCS_GREEN_BALL_R_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.RCS_GREEN_BALL_THRESHOLD

import org.woen.telemetry.Configs.STORAGE_SENSORS.RCS_PURPLE_BALL_B_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.RCS_PURPLE_BALL_G_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.RCS_PURPLE_BALL_R_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.RCS_PURPLE_BALL_THRESHOLD

import org.woen.telemetry.Configs.STORAGE_SENSORS.CONST_MAXIMUM_READING



enum class SensorsId { NOTHING, LEFT, RIGHT }
class ColorSensorsData(var color: Ball.Name, var sensorsId: SensorsId)



class HwSortingSensors(): IHardwareDevice
{
//    private lateinit var _intakeColorSensorL : AdafruitI2cColorSensor
    private lateinit var _intakeColorSensorR : RevColorSensorV3

//    private lateinit var _turretOptic1 : AnalogInput
//    private lateinit var _turretOptic2 : AnalogInput
    
    val logM = LogManager(SENSORS_DEBUG_SETTING, 
        SENSORS_DEBUG_LEVELS, "SENSORS")


    val opticDetectedShotFiringEvent    = SimpleEmptyEvent()
    val colorSensorsDetectedIntakeEvent = SimpleEvent<ColorSensorsData>()



    override fun init(hardwareMap : HardwareMap)
    {
//        _intakeColorSensorL = fixSensor(
//            hardwareMap.get(INTAKE_COLOR_SENSOR_L)
//                    as AdafruitI2cColorSensor)
        _intakeColorSensorR = hardwareMap.get(INTAKE_COLOR_SENSOR_R)
                    as RevColorSensorV3


//        _turretOptic1 = hardwareMap.get(TURRET_OPTIC_1) as AnalogInput
//        _turretOptic2 = hardwareMap.get(TURRET_OPTIC_2) as AnalogInput
    }



    override fun update()
    {
//        if (_turretOptic1.voltage > OPTIC_SEES_NOT_BLACK ||
//            _turretOptic2.voltage > OPTIC_SEES_NOT_BLACK)
//            opticDetectedShotFiringEvent.invoke()


//        testLCS()
        testRCS()
    }

//    private fun testLCS()
//    {
//        val argb1 = _intakeColorSensorL.normalizedColors
//        val r1 = argb1.red   * CONST_MAXIMUM_READING
//        val g1 = argb1.green * CONST_MAXIMUM_READING
//        val b1 = argb1.blue  * CONST_MAXIMUM_READING
//
//        //(r * kr + b * kb + g * kg) - max(r * (1 - kr), b * (1 - kb), g * (1 - kg)) > n
//
//        if (
//            (LCS_GREEN_BALL_R_K * r1 + LCS_GREEN_BALL_B_K * b1 + LCS_GREEN_BALL_G_K * g1) -
//            max(r1 * (1 - LCS_GREEN_BALL_R_K),
//                max(
//                    b1 * (1 - LCS_GREEN_BALL_B_K),
//                    g1 * (1 - LCS_GREEN_BALL_G_K)
//                )
//            ) > LCS_GREEN_BALL_THRESHOLD)
//        {
//            colorSensorsDetectedIntakeEvent.invoke(
//                ColorSensorsData(Ball.Name.GREEN, SensorsId.LEFT))
//
//            logM.logTag(
//                "GREEN BALL DETECTED",
//                "StorageSensors", HARDWARE)
//        }
//        else if (
//            (LCS_PURPLE_BALL_R_K * r1 + LCS_PURPLE_BALL_B_K * b1 + LCS_PURPLE_BALL_G_K * g1) -
//            max(r1 * (1 - LCS_PURPLE_BALL_R_K),
//                max(
//                    b1 * (1 - LCS_PURPLE_BALL_B_K),
//                    g1 * (1 - LCS_PURPLE_BALL_G_K)
//                )
//            ) > LCS_PURPLE_BALL_THRESHOLD)
//        {
//            colorSensorsDetectedIntakeEvent.invoke(
//                ColorSensorsData(Ball.Name.PURPLE, SensorsId.LEFT))
//
//            logM.logTag(
//                "[!!] - PURPLE BALL DETECTED",
//                "StorageSensors", HARDWARE)
//        }
//        else colorSensorsDetectedIntakeEvent.invoke(
//            ColorSensorsData(Ball.Name.NONE, SensorsId.LEFT))
//    }
    private fun testRCS()
    {
        val argb2 = _intakeColorSensorR.normalizedColors
        val r2 = argb2.red   * CONST_MAXIMUM_READING
        val g2 = argb2.green * CONST_MAXIMUM_READING
        val b2 = argb2.blue  * CONST_MAXIMUM_READING

        if (
            (RCS_GREEN_BALL_R_K * r2 + RCS_GREEN_BALL_B_K * b2 + RCS_GREEN_BALL_G_K * g2) -
            max(r2 * (1 - RCS_GREEN_BALL_R_K),
                max(
                    b2 * (1 - RCS_GREEN_BALL_B_K),
                    g2 * (1 - RCS_GREEN_BALL_G_K)
                )
            ) > RCS_GREEN_BALL_THRESHOLD)
        {
            colorSensorsDetectedIntakeEvent.invoke(
                ColorSensorsData(Ball.Name.GREEN, SensorsId.RIGHT))

            logM.logTag("GREEN BALL DETECTED",
                "StorageSensors", HARDWARE)
        }
        else if (
            (RCS_PURPLE_BALL_R_K * r2 + RCS_PURPLE_BALL_B_K * b2 + RCS_PURPLE_BALL_G_K * g2) -
            max(r2 * (1 - RCS_PURPLE_BALL_R_K),
                max(
                    b2 * (1 - RCS_PURPLE_BALL_B_K),
                    g2 * (1 - RCS_PURPLE_BALL_G_K)
                )
            ) > RCS_PURPLE_BALL_THRESHOLD)
        {
            colorSensorsDetectedIntakeEvent.invoke(
                ColorSensorsData(Ball.Name.PURPLE, SensorsId.RIGHT))

            logM.logTag(
                "PURPLE BALL DETECTED",
                "StorageSensors", HARDWARE)
        }
        else colorSensorsDetectedIntakeEvent.invoke(
            ColorSensorsData(Ball.Name.NONE, SensorsId.RIGHT))
    }



    override fun opModeStart() { }
    override fun opModeStop()  { }

    override fun dispose()     { }
}

//*/