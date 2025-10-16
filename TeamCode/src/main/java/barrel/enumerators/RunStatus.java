package barrel.enumerators;


public class RunStatus
{
    static public final int INACTIVE = 0, ACTIVE = 1, PAUSE = 2, TERMINATED = 3, USED_BY_ANOTHER_PROCESS = 4;
    static public final int IS_INACTIVE = 0, IS_ACTIVE = 1, DO_TERMINATE = 2, IS_TERMINATED = 3;

    private TerminationStatus _terminationStatus;
    private Name _memName;
    private int  _memId, _terminationId;


    public RunStatus(int value)
    {
        SetTermination(TerminationStatus.IS_INACTIVE, IS_INACTIVE);
        Set(ToName(value), value);
    }
    public RunStatus(Name name)
    {
        SetTermination(TerminationStatus.IS_INACTIVE, IS_INACTIVE);
        Set(name, ToInt(name));
    }
    public RunStatus(int value, Name name)
    {
        SetTermination(TerminationStatus.IS_INACTIVE, IS_INACTIVE);
        Set(name, value);
    }
    public RunStatus(Name name, int value)
    {
        SetTermination(TerminationStatus.IS_INACTIVE, IS_INACTIVE);
        Set(name, value);
    }
    public RunStatus()
    {
        SetTermination(TerminationStatus.IS_INACTIVE, IS_INACTIVE);
        Set(Name.INACTIVE, INACTIVE);
    }


    public enum Name
    {
        INACTIVE,
        ACTIVE,
        PAUSE,
        TERMINATED,
        USED_BY_ANOTHER_PROCESS
    }
    public enum TerminationStatus
    {
        IS_INACTIVE,
        IS_ACTIVE,
        DO_TERMINATE,
        IS_TERMINATED
    }



    public void Set(Name name)
    {
        Set(name, ToInt(name));
    }
    public void Set(int value)
    {
        Set(ToName(value), value);
    }
    public void Set(Name name, int value)
    {
        Set(value, name);
    }
    public void Set(int value, Name name)
    {
        _memId = value;
        _memName = name;
    }


    public void SafeResetTermination()
    {
        if (_terminationId == IS_TERMINATED)
            SetTermination(IS_ACTIVE, TerminationStatus.IS_ACTIVE);
    }
    public void SetTermination(int value)
    {
        SetTermination(ToTermination(value), value);
    }
    public void SetTermination(TerminationStatus name)
    {
        SetTermination(name, ToInt(name));
    }
    public void SetTermination(int value, TerminationStatus name)
    {
        SetTermination(name, value);
    }
    public void SetTermination(TerminationStatus name, int value)
    {
        _terminationStatus = name;
        _terminationId = value;
    }


    public Name Name()
    {
        return _memName;
    }
    public int  Id()
    {
        return _memId;
    }
    public TerminationStatus Termination()
    {
        return _terminationStatus;
    }
    public int TerminationId()
    {
        return _terminationId;
    }

    public boolean IsActive()
    {
        return _memId == IS_ACTIVE;
    }
    public boolean IsInactive()
    {
        return _memId == IS_INACTIVE;
    }
    public boolean IsUsedByAnotherProcess()
    {
        return _memId == USED_BY_ANOTHER_PROCESS;
    }



    static public Name ToName (int value)
    {
        switch (value)
        {
            case INACTIVE:   return Name.INACTIVE;
            case ACTIVE:     return Name.ACTIVE;
            case PAUSE:      return Name.PAUSE;
            case TERMINATED: return Name.TERMINATED;
            default:         return Name.USED_BY_ANOTHER_PROCESS;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case INACTIVE:   return INACTIVE;
            case ACTIVE:     return ACTIVE;
            case PAUSE:      return PAUSE;
            case TERMINATED: return TERMINATED;
            default:         return USED_BY_ANOTHER_PROCESS;
        }
    }



    static public TerminationStatus ToTermination (int value)
    {
        switch (value)
        {
            case IS_INACTIVE:   return TerminationStatus.IS_INACTIVE;
            case IS_ACTIVE:     return TerminationStatus.IS_ACTIVE;
            case DO_TERMINATE:  return TerminationStatus.DO_TERMINATE;
            default:            return TerminationStatus.IS_TERMINATED;
        }
    }
    static public int ToInt (TerminationStatus name)
    {
        switch (name)
        {
            case IS_INACTIVE:   return IS_INACTIVE;
            case IS_ACTIVE:     return IS_ACTIVE;
            case DO_TERMINATE:  return DO_TERMINATE;
            default:            return IS_TERMINATED;
        }
    }
}