package org.woen.modules.scoringSystem.storage.sorting.hardware


import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import java.util.concurrent.atomic.AtomicReference

import org.woen.hotRun.HotRun

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.SORTING_STORAGE_BELT_MOTOR_1
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.SORTING_STORAGE_BELT_MOTOR_2

import org.woen.telemetry.Configs.STORAGE.SORTING_STORAGE_BELT_MOTOR_1_DIRECTION
import org.woen.telemetry.Configs.STORAGE.SORTING_STORAGE_BELT_MOTOR_2_DIRECTION



class HwSorting() : IHardwareDevice
{
    private lateinit var _beltMotor1 : DcMotorEx
    private lateinit var _beltMotor2 : DcMotorEx

    private val _motor1Power = AtomicReference(0.0)
    private val _motor2Power = AtomicReference(0.0)



    override fun init(hardwareMap : HardwareMap)
    {
        _beltMotor1 = hardwareMap.get(SORTING_STORAGE_BELT_MOTOR_1) as DcMotorEx
        _beltMotor2 = hardwareMap.get(SORTING_STORAGE_BELT_MOTOR_2) as DcMotorEx


        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _beltMotor1.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            _beltMotor1.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

            _beltMotor2.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            _beltMotor2.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER


            _beltMotor1.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            _beltMotor2.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

            _beltMotor1.direction = SORTING_STORAGE_BELT_MOTOR_1_DIRECTION
            _beltMotor2.direction = SORTING_STORAGE_BELT_MOTOR_2_DIRECTION
        }
    }
    override fun update()
    {
        _beltMotor1.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_motor1Power.get())
        _beltMotor2.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_motor2Power.get())
    }



    fun startBeltMotor()
    {
        _motor1Power.set(12.0)
        _motor2Power.set(12.0)
    }
    fun reverseBeltMotor()
    {
        _motor1Power.set(-12.0)
        _motor2Power.set(-12.0)
    }
    fun stopBeltMotor()
    {
        _motor1Power.set(0.0)
        _motor2Power.set(0.0)
    }



    override fun dispose() { }
}