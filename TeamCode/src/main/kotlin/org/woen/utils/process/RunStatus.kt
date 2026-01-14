package org.woen.utils.process


import kotlin.math.abs
import org.woen.telemetry.Configs.PROCESS_ID.UNDEFINED_PROCESS_ID



class RunStatus
{
    private var _prioritySetting = Priority.PRIORITIZE_HIGH_PROCESS_ID

    private val _processQueue    = ArrayList<Int>()
    private val _terminationList = ArrayList<Int>()
    private var _currentActiveProcessId = UNDEFINED_PROCESS_ID


    constructor(prioritySetting: Priority = Priority.PRIORITIZE_HIGH_PROCESS_ID)
    {
        _prioritySetting = prioritySetting
        fullResetToActiveState()
    }


    enum class Priority
    {
        PRIORITIZE_LOW_PROCESS_ID,
        PRIORITIZE_HIGH_PROCESS_ID,

        PRIORITIZE_CLOSER_TO_ZERO_FIRST_POSITIVE,
        PRIORITIZE_CLOSER_TO_ZERO_FIRST_NEGATIVE,

        PRIORITIZE_FARTHER_FROM_ZERO_FIRST_POSITIVE,
        PRIORITIZE_FARTHER_FROM_ZERO_FIRST_NEGATIVE,
    }


    @Synchronized
    fun fullResetToActiveState()
    {
        clearAllProcesses()
        clearAllTermination()
        clearCurrentActiveProcess()
    }


    @Synchronized
    fun getCurrentActiveProcess() = _currentActiveProcessId
    @Synchronized
    fun clearCurrentActiveProcess()
    {
        _currentActiveProcessId = UNDEFINED_PROCESS_ID
    }
    @Synchronized
    fun setCurrentActiveProcess(processId: Int)
    {
        _currentActiveProcessId = processId
    }



    @Synchronized
    fun isNotBusy()          =  _processQueue.isEmpty()
    @Synchronized
    fun isUsedByAnyProcess() = !_processQueue.isEmpty()
    @Synchronized
    fun isUsedByThisProcess(targetProcessId: Int)
            = _processQueue.contains(targetProcessId)
    @Synchronized
    fun isUsedByAnotherProcess(exceptionProcessId: Int)
            = _processQueue.any { it != exceptionProcessId }



    @Synchronized
    fun addProcessToQueue(processId: Int) = _processQueue.add(processId)
    @Synchronized
    fun clearAllProcesses() = _processQueue.clear()
    @Synchronized
    fun safeRemoveThisProcessIdFromQueue(processId: Int)
            = _processQueue.removeAll { it == processId }
    @Synchronized
    fun safeRemoveOnlyOneInstanceOfThisProcessFromQueue(processId: Int)
            = _processQueue.remove(processId)



    @Synchronized
    fun isThisProcessHighestPriority(processId: Int): Boolean
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

            if (curProcess > maxId) maxId = curProcess
            if (curProcess < minId) minId = curProcess

            if (curProcess > 0 && curProcess < zeroPositive)
                zeroPositive = curProcess
            if (curProcess < 0 && curProcess > zeroNegative)
                zeroNegative = curProcess
        }

        return containsRequestedId &&
                processId == returnPrioritized(maxId, minId, zeroPositive, zeroNegative)
    }
    @Synchronized
    fun getHighestPriorityProcessId(): Int
    {
        if (_processQueue.isEmpty()) return UNDEFINED_PROCESS_ID

        var maxId = Int.MIN_VALUE
        var minId = Int.MAX_VALUE
        var zeroPositive = Int.MAX_VALUE
        var zeroNegative = Int.MIN_VALUE

        for (curProcess in _processQueue)
        {
            if (curProcess > maxId) maxId = curProcess
            if (curProcess < minId) minId = curProcess

            if (curProcess > 0 && curProcess < zeroPositive)
                zeroPositive = curProcess
            if (curProcess < 0 && curProcess > zeroNegative)
                zeroNegative = curProcess
        }
        return returnPrioritized(maxId, minId, zeroPositive, zeroNegative)
    }
    @Synchronized
    private fun returnPrioritized(
        maxId: Int, minId: Int, zeroPositive: Int, zeroNegative: Int): Int
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



    @Synchronized
    fun isForcedToTerminateThisProcess(processId: Int) = _terminationList.contains(processId)
    @Synchronized
    fun addProcessToTerminationList   (processId: Int) = _terminationList.add(processId)

    @Synchronized
    fun safeRemoveThisProcessFromTerminationList(processId: Int)
            = _terminationList.removeAll { it == processId }

    @Synchronized
    fun clearAllTermination() = _terminationList.clear()


    fun changePrioritySetting(newPrioritySetting: Priority)
    {
        _prioritySetting = newPrioritySetting
    }
}