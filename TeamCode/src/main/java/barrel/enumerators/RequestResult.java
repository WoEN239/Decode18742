package barrel.enumerators;


public class RequestResult
{
    static public final int SUCCESS = 0, SUCCESS_BOTTOM = 1, SUCCESS_CENTER = 2,
            SUCCESS_MOBILE_OUT = 3, SUCCESS_MOBILE_IN = 4, SUCCESS_IS_NOW_EMPTY = 5,

                FAIL_UNKNOWN = 6, FAIL_IS_EMPTY = 7, FAIL_ILLEGAL_ARGUMENT = 8,

                    FAIL_COLOR_NOT_PRESENT = 9, FAIL_NOT_ENOUGH_COLORS = 10,

                        FAIL_HARDWARE_PROBLEM = 11, FAIL_SOFTWARE_STORAGE_DESYNC = 12,

                            FAIL_IS_CURRENTLY_BUSY = 13, FAIL_PROCESS_WAS_TERMINATED = 14;


    private Name _memName;
    private int  _memId;



    public RequestResult(int value)
    {
        Set(value, ToName(value));
    }
    public RequestResult(Name name)
    {
        Set(ToInt(name), name);
    }
    public RequestResult(int value, Name name)
    {
        Set(value, name);
    }
    public RequestResult(Name name, int value)
    {
        Set(value, name);
    }
    public RequestResult()
    {
        Set(FAIL_UNKNOWN, Name.FAIL_UNKNOWN);
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
        FAIL_ILLEGAL_ARGUMENT,

        FAIL_COLOR_NOT_PRESENT,
        FAIL_NOT_ENOUGH_COLORS,

        FAIL_HARDWARE_PROBLEM,
        FAIL_SOFTWARE_STORAGE_DESYNC,

        FAIL_IS_CURRENTLY_BUSY,
        FAIL_PROCESS_WAS_TERMINATED
    }



    public void Set(Name name)
    {
        Set(ToInt(name), name);
    }
    public void Set(int value)
    {
        Set(value, ToName(value));
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
            case SUCCESS: return Name.SUCCESS;

            case SUCCESS_BOTTOM: return Name.SUCCESS_BOTTOM;
            case SUCCESS_CENTER: return Name.SUCCESS_CENTER;
            case SUCCESS_MOBILE_OUT:    return Name.SUCCESS_MOBILE_OUT;
            case SUCCESS_MOBILE_IN:     return Name.SUCCESS_MOBILE_IN;

            case SUCCESS_IS_NOW_EMPTY:  return Name.SUCCESS_IS_NOW_EMPTY;



            case FAIL_UNKNOWN:           return Name.FAIL_UNKNOWN;
            case FAIL_IS_EMPTY:          return Name.FAIL_IS_EMPTY;
            case FAIL_ILLEGAL_ARGUMENT:  return Name.FAIL_ILLEGAL_ARGUMENT;

            case FAIL_COLOR_NOT_PRESENT: return Name.FAIL_COLOR_NOT_PRESENT;
            case FAIL_NOT_ENOUGH_COLORS: return Name.FAIL_NOT_ENOUGH_COLORS;

            case FAIL_HARDWARE_PROBLEM:        return Name.FAIL_HARDWARE_PROBLEM;
            case FAIL_SOFTWARE_STORAGE_DESYNC: return Name.FAIL_SOFTWARE_STORAGE_DESYNC;

            case FAIL_IS_CURRENTLY_BUSY: return Name.FAIL_IS_CURRENTLY_BUSY;
            default:                     return Name.FAIL_PROCESS_WAS_TERMINATED;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case SUCCESS: return SUCCESS;

            case SUCCESS_BOTTOM: return SUCCESS_BOTTOM;
            case SUCCESS_CENTER: return SUCCESS_CENTER;
            case SUCCESS_MOBILE_OUT:   return SUCCESS_MOBILE_OUT;
            case SUCCESS_MOBILE_IN:    return SUCCESS_MOBILE_IN;

            case SUCCESS_IS_NOW_EMPTY: return SUCCESS_IS_NOW_EMPTY;



            case FAIL_UNKNOWN:           return FAIL_UNKNOWN;
            case FAIL_IS_EMPTY:          return FAIL_IS_EMPTY;
            case FAIL_ILLEGAL_ARGUMENT:  return FAIL_ILLEGAL_ARGUMENT;

            case FAIL_COLOR_NOT_PRESENT: return FAIL_COLOR_NOT_PRESENT;
            case FAIL_NOT_ENOUGH_COLORS: return FAIL_NOT_ENOUGH_COLORS;

            case FAIL_HARDWARE_PROBLEM:        return FAIL_HARDWARE_PROBLEM;
            case FAIL_SOFTWARE_STORAGE_DESYNC: return FAIL_SOFTWARE_STORAGE_DESYNC;

            case FAIL_IS_CURRENTLY_BUSY: return FAIL_IS_CURRENTLY_BUSY;
            default:                     return FAIL_PROCESS_WAS_TERMINATED;
        }
    }
}