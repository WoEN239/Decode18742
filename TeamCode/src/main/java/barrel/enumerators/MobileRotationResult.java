package barrel.enumerators;

import org.woen.modules.storage.sorting.MobileSlot;

public class MobileRotationResult
{
    static public final int SUCCESS = 0,
            SUCCESS_IN = 1, SUCCESS_IN_DOUBLE = 2,
            SUCCESS_OUT = 3, SUCCESS_OUT_DOUBLE = 4,
            FAIL_UNKNOWN = 5, FAIL_ILLEGAL_ARGUMENTS = 6,
            FAIL_SAME_POSITION = 7, FAIL_CANT_ROTATE_BACKWARDS = 8;


    private Name _memName;
    private int  _memId;


    public enum Name
    {
        SUCCESS,

        SUCCESS_IN,
        SUCCESS_IN_DOUBLE,

        SUCCESS_OUT,
        SUCCESS_OUT_DOUBLE,

        FAIL_UNKNOWN,
        FAIL_ILLEGAL_ARGUMENTS,

        FAIL_SAME_POSITION,
        FAIL_CANT_ROTATE_BACKWARDS
    }



    public MobileRotationResult(int value)
    {
        Set(value, ToName(value));
    }
    public MobileRotationResult(Name name)
    {
        Set(ToInt(name), name);
    }
    public MobileRotationResult(int value, Name name)
    {
        Set(value, name);
    }
    public MobileRotationResult(Name name, int value)
    {
        Set(value, name);
    }
    public MobileRotationResult()
    {
        Set(FAIL_UNKNOWN, Name.FAIL_UNKNOWN);
    }



    public void Set(int value)
    {
        Set(value, ToName(value));
    }
    public void Set(Name name)
    {
        Set(ToInt(name), name);
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
    public int Id()
    {
        return _memId;
    }



    public boolean DidFail()
    {
        return _memId > SUCCESS_OUT_DOUBLE;
    }
    static public  boolean DidFail(Name name)
    {
        return DidFail(ToInt(name));
    }
    static public  boolean DidFail(int value)
    {
        return value > SUCCESS_OUT_DOUBLE;
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
            case SUCCESS: return Name.SUCCESS;

            case SUCCESS_IN:        return Name.SUCCESS_IN;
            case SUCCESS_IN_DOUBLE: return Name.SUCCESS_IN_DOUBLE;

            case SUCCESS_OUT:        return Name.SUCCESS_OUT;
            case SUCCESS_OUT_DOUBLE: return Name.SUCCESS_OUT_DOUBLE;

            case FAIL_UNKNOWN:           return Name.FAIL_UNKNOWN;
            case FAIL_ILLEGAL_ARGUMENTS: return Name.FAIL_ILLEGAL_ARGUMENTS;

            case FAIL_SAME_POSITION: return Name.FAIL_SAME_POSITION;
            default:                 return Name.FAIL_CANT_ROTATE_BACKWARDS;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case SUCCESS: return SUCCESS;

            case SUCCESS_IN:        return SUCCESS_IN;
            case SUCCESS_IN_DOUBLE: return SUCCESS_IN_DOUBLE;

            case SUCCESS_OUT:        return SUCCESS_OUT;
            case SUCCESS_OUT_DOUBLE: return SUCCESS_OUT_DOUBLE;

            case FAIL_UNKNOWN:           return FAIL_UNKNOWN;
            case FAIL_ILLEGAL_ARGUMENTS: return FAIL_ILLEGAL_ARGUMENTS;

            case FAIL_SAME_POSITION: return FAIL_SAME_POSITION;
            default:                 return FAIL_CANT_ROTATE_BACKWARDS;
        }
    }

    public int ToEndStorageSlot()
    {
        switch (_memId)
        {
            case SUCCESS_IN:
                return StorageSlot.MOBILE_OUT;

            case SUCCESS:
            case SUCCESS_IN_DOUBLE:
                return StorageSlot.MOBILE_IN;


            case SUCCESS_OUT:
            case SUCCESS_OUT_DOUBLE:
                return StorageSlot.OUTSIDE_MOBILE;

            default: return StorageSlot.UNDEFINED;
        }
    }
}