package barrel.enumerators;


public class RequestResult
{
    public enum Name
    {
        SUCCESS_LEFT,
        SUCCESS_CENTER,
        SUCCESS_RIGHT,
        FAIL_COLOR_NOT_PRESENT
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
    public int SUCCESS_RIGHT()
    {
        return 2;
    }
    public int FAIL_COLOR_NOT_PRESENT()
    {
        return 3;
    }


    static public Name ToName(int value)
    {
        return value > 1 ?
                value < 0 ?
                    Name.SUCCESS_LEFT : Name.SUCCESS_CENTER
                : value < 2 ?
                Name.SUCCESS_RIGHT : Name.FAIL_COLOR_NOT_PRESENT;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case SUCCESS_LEFT:   return 0;
            case SUCCESS_CENTER: return 1;
            case SUCCESS_RIGHT:  return 2;
            default: return 3;
        }
    }
}