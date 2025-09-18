package barrel.enumerators;


public class RequestResult
{
    public enum Name
    {
        SUCCESS,
        SUCCESS_LEFT,
        SUCCESS_CENTER,
        SUCCESS_RIGHT,
        SUCCESS_IS_NOW_EMPTY,
        FAIL_UNKNOWN,
        FAIL_IS_EMPTY,
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



    public boolean DidFail()
    {
        return memId > SUCCESS_IS_NOW_EMPTY();
    }
    static public boolean DidFail(Name name)
    {
        return ToInt(name) > SUCCESS_IS_NOW_EMPTY();
    }
    public boolean DidSucceed()
    {
        return memId < FAIL_UNKNOWN();
    }
    static public boolean DidSucceed(Name name)
    {
        return ToInt(name) < FAIL_UNKNOWN();
    }



    static public int SUCCESS()
    {
        return 0;
    }
    static public int SUCCESS_LEFT()
    {
        return 1;
    }
    static public int SUCCESS_CENTER()
    {
        return 2;
    }
    static public int SUCCESS_RIGHT()
    {
        return 3;
    }
    static public int SUCCESS_IS_NOW_EMPTY()
    {
        return 4;
    }
    static public int FAIL_UNKNOWN()
    {
        return 5;
    }
    static public int FAIL_IS_EMPTY()
    {
        return 6;
    }
    static public int FAIL_COLOR_NOT_PRESENT()
    {
        return 7;
    }
    static public int FAIL_IS_CURRENTLY_BUSY()
    {
        return 8;
    }
    static public int FAIL_NOT_ENOUGH_COLORS()
    {
        return 9;
    }


    static public Name ToName(int value)
    {
        switch (value)
        {
            case 0: return Name.SUCCESS;
            case 1: return Name.SUCCESS_LEFT;
            case 2: return Name.SUCCESS_CENTER;
            case 3: return Name.SUCCESS_RIGHT;
            case 4: return Name.SUCCESS_IS_NOW_EMPTY;

            case 5: return Name.FAIL_UNKNOWN;
            case 6: return Name.FAIL_IS_EMPTY;
            case 7: return Name.FAIL_COLOR_NOT_PRESENT;
            case 8: return Name.FAIL_IS_CURRENTLY_BUSY;
            default: return Name.FAIL_NOT_ENOUGH_COLORS;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case SUCCESS_LEFT:   return 0;
            case SUCCESS_CENTER: return 1;
            case SUCCESS_RIGHT:  return 2;
            case SUCCESS_IS_NOW_EMPTY:   return 3;

            case FAIL_UNKNOWN:   return 4;
            case FAIL_IS_EMPTY:  return 5;
            case FAIL_COLOR_NOT_PRESENT: return 6;
            case FAIL_IS_CURRENTLY_BUSY: return 7;
            default: return 8;  //  FAIL_NOT_ENOUGH_COLORS
        }
    }
}