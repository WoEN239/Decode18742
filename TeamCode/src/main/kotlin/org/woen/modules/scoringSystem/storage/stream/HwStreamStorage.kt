/*package org.woen.modules.scoringSystem.storage.stream


import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import java.util.concurrent.atomic.AtomicReference

import org.woen.telemetry.Configs.STORAGE.STREAM_STORAGE_MOTOR_DIRECTION
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.STREAM_STORAGE_BELT_MOTOR



class HwStreamStorage : IHardwareDevice
{
    private lateinit var _beltMotor : DcMotorEx
    private var _beltMotorPower = AtomicReference(0.0)



    override fun init(hardwareMap : HardwareMap)
    {
        _beltMotor = hardwareMap.get(STREAM_STORAGE_BELT_MOTOR) as DcMotorEx

        _beltMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _beltMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _beltMotor.direction = STREAM_STORAGE_MOTOR_DIRECTION
    }

    override fun update()
    {
        _beltMotor.power = voltageToPowerAlias()
    }



    fun start()
    {
        _beltMotorPower.set(12.0)
    }
    fun stop()
    {
        _beltMotorPower.set(0.0)
    }

    fun voltageToPowerAlias()
        = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_beltMotorPower.get())



    override fun dispose() { }
}*/