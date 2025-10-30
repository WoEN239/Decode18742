package woen239.enumerators;


public class IntakeResult
{
    static public final int SUCCESS = 0, SUCCESS_BOTTOM = 1, SUCCESS_CENTER = 2,
        SUCCESS_MOBILE_OUT = 3, SUCCESS_MOBILE_IN = 4, FAIL_UNKNOWN = 5,
            FAIL_STORAGE_IS_FULL = 6, FAIL_IS_CURRENTLY_BUSY = 7,
                FAIL_HARDWARE_PROBLEM = 8, FAIL_PROCESS_WAS_TERMINATED = 9;


    private Name _memName;
    private int  _memId;


    public IntakeResult(int value)
    {
        Set(value, ToName(value));
    }
    public IntakeResult(Name name)
    {
        Set(ToInt(name), name);
    }
    public IntakeResult(int value, Name name)
    {
        Set(value, name);
    }
    public IntakeResult(Name name, int value)
    {
        Set(value, name);
    }
    public IntakeResult()
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
        FAIL_UNKNOWN,
        FAIL_STORAGE_IS_FULL,
        FAIL_IS_CURRENTLY_BUSY,
        FAIL_HARDWARE_PROBLEM,
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



    public boolean SolutionIsMobileIn()
    {
        return _memId == SUCCESS_MOBILE_IN;
    }
    public boolean SolutionIsMobileOut()
    {
        return _memId == SUCCESS_MOBILE_OUT;
    }
    public boolean SolutionIsMobileSlot()
    {
        return _memId == SUCCESS_MOBILE_OUT || _memId == SUCCESS_MOBILE_IN;
    }

    public boolean DidFail()
    {
        return _memId > SUCCESS_MOBILE_IN;
    }
    static public  boolean DidFail(Name name)
    {
        return DidFail(ToInt(name));
    }
    static public  boolean DidFail(int value)
    {
        return value > SUCCESS_MOBILE_IN;
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



    static public Name ToName (int value)
    {
        switch (value)
        {
            case SUCCESS:             return Name.SUCCESS;
            case SUCCESS_BOTTOM:      return Name.SUCCESS_BOTTOM;
            case SUCCESS_CENTER:      return Name.SUCCESS_CENTER;
            case SUCCESS_MOBILE_OUT:  return Name.SUCCESS_MOBILE_OUT;
            case SUCCESS_MOBILE_IN:   return Name.SUCCESS_MOBILE_IN;

            case FAIL_UNKNOWN:            return Name.FAIL_UNKNOWN;
            case FAIL_STORAGE_IS_FULL:    return Name.FAIL_STORAGE_IS_FULL;
            case FAIL_IS_CURRENTLY_BUSY:  return Name.FAIL_IS_CURRENTLY_BUSY;
            case FAIL_HARDWARE_PROBLEM:   return Name.FAIL_HARDWARE_PROBLEM;
            default:                      return Name.FAIL_PROCESS_WAS_TERMINATED;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case SUCCESS:             return SUCCESS;
            case SUCCESS_BOTTOM:      return SUCCESS_BOTTOM;
            case SUCCESS_CENTER:      return SUCCESS_CENTER;
            case SUCCESS_MOBILE_OUT:  return SUCCESS_MOBILE_OUT;
            case SUCCESS_MOBILE_IN:   return SUCCESS_MOBILE_IN;

            case FAIL_UNKNOWN:            return FAIL_UNKNOWN;
            case FAIL_STORAGE_IS_FULL:    return FAIL_STORAGE_IS_FULL;
            case FAIL_IS_CURRENTLY_BUSY:  return FAIL_IS_CURRENTLY_BUSY;
            case FAIL_HARDWARE_PROBLEM:   return FAIL_HARDWARE_PROBLEM;
            default:                      return FAIL_PROCESS_WAS_TERMINATED;
        }
    }
}