package barrel.enumerators;


public class RequestResult
{
    public enum Name
    {
        SUCCESS_LEFT,
        SUCCESS_CENTER,
        SUCCESS_RIGHT,
        FAIL_UNKNOWN,
        FAIL_COLOR_NOT_PRESENT,
        FAIL_IS_CURRENTLY_BUSY,
        FAIL_NOT_ENOUGH_COLORS
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
    static public int FAIL_COLOR_NOT_PRESENT()
    {
        return 4;
    }
    static public int FAIL_IS_CURRENTLY_BUSY()
    {
        return 5;
    }
    static public int FAIL_NOT_ENOUGH_COLORS()
    {
        return 6;
    }


    static public Name ToName(int value)
    {
        return value < 3 ?
                value < 2 ?
                    value < 1 ? Name.SUCCESS_LEFT : Name.SUCCESS_CENTER
                    : value < 3 ? Name.SUCCESS_RIGHT : Name.FAIL_UNKNOWN
                : value < 5 ?
                    value < 4 ? Name.FAIL_COLOR_NOT_PRESENT : Name.FAIL_IS_CURRENTLY_BUSY
                    : Name.FAIL_NOT_ENOUGH_COLORS;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case SUCCESS_LEFT:   return 0;
            case SUCCESS_CENTER: return 1;
            case SUCCESS_RIGHT:  return 2;
            case FAIL_UNKNOWN:   return 3;
            case FAIL_COLOR_NOT_PRESENT: return 4;
            case FAIL_IS_CURRENTLY_BUSY: return 5;
            default: return 6;
        }
    }
}