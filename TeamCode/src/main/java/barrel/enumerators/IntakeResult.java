package barrel.enumerators;


public class IntakeResult
{
    public enum Name
    {
        SUCCESS_LEFT,
        SUCCESS_CENTER,
        SUCCESS_RIGHT,
        FAIL_UNKNOWN,
        FAIL_STORAGE_IS_FULL,
        FAIL_IS_CURRENTLY_BUSY
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



    static public int SUCCESS_LEFT()
    {
        return 0;
    }
    static public int SUCCESS_CENTER()
    {
        return 1;
    }
    static public int SUCCESS_RIGHT()
    {
        return 2;
    }
    static public int FAIL_UNKNOWN()
    {
        return 3;
    }
    static public int FAIL_STORAGE_IS_FULL()
    {
        return 4;
    }
    static public int FAIL_IS_CURRENTLY_BUSY()
    {
        return 5;
    }


    static public Name ToName (int value)
    {
        return value < 3 ?
                value < 2 ?
                    value < 1 ? Name.SUCCESS_LEFT : Name.SUCCESS_CENTER
                    : value < 3 ? Name.SUCCESS_RIGHT : Name.FAIL_UNKNOWN
                : value < 4 ?
                    Name.FAIL_STORAGE_IS_FULL : Name.FAIL_IS_CURRENTLY_BUSY;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case SUCCESS_LEFT:   return 0;
            case SUCCESS_CENTER: return 1;
            case SUCCESS_RIGHT:  return 2;
            case FAIL_UNKNOWN:   return 3;
            case FAIL_STORAGE_IS_FULL: return 4;
            default: return 5;
        }
    }
}
