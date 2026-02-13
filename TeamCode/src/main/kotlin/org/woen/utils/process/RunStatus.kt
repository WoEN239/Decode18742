package org.woen.utils.process


import kotlin.math.abs
import java.util.concurrent.CopyOnWriteArrayList

import org.woen.telemetry.configs.ProcessId



class RunStatus
{
    private var _prioritySetting = Priority.PRIORITIZE_HIGH_PROCESS_ID

    private val _processQueue    = CopyOnWriteArrayList<Int>()
    private val _terminations    = CopyOnWriteArrayList<Int>()
    private var _activeProcessId = ProcessId.UNDEFINED_PROCESS_ID



    enum class Priority
    {
        PRIORITIZE_LOW_PROCESS_ID,
        PRIORITIZE_HIGH_PROCESS_ID,

        PRIORITIZE_CLOSER_TO_ZERO_FIRST_POSITIVE,
        PRIORITIZE_CLOSER_TO_ZERO_FIRST_NEGATIVE,

        PRIORITIZE_FARTHER_FROM_ZERO_FIRST_POSITIVE,
        PRIORITIZE_FARTHER_FROM_ZERO_FIRST_NEGATIVE,
    }



    constructor(prioritySetting: Priority = Priority.PRIORITIZE_HIGH_PROCESS_ID)
    {
        _prioritySetting = prioritySetting
        fullResetToActiveState()
        changePrioritySetting(prioritySetting)
    }



    fun fullResetToActiveState()
    {
        clearProcessQueue()
        clearTerminations()
        clearActiveProcess()
    }



    fun getActiveProcess() = _activeProcessId
    fun clearActiveProcess()
    {
        _activeProcessId = ProcessId.UNDEFINED_PROCESS_ID
    }
    fun setActiveProcess(processId: Int)
    {
        _activeProcessId = processId
    }



    fun isNotBusy()          =  _processQueue.isEmpty()
    fun isUsedByAnyProcess() = !_processQueue.isEmpty()
    fun isUsedByThisProcess(processId: Int)
        = _processQueue.contains(processId)
    fun isUsedByAnotherProcess(vararg exceptionProcessesId: Int): Boolean
        = _processQueue.any { it !in exceptionProcessesId }
    fun countOfThisProcess(processId: Int)
        = _processQueue.count { it == processId }



    fun addProcessToQueue(processId: Int) = _processQueue.add(processId)
    fun clearProcessQueue() = _processQueue.clear()
    fun removeProcessFromQueue(processId: Int)
        = _processQueue.removeAll { it == processId }
    fun removeOneInstanceOfProcessFromQueue(processId: Int)
        = _processQueue.remove(processId)



    fun isThisProcessHighestPriority(processId: Int,
        vararg exceptionProcessesId: Int): Boolean
    {
        if (_processQueue.isEmpty()) return false

        var maxId = Int.MIN_VALUE
        var minId = Int.MAX_VALUE
        var zeroPositive = Int.MAX_VALUE
        var zeroNegative = Int.MIN_VALUE
        var containsRequestedId = false

        for (curProcess in _processQueue)
        {
            if (curProcess == processId) containsRequestedId = true

            if (!exceptionProcessesId.contains(curProcess))
            {
                if (curProcess > maxId) maxId = curProcess
                if (curProcess < minId) minId = curProcess

                if (curProcess > 0 && curProcess < zeroPositive)
                    zeroPositive = curProcess
                if (curProcess < 0 && curProcess > zeroNegative)
                    zeroNegative = curProcess
            }
        }

        return containsRequestedId &&
                processId == returnPrioritized(maxId, minId, zeroPositive, zeroNegative)
    }
    fun getHighestPriorityProcess(vararg exceptionProcessesId: Int): Int
    {
        if (_processQueue.isEmpty()) return ProcessId.UNDEFINED_PROCESS_ID

        var maxId = Int.MIN_VALUE
        var minId = Int.MAX_VALUE
        var zeroPositive = Int.MAX_VALUE
        var zeroNegative = Int.MIN_VALUE

        for (curProcess in _processQueue)
        {
            if (!exceptionProcessesId.contains(curProcess))
            {
                if (curProcess > maxId) maxId = curProcess
                if (curProcess < minId) minId = curProcess

                if (curProcess > 0 && curProcess < zeroPositive)
                    zeroPositive = curProcess
                if (curProcess < 0 && curProcess > zeroNegative)
                    zeroNegative = curProcess
            }
        }
        return returnPrioritized(maxId, minId, zeroPositive, zeroNegative)
    }
    private fun returnPrioritized(
        maxId: Int, minId: Int,
        zeroPositive: Int, zeroNegative: Int): Int
    {
        return when (_prioritySetting)
        {
            Priority.PRIORITIZE_LOW_PROCESS_ID  -> minId
            Priority.PRIORITIZE_HIGH_PROCESS_ID -> maxId

            Priority.PRIORITIZE_CLOSER_TO_ZERO_FIRST_POSITIVE ->
                if  (zeroPositive <= abs(zeroNegative))
                    zeroPositive
                else zeroNegative
            Priority.PRIORITIZE_CLOSER_TO_ZERO_FIRST_NEGATIVE ->
                if  (abs(zeroNegative) <= zeroPositive)
                    zeroNegative
                else zeroPositive

            Priority.PRIORITIZE_FARTHER_FROM_ZERO_FIRST_POSITIVE ->
                if  (maxId >= abs(minId)) maxId
                else minId
            Priority.PRIORITIZE_FARTHER_FROM_ZERO_FIRST_NEGATIVE ->
                if  (abs(minId) >= maxId) minId
                else maxId
        }
    }



    fun isForcedToTerminateThisProcess(processId: Int)
        = _terminations.contains(processId)
    fun addProcessToTermination(processId: Int)
        = _terminations.add(processId)

    fun removeProcessFromTermination(vararg processId: Int)
        = _terminations.removeAll { it in processId }

    fun clearTerminations() = _terminations.clear()


    fun changePrioritySetting(newPrioritySetting: Priority)
    {
        _prioritySetting = newPrioritySetting
    }
}