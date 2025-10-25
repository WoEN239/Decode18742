package org.woen.modules.storage.stream


import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import org.woen.telemetry.Configs.STORAGE.STREAM_MOTOR_DIRECTION



class HwStreamStorage(private val _deviceName: String) : IHardwareDevice
{
    private lateinit var _motor : DcMotorEx


    fun start()
    {
        _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(12.0)
    }
    fun stop()
    {
        _motor.power = 0.0
    }



    override fun update() { }

    override fun dispose() { }



    override fun init(hardwareMap : HardwareMap)
    {
        _motor = hardwareMap.get(_deviceName) as DcMotorEx

        _motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(12.0)

        _motor.direction = STREAM_MOTOR_DIRECTION
    }
}