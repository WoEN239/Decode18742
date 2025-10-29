package org.woen.modules.scoringSystem.storage.sorting.hardware


import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import java.util.concurrent.atomic.AtomicReference

import org.woen.telemetry.Configs.STORAGE.SORTING_BELT_MOTOR_DIRECTION



class HwSorting (private val _deviceName: String) : IHardwareDevice
{
    private lateinit var _beltMotor : DcMotorEx
    private val _motorPower = AtomicReference(0.0)



    fun startBeltMotor()
    {
        _motorPower.set(12.0)
    }
    fun stopBeltMotor()
    {
        _motorPower.set(0.0)
    }



    override fun dispose() { }

    override fun update()
    {
        _beltMotor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_motorPower.get())
    }

    override fun init(hardwareMap : HardwareMap)
    {
        _beltMotor = hardwareMap.get(_deviceName) as DcMotorEx

        _beltMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _beltMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _beltMotor.direction = SORTING_BELT_MOTOR_DIRECTION
    }
}