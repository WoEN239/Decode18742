package org.woen.modules.storage.sorting.hardware


import barrel.enumerators.RunStatus
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery

import kotlinx.coroutines.delay
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING
import org.woen.telemetry.Configs.STORAGE.SORTING_BELT_MOTOR_DIRECTION



class HwSorting (private val _deviceName: String) : IHardwareDevice
{
    private lateinit var _beltMotor : DcMotorEx
    private val _runStatus = RunStatus()



    fun safeStart(): Boolean
    {
        if (_runStatus.IsActive()) return true

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
        if (_runStatus.IsInactive()) return true  //  Already stopped
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
        if (_runStatus.IsUsedByAnotherProcess()) return true  //  Already paused

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
    suspend fun forceSafePause(): Boolean
    {
        while (!safePause()) delay(DELAY_FOR_EVENT_AWAITING)

        return true
    }

    fun safeResume(): Boolean
    {
        if (_runStatus.IsActive()) return true  //  Already active

        val resumeCondition = _runStatus.IsUsedByAnotherProcess()  //  NOT INACTIVE
        if (resumeCondition)
        {
            _runStatus.SetActive()
            _beltMotor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(12.0)
        }

        return resumeCondition
    }
    suspend fun forceSafeResume(): Boolean
    {
        while (!safeResume()) delay(DELAY_FOR_EVENT_AWAITING)

        return true
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