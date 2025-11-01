package org.woen.modules.scoringSystem.storage.sorting.hardware


import woen239.enumerators.RunStatus
import org.woen.threading.hardware.HardwareThreads

import kotlinx.coroutines.delay
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS



class HwSortingManager()
{
    private lateinit var _hwSorting: HwSorting
    private val _runStatus = RunStatus()



    fun safeStart(): Boolean
    {
        if (_runStatus.IsActive()) return true

        val startCondition = _runStatus.IsInactive()
        if (startCondition)
        {
            _runStatus.SetActive()
            _hwSorting.startBeltMotor()
        }

        return startCondition
    }
    suspend fun forceSafeStart()
    {
        while (!safeStart())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
    }
    fun safeStop(): Boolean
    {
        if (_runStatus.IsInactive()) return true  //  Already stopped

        val stopCondition = _runStatus.IsActive()
        if (stopCondition)
        {
            _runStatus.SetInactive()
            _hwSorting.stopBeltMotor()
        }

        return stopCondition
    }
    fun forceStop()
    {
        _runStatus.SetInactive()
        _hwSorting.stopBeltMotor()
    }
    suspend fun forceSafeStop()
    {
        while (!safeStop())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
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
            _hwSorting.stopBeltMotor()
        }

        return pauseCondition
    }
    suspend fun forceSafePause()
    {
        while (!safePause())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
    }

    fun safeResume(): Boolean
    {
        if (_runStatus.IsActive()) return true  //  Already active

        val resumeCondition = _runStatus.IsUsedByAnotherProcess()  //  NOT INACTIVE
        if (resumeCondition)
        {
            _runStatus.SetActive()
            _hwSorting.startBeltMotor()
        }

        return resumeCondition
    }
    suspend fun forceSafeResume()
    {
        while (!safeResume())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
    }



    fun addDevice()
    {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hwSorting)
    }
    fun linkHardware()
    {
        _hwSorting = HwSorting()
    }
    init
    {
        linkHardware()
    }
}