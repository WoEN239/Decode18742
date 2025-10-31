package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Ball
import woen239.FixColorSensor.fixSensor

import org.woen.threading.hardware.IHardwareDevice

import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.HardwareMap

import org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap
import java.util.concurrent.atomic.AtomicReference


class HwSwitchStorage : IHardwareDevice
{
    private lateinit var _turretGateServo : Servo
    private var _gatePosition = AtomicReference(0.5)


    private val _intakeColorSensor1 : AdafruitI2cColorSensor
        = fixSensor(hardwareMap.get("intake color 1") as AdafruitI2cColorSensor)

    private val _intakeColorSensor2 : AdafruitI2cColorSensor
        = fixSensor(hardwareMap.get("intake color 2") as AdafruitI2cColorSensor)





    override fun update()
    {
        if (_intakeColorSensor1.green() > 200
            ||
            _intakeColorSensor2.green() > 200)
        {
            ColorSensorsSeeIntakeIncoming(Ball.Name.GREEN)
        }


        if (_intakeColorSensor1.red()  > 200 &&
            _intakeColorSensor1.blue() > 200
            ||
            _intakeColorSensor2.red()  > 200 &&
            _intakeColorSensor2.blue() > 200)
        {
            ColorSensorsSeeIntakeIncoming(Ball.Name.PURPLE)
        }


        _turretGateServo.position = _gatePosition.get()
    }

    fun calibrateGateServo()
    {

    }

    fun openGate()
    {
        _gatePosition.set(0.0)
    }
    fun closeGate()
    {
        _gatePosition.set(0.5)
    }



    override fun dispose() { }

    override fun init(hardwareMap: HardwareMap)
    {
        _turretGateServo = hardwareMap.get("") as Servo

        calibrateGateServo()
    }
}