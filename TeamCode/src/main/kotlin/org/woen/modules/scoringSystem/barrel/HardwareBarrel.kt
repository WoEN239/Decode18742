package org.woen.modules.scoringSystem.barrel

import org.woen.utils.regulator.Regulator;
import org.woen.telemetry.Configs

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple

import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.threading.hardware.IHardwareDevice



class HardwareBarrel(private val _deviceName: String) : IHardwareDevice
{
    private var _regulator = Regulator(Configs.BARREL.BARREL_REGULATOR_PARAMETERS);
    var targetPos : Double = 0.0;
    private var _currentPos : Double = 0.0;
    private lateinit var _motor : DcMotorEx;



    override fun update()
    {
        _motor.power = _regulator.update(targetPos - _currentPos)
    }

    override fun init(hardwareMap : HardwareMap)
    {
        _motor = hardwareMap.get(_deviceName) as DcMotorEx;

        _motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

//        _motor.direction = DcMotorSimple.Direction.REVERSE
    }

    override fun dispose() { }


    fun Rotate(deltaDegrees : Double)
    {
        targetPos += deltaDegrees;
    }
}