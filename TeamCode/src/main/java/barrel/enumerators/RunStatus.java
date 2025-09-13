package barrel.enumerators;


public class RunStatus
{
    public RunStatus()
    {
        memName = Name.INACTIVE;
        memId = 0;
    }


    public enum Name
    {
        INACTIVE,
        ACTIVE,
        PAUSE
    }


    Name memName;
    int memId;


    public void Set(int value)
    {
        memId = value;
        memName = ToName(value);
    }
    public void Set(Name name)
    {
        memId = ToInt(name);
        memName = name;
    }
    public void Set(int value, Name name)
    {
        memId = value;
        memName = name;
    }
    public void Set(Name name, int value)
    {
        memId = value;
        memName = name;
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


    static public Name ToName (int value)
    {
        return value == 0 ? Name.INACTIVE
                : value == 1 ? Name.ACTIVE
                : Name.PAUSE;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case INACTIVE: return 0;
            case ACTIVE:   return 1;
            default:       return 2;
        }
    }
}
