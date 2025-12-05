package woen239.enumerators;


import java.util.ArrayList;

import static org.woen.telemetry.Configs.PROCESS_ID.UNDEFINED_PROCESS_ID;

import org.woen.telemetry.ThreadedTelemetry;


public class RunStatus
{
    private final int INTEGER_MAX_VALUE = 2147483647;
    private int _currentActiveProcessId = -1;

    public enum Priority
    {
        PRIORITIZE_LOW_PROCESS_ID,
        PRIORITIZE_HIGH_PROCESS_ID,
        USE_CUSTOM_PRIORITY
    }



    private Priority _prioritySetting = Priority.PRIORITIZE_HIGH_PROCESS_ID;

    private final ArrayList<Integer> _processQueue = new ArrayList<>();
    private final ArrayList<Integer> _terminationList = new ArrayList<>();



    public RunStatus()
    {
        _prioritySetting = Priority.PRIORITIZE_HIGH_PROCESS_ID;
    }
    public RunStatus(Priority priority)
    {
        _prioritySetting = priority;
    }



    public void FullResetToActiveState()
    {
        ClearAllProcesses();
        ClearAllTerminations();
        ClearCurrentActiveProcess();
    }



    public int GetCurrentActiveProcess()
    {
        return _currentActiveProcessId;
    }
    public void ClearCurrentActiveProcess()
    {
        _currentActiveProcessId = UNDEFINED_PROCESS_ID;
    }
    public void SetCurrentActiveProcess(int processId)
    {
        _currentActiveProcessId = processId;
    }



    public int Current(int pos)
    {
        return _processQueue.get(pos);
    }
    public int size()
    {
        return _processQueue.size();
    }
    public boolean IsNotBusy()
    {
        return _processQueue.isEmpty();
    }
    public boolean IsUsedByAnyProcess()
    {
        return !_processQueue.isEmpty();
    }
    public boolean IsUsedByAnotherProcess(int ourProcessId)
    {
        for (int i = 0; i < _processQueue.size(); i++)
        {
            if (_processQueue.get(i) != ourProcessId)
                return false;
        }

        return true;
    }
    public boolean IsThisProcessTerminated(int processId)
    {
        return _terminationList.contains(processId);
    }



    public void AddProcessToQueue(int processId)
    {
        _processQueue.add(processId);
    }
    public void ClearAllProcesses()
    {
        _processQueue.clear();
    }
    public void TryToRemoveProcessIdFromQueue(int processId)
    {
        int position = _processQueue.indexOf(processId);
        while(position != -1)
        {
            position = _processQueue.indexOf(processId);
            if (position != -1) _processQueue.remove(position);
        }
    }
    public boolean IsThisProcessHighestPriority(int processId)
    {
        int maxId = 0, minId = INTEGER_MAX_VALUE;
        boolean containsRequestedId = false;

        for (int i = 0; i < _processQueue.size(); i++)
        {
            int curProcess = _processQueue.get(i);
            if (curProcess == processId)
                containsRequestedId = true;

            if (curProcess > maxId) maxId = curProcess;
            if (curProcess < minId) minId = curProcess;
        }

        return containsRequestedId
            &&
                (
                    _prioritySetting == Priority.PRIORITIZE_HIGH_PROCESS_ID
                    && processId >= maxId
                ||
                    _prioritySetting == Priority.PRIORITIZE_LOW_PROCESS_ID
                    && processId <= minId
                );
    }
    public int GetHighestPriorityProcessId()
    {
        int maxId = 0, minId = INTEGER_MAX_VALUE, processId = 0;

        for (int i = 0; i < _processQueue.size(); i++)
        {
            int curProcess = _processQueue.get(i);

            if (curProcess > maxId)
            {
                maxId = curProcess;
                processId = i;
            }
            if (curProcess < minId)
            {
                minId = curProcess;
                processId = i;
            }
        }

        return processId;
    }



    public void AddProcessToTerminationList(int processId)
    {
        TryToRemoveProcessIdFromQueue(processId);
        _terminationList.add(processId);
    }
    public boolean IsForcedToTerminateThisProcess(int processId)
    {
        for (int i = 0; i < _terminationList.size(); i++)
        {
            if (_terminationList.get(i) == processId)
                return true;
        }

        return false;
    }
    public void TryToRemoveThisProcessFromTerminationList(int processId)
    {
        int position = _terminationList.indexOf(processId);
        while(position != -1)
        {
            position = _terminationList.indexOf(processId);
            if (position != -1) _terminationList.remove(position);
        }
    }
    public void ClearAllTerminations()
    {
        _terminationList.clear();
    }



    public void ChangePrioritySetting(Priority newPrioritySetting)
    {
        _prioritySetting = newPrioritySetting;
    }
}