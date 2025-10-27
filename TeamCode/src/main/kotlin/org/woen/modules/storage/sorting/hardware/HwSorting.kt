package org.woen.modules.storage.sorting.hardware


import barrel.enumerators.RunStatus
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import org.woen.telemetry.Configs.STORAGE.SORTING_BELT_MOTOR_DIRECTION



class HwSorting (private val _deviceName: String) : IHardwareDevice
{
    private lateinit var _beltMotor : DcMotorEx
    private val _runStatus = RunStatus()



    fun safeStart(): Boolean
    {
        val startCondition = _runStatus.IsInactive()
        if (startCondition)
        {
            _runStatus.SetActive()
            _beltMotor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(12.0)
        }

        return startCondition
    }
    fun safeStop(): Boolean
    {
        val stopCondition = _runStatus.IsActive()
        if (stopCondition)
        {
            _runStatus.SetInactive()
            _beltMotor.power = 0.0
        }

        return stopCondition
    }
    fun forceStop()
    {
        _runStatus.SetInactive()
        _beltMotor.power = 0.0
    }

    fun safePause(): Boolean
    {
        val pauseCondition = _runStatus.IsActive()
        if (pauseCondition)
        {
            _runStatus.Set(
                RunStatus.USED_BY_ANOTHER_PROCESS,
                RunStatus.Name.USED_BY_ANOTHER_PROCESS
            )
            _beltMotor.power = 0.0
        }

        return pauseCondition
    }
    fun safeResume(): Boolean
    {
        val resumeCondition = _runStatus.IsUsedByAnotherProcess()  //  NOT INACTIVE
        if (resumeCondition)
        {
            _runStatus.SetActive()
            _beltMotor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(12.0)
        }

        return resumeCondition
    }



    override fun update() { }

    override fun dispose() { }

    override fun init(hardwareMap : HardwareMap)
    {
        _beltMotor = hardwareMap.get(_deviceName) as DcMotorEx

        _beltMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _beltMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _beltMotor.direction = SORTING_BELT_MOTOR_DIRECTION
    }
}