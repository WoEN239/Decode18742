package barrel.enumerators;


public class RunStatus
{
    public RunStatus()
    {
        terminationStatus = TerminationStatus.IS_INACTIVE;
        memName = Name.INACTIVE;
        memId = INACTIVE();
    }


    public enum Name
    {
        INACTIVE,
        ACTIVE,
        PAUSE,
        TERMINATED
    }
    public enum TerminationStatus
    {
        IS_INACTIVE,
        IS_ACTIVE,
        DO_TERMINATE,
        IS_TERMINATED
    }



    TerminationStatus terminationStatus;
    int terminationId;
    Name memName;
    int memId;


    public void Set(int value)
    {
        Set(ToName(value), value);
    }
    public void Set(Name name)
    {
        Set(name, ToInt(name));
    }
    public void Set(int value, Name name)
    {
        Set(name, value);
    }
    public void Set(Name name, int value)
    {
        memId = value;
        memName = name;
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
        terminationStatus = name;
        terminationId = value;
    }


    public TerminationStatus GetTermination()
    {
        return terminationStatus;
    }
    public int GetTerminationId()
    {
        return terminationId;
    }


    public Name GetName()
    {
        return memName;
    }
    public int GetId()
    {
        return memId;
    }



    static public int INACTIVE()
    {
        return 0;
    }
    static public int ACTIVE()
    {
        return 1;
    }
    static public int PAUSE()
    {
        return 2;
    }
    static public int TERMINATED()
    {
        return 3;
    }

    static public int IS_INACTIVE()
    {
        return INACTIVE();
    }
    static public int IS_ACTIVE()
    {
        return ACTIVE();
    }
    static public int DO_TERMINATE()
    {
        return -1;
    }
    static public int IS_TERMINATED()
    {
        return TERMINATED();
    }



    static public Name ToName (int value)
    {
        switch (value)
        {
            case 0:  return Name.INACTIVE;
            case 1:  return Name.ACTIVE;
            case 2:  return Name.PAUSE;
            default: return Name.TERMINATED;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case INACTIVE:     return 0;
            case ACTIVE:       return 1;
            case PAUSE:        return 2;
            default:           return 4;  //  TERMINATED
        }
    }



    static public TerminationStatus ToTermination (int value)
    {
        switch (value)
        {
            case 0:  return TerminationStatus.IS_INACTIVE;
            case 1:  return TerminationStatus.IS_ACTIVE;
            case -1: return TerminationStatus.DO_TERMINATE;
            default: return TerminationStatus.IS_TERMINATED;
        }
    }
    static public int ToInt (TerminationStatus name)
    {
        switch (name)
        {
            case IS_INACTIVE:  return 0;
            case IS_ACTIVE:    return 1;
            case DO_TERMINATE: return -1;
            default:           return 3;   //  IS_TERMINATED
        }
    }
}
