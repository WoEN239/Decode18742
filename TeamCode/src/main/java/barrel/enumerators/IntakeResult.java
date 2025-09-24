package barrel.enumerators;


public class IntakeResult
{
    public enum Name
    {
        SUCCESS,
        SUCCESS_LEFT,
        SUCCESS_CENTER,
        SUCCESS_RIGHT,
        FAIL_UNKNOWN,
        FAIL_STORAGE_IS_FULL,
        FAIL_IS_CURRENTLY_BUSY,
        PROCESS_WAS_TERMINATED
    }


    Name memName;
    int  memId;


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
    public int  GetId()
    {
        return memId;
    }



    public boolean DidFail()
    {
        return memId > SUCCESS_RIGHT();
    }
    static public boolean DidFail(Name name)
    {
        return ToInt(name) > SUCCESS_RIGHT();
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
    static public int FAIL_UNKNOWN()
    {
        return 4;
    }
    static public int FAIL_STORAGE_IS_FULL()
    {
        return 5;
    }
    static public int FAIL_IS_CURRENTLY_BUSY()
    {
        return 6;
    }
    static public int PROCESS_WAS_TERMINATED()
    {
        return 7;
    }



    static public Name ToName (int value)
    {
        switch (value)
        {
            case 0:  return Name.SUCCESS;
            case 1:  return Name.SUCCESS_LEFT;
            case 2:  return Name.SUCCESS_CENTER;
            case 3:  return Name.SUCCESS_RIGHT;

            case 4:  return Name.FAIL_UNKNOWN;
            case 5:  return Name.FAIL_STORAGE_IS_FULL;
            case 6:  return Name.FAIL_IS_CURRENTLY_BUSY;
            default: return Name.PROCESS_WAS_TERMINATED;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case SUCCESS:        return 0;
            case SUCCESS_LEFT:   return 1;
            case SUCCESS_CENTER: return 2;
            case SUCCESS_RIGHT:  return 3;

            case FAIL_UNKNOWN:   return 4;
            case FAIL_STORAGE_IS_FULL:   return 5;
            case FAIL_IS_CURRENTLY_BUSY: return 6;
            default: return 7;   //  PROCESS_WAS_TERMINATED
        }
    }
}
