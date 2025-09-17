package org.woen.modules.barrel

import org.woen.utils.regulator.Regulator;
import org.woen.telemetry.ThreadedConfigs

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx

import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.threading.hardware.IHardwareDevice



class HardwareBarrel(private val _deviceName: String, private val _direction : Int) : IHardwareDevice
{
    private var _regulator = Regulator(ThreadedConfigs.BARREL_REGULATOR_PARAMETERS);
    var targetPos : Double = 0.0;
    private var _currentPos : Double = 0.0;
    private lateinit var _motor : DcMotorEx;



    override fun update()
    {
        _regulator.update(_direction * (targetPos - _currentPos));
    }

    override fun init(hardwareMap : HardwareMap)
    {
        _motor = hardwareMap.get(_deviceName) as DcMotorEx;

        _motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    }

    override fun dispose() { }


    fun Rotate(deltaDegrees : Double)
    {
        targetPos += deltaDegrees;
    }
}