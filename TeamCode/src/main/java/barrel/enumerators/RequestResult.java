package barrel.enumerators;


public class RequestResult
{
    static public final int SUCCESS = 0, SUCCESS_BOTTOM = 1, SUCCESS_CENTER = 2,
            SUCCESS_MOBILE_OUT = 3, SUCCESS_MOBILE_IN = 4, SUCCESS_IS_NOW_EMPTY = 5,
                FAIL_UNKNOWN = 6, FAIL_IS_EMPTY = 7, FAIL_COLOR_NOT_PRESENT = 8,
                    FAIL_IS_CURRENTLY_BUSY = 9, FAIL_NOT_ENOUGH_COLORS = 10,
                        FAIL_PROCESS_WAS_TERMINATED = 11;

    private Name _memName;
    private int  _memId;


    public RequestResult(int value)
    {
        Set(ToName(value), value);
    }
    public RequestResult(Name name)
    {
        Set(name, ToInt(name));
    }
    public RequestResult(int value, Name name)
    {
        Set(name, value);
    }
    public RequestResult(Name name, int value)
    {
        Set(name, value);
    }
    public RequestResult()
    {
        Set(Name.FAIL_UNKNOWN, FAIL_UNKNOWN);
    }


    public enum Name
    {
        SUCCESS,
        SUCCESS_BOTTOM,
        SUCCESS_CENTER,
        SUCCESS_MOBILE_OUT,
        SUCCESS_MOBILE_IN,
        SUCCESS_IS_NOW_EMPTY,
        FAIL_UNKNOWN,
        FAIL_IS_EMPTY,
        FAIL_COLOR_NOT_PRESENT,
        FAIL_IS_CURRENTLY_BUSY,
        FAIL_NOT_ENOUGH_COLORS,
        FAIL_PROCESS_WAS_TERMINATED
    }



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
        _memId = value;
        _memName = name;
    }


    public Name Name()
    {
        return _memName;
    }
    public int  Id()
    {
        return _memId;
    }



    public boolean DidFail()
    {
        return _memId > SUCCESS_IS_NOW_EMPTY;
    }
    static public  boolean DidFail(Name name)
    {
        return DidFail(ToInt(name));
    }
    static public  boolean DidFail(int value)
    {
        return value > SUCCESS_IS_NOW_EMPTY;
    }
    public boolean DidSucceed()
    {
        return _memId < FAIL_UNKNOWN;
    }
    static public  boolean DidSucceed(Name name)
    {
        return DidSucceed(ToInt(name));
    }
    static public  boolean DidSucceed(int value)
    {
        return value < FAIL_UNKNOWN;
    }



    static public Name ToName(int value)
    {
        switch (value)
        {
            case SUCCESS: return  Name.SUCCESS;
            case SUCCESS_BOTTOM: return  Name.SUCCESS_BOTTOM;
            case SUCCESS_CENTER: return  Name.SUCCESS_CENTER;
            case SUCCESS_MOBILE_OUT:   return  Name.SUCCESS_MOBILE_OUT;
            case SUCCESS_MOBILE_IN:    return  Name.SUCCESS_MOBILE_IN;
            case SUCCESS_IS_NOW_EMPTY: return  Name.SUCCESS_IS_NOW_EMPTY;

            case FAIL_UNKNOWN:   return  Name.FAIL_UNKNOWN;
            case FAIL_IS_EMPTY:  return  Name.FAIL_IS_EMPTY;
            case FAIL_COLOR_NOT_PRESENT: return  Name.FAIL_COLOR_NOT_PRESENT;
            case FAIL_IS_CURRENTLY_BUSY: return  Name.FAIL_IS_CURRENTLY_BUSY;
            case FAIL_NOT_ENOUGH_COLORS: return  Name.FAIL_NOT_ENOUGH_COLORS;
            default: return Name.FAIL_PROCESS_WAS_TERMINATED;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case SUCCESS: return  SUCCESS;
            case SUCCESS_BOTTOM: return  SUCCESS_BOTTOM;
            case SUCCESS_CENTER: return  SUCCESS_CENTER;
            case SUCCESS_MOBILE_OUT:   return  SUCCESS_MOBILE_OUT;
            case SUCCESS_MOBILE_IN:    return  SUCCESS_MOBILE_IN;
            case SUCCESS_IS_NOW_EMPTY: return  SUCCESS_IS_NOW_EMPTY;

            case FAIL_UNKNOWN:   return  FAIL_UNKNOWN;
            case FAIL_IS_EMPTY:  return  FAIL_IS_EMPTY;
            case FAIL_COLOR_NOT_PRESENT: return  FAIL_COLOR_NOT_PRESENT;
            case FAIL_IS_CURRENTLY_BUSY: return  FAIL_IS_CURRENTLY_BUSY;
            case FAIL_NOT_ENOUGH_COLORS: return  FAIL_NOT_ENOUGH_COLORS;
            default: return FAIL_PROCESS_WAS_TERMINATED;
        }
    }
}