package org.woen.modules.scoringSystem.storage.sorting.hardware


import android.icu.text.RelativeDateTimeFormatter
import kotlinx.coroutines.delay

import woen239.enumerators.RunStatus

import org.woen.threading.hardware.HardwareThreads
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.ThreadedBattery


class HwSortingManager
{
    private lateinit var _hwSorting: HwSorting
    private val _runStatus = RunStatus(RunStatus.USED_BY_ANOTHER_PROCESS, RunStatus.Name.USED_BY_ANOTHER_PROCESS)



    constructor()
    {
        linkHardware()
    }



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

        ThreadedTelemetry.LAZY_INSTANCE.log("HW - STOPPED")
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

        ThreadedTelemetry.LAZY_INSTANCE.log("HW - STARTED")
    }
    fun safeReverse(): Boolean
    {
        if (_runStatus.IsActive()) return true  //  Already active

        val resumeCondition = _runStatus.IsUsedByAnotherProcess()  //  NOT INACTIVE
        if (resumeCondition)
        {
            _runStatus.SetActive()
            _hwSorting.reverseBeltMotor()
        }

        return resumeCondition
    }
    suspend fun forceSafeReverse()
    {
        while (!safeReverse())
            delay(DELAY_FOR_EVENT_AWAITING_MS)

        ThreadedTelemetry.LAZY_INSTANCE.log("HW - STARTED")
    }



    fun addDevice() = HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSorting)
    fun linkHardware()
    {
        _hwSorting = HwSorting()
    }
}