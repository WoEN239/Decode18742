package org.woen.utils.process


import org.woen.telemetry.Configs
import org.woen.telemetry.Configs.GENERIC.NOTHING



class RunStatus
{
    private var _prioritySetting: Priority = Priority.PRIORITIZE_HIGH_PROCESS_ID

    private var _currentActiveProcessId = Configs.PROCESS_ID.UNDEFINED_PROCESS_ID
    private val _processQueue    = ArrayList<Int>()
    private val _terminationList = ArrayList<Int>()


    constructor(prioritySetting: Priority = Priority.PRIORITIZE_HIGH_PROCESS_ID)
    { _prioritySetting = prioritySetting }


    enum class Priority
    {
        PRIORITIZE_LOW_PROCESS_ID,
        PRIORITIZE_HIGH_PROCESS_ID,
        USE_CUSTOM_PRIORITY
    }


    fun fullResetToActiveState()
    {
        clearAllProcesses()
        clearAllTermination()
        clearCurrentActiveProcess()
    }


    fun getCurrentActiveProcess() = _currentActiveProcessId
    fun clearCurrentActiveProcess()
    {
        _currentActiveProcessId = Configs.PROCESS_ID.UNDEFINED_PROCESS_ID
    }
    fun setCurrentActiveProcess(processId: Int)
    {
        _currentActiveProcessId = processId
    }


    fun isNotBusy()          =  _processQueue.isEmpty()
    fun isUsedByAnyProcess() = !_processQueue.isEmpty()
    fun isUsedByAnotherProcess(ourProcessId: Int): Boolean
    {
        for (curProcess in _processQueue)
            if (curProcess != ourProcessId) return false

        return true
    }


    fun addProcessToQueue(processId: Int) = _processQueue.add(processId)
    fun clearAllProcesses() = _processQueue.clear()
    fun safeRemoveThisProcessIdFromQueue(processId: Int)
    {
        do
        {
            val position  =     _processQueue.indexOf(processId)
            if (position != -1) _processQueue.removeAt(position)
        }
        while (position != -1)
    }


    fun isThisProcessHighestPriority(processId: Int): Boolean
    {
        var maxId = NOTHING
        var minId = Int.MAX_VALUE
        var containsRequestedId = false

        for (curProcess in _processQueue)
        {
            if (curProcess == processId) containsRequestedId = true

            if (curProcess > maxId) maxId = curProcess
            if (curProcess < minId) minId = curProcess
        }

        return containsRequestedId &&
                (
                    isPrioritizingLowId()  && processId <= minId
                    ||
                    isPrioritizingHighId() && processId >= maxId
                )
    }
    fun getHighestPriorityProcessId(): Int
    {
        var maxId = NOTHING
        var minId = Int.MAX_VALUE

        for (curProcess in _processQueue)
        {
            if (curProcess > maxId) maxId = curProcess
            if (curProcess < minId) minId = curProcess
        }
        return if (isPrioritizingHighId()) maxId else minId
    }


    fun isForcedToTerminateThisProcess(processId: Int) = _terminationList.contains(processId)
    fun addProcessToTerminationList(processId: Int)
    {
        safeRemoveThisProcessIdFromQueue(processId)
        _terminationList.add(processId)
    }
    fun safeRemoveThisProcessFromTerminationList(processId: Int)
    {
        do
        {
            val position = _terminationList.indexOf(processId)
            if (position != -1) _terminationList.removeAt(position)
        }
        while (position != -1)
    }

    fun clearAllTermination() = _terminationList.clear()


    fun isPrioritizingLowId()  = _prioritySetting == Priority.PRIORITIZE_LOW_PROCESS_ID
    fun isPrioritizingHighId() = _prioritySetting == Priority.PRIORITIZE_HIGH_PROCESS_ID
    fun changePrioritySetting(newPrioritySetting: Priority)
    {
        _prioritySetting = newPrioritySetting
    }
}