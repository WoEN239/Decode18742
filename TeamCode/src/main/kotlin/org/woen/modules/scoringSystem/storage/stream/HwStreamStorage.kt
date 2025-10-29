package org.woen.modules.scoringSystem.storage.stream


import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import java.util.concurrent.atomic.AtomicReference

import org.woen.telemetry.Configs.STORAGE.STREAM_MOTOR_DIRECTION



class HwStreamStorage(private val _deviceName: String) : IHardwareDevice
{
    private lateinit var _motor : DcMotorEx
    private var _motorPower = AtomicReference(0.0)



    fun start()
    {
        _motorPower.set(12.0)
    }
    fun stop()
    {
        _motorPower.set(0.0)
    }



    override fun update()
    {
        _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_motorPower.get())
    }



    override fun dispose() { }

    override fun init(hardwareMap : HardwareMap)
    {
        _motor = hardwareMap.get(_deviceName) as DcMotorEx

        _motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _motor.direction = STREAM_MOTOR_DIRECTION
    }
}