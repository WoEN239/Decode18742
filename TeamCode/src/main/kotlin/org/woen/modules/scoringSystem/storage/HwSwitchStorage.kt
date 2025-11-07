package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Ball
import woen239.FixColorSensor.fixSensor

import kotlin.math.min
import java.util.concurrent.atomic.AtomicReference

import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor

import org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion

import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.TURRET_GATE_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_1
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_2

import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_MAX_C_RED
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_MIN_C_GREEN
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_MAX_C_BLUE

import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_MIN_C_RED
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_MAX_C_GREEN
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_MIN_C_BLUE



class HwSwitchStorage : IHardwareDevice
{
    private lateinit var _turretGateServo : Servo
    private var _gatePosition = AtomicReference(TURRET_GATE_SERVO_CLOSE_VALUE)


    private lateinit var _intakeColorSensor1 : AdafruitI2cColorSensor
    private lateinit var _intakeColorSensor2 : AdafruitI2cColorSensor



    override fun init(hardwareMap: HardwareMap)
    {
        _intakeColorSensor1 = fixSensor(
            BlocksOpModeCompanion.hardwareMap.get(INTAKE_COLOR_SENSOR_1)
                    as AdafruitI2cColorSensor)
        _intakeColorSensor2 = fixSensor(
            BlocksOpModeCompanion.hardwareMap.get(INTAKE_COLOR_SENSOR_2)
                    as AdafruitI2cColorSensor)


        _turretGateServo = hardwareMap.get(TURRET_GATE_SERVO) as Servo

        closeGate()
    }
    override fun update()
    {
        val r1 = _intakeColorSensor1.red()
        val g1 = _intakeColorSensor1.green()
        val b1 = _intakeColorSensor1.blue()

        val r2 = _intakeColorSensor2.red()
        val g2 = _intakeColorSensor2.green()
        val b2 = _intakeColorSensor2.blue()


        if (r1 < THRESHOLD_GREEN_MAX_C_RED &&
            g1 > THRESHOLD_GREEN_MIN_C_GREEN &&
            b1 < THRESHOLD_GREEN_MAX_C_BLUE
            ||
            r2 < THRESHOLD_GREEN_MAX_C_RED &&
            g2 > THRESHOLD_GREEN_MIN_C_GREEN &&
            b2 < THRESHOLD_GREEN_MAX_C_BLUE)
        {
            ColorSensorsSeeIntakeIncoming(Ball.Name.GREEN)
        }
        else if (r1 > THRESHOLD_PURPLE_MIN_C_RED &&
                 g1 < THRESHOLD_PURPLE_MAX_C_GREEN &&
                 b1 > THRESHOLD_PURPLE_MIN_C_BLUE
                 ||
                 r2 > THRESHOLD_PURPLE_MIN_C_RED &&
                 g2 < THRESHOLD_PURPLE_MAX_C_GREEN &&
                 b2 > THRESHOLD_PURPLE_MIN_C_BLUE)
        {
            ColorSensorsSeeIntakeIncoming(Ball.Name.PURPLE)
        }


        _turretGateServo.position = _gatePosition.get()
    }



    fun openGate() = _gatePosition.set(TURRET_GATE_SERVO_OPEN_VALUE)
    fun closeGate() = _gatePosition.set(TURRET_GATE_SERVO_CLOSE_VALUE)



    override fun dispose() { }
}