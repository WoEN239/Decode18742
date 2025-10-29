package org.woen.modules.scoringSystem.storage.sorting.hardware


import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import org.woen.telemetry.Configs.STORAGE.GATE_MOTOR_DIRECTION
import org.woen.telemetry.Configs.STORAGE.PUSH_MOTOR_DIRECTION



class HwMobileSlot (private val _gateMotorName: String,
                    private val _pushMotorName: String) : IHardwareDevice
{
    private lateinit var _gateMotor : DcMotorEx
    private lateinit var _pushMotor : DcMotorEx



    fun openGate()
    {
        TODO("Rotate gate motor to open state")
    }
    fun closeGate()
    {
        TODO("Rotate gate motor to close state")
    }
    fun startPusher()
    {
        //!  Figure out - rotate 90 deg or by timer
        TODO("Push ball into MOBILE_IN slot")
    }
    fun stopPusher()
    {
        _pushMotor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(0.0)
    }

    fun calibrateGate()
    {

    }
    fun calibratePush()
    {

    }



    override fun update() { }

    override fun dispose() { }



    override fun init(hardwareMap : HardwareMap)
    {
        _gateMotor = hardwareMap.get(_gateMotorName) as DcMotorEx
        _pushMotor = hardwareMap.get(_pushMotorName) as DcMotorEx

        _gateMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        //!  _gateMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _pushMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        //!  _pushMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _gateMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        _pushMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _gateMotor.direction = GATE_MOTOR_DIRECTION
        _pushMotor.direction = PUSH_MOTOR_DIRECTION
    }
}