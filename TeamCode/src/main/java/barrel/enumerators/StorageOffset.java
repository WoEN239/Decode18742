package barrel.enumerators;


public class StorageOffset
{
    public StorageOffset()
    {
        memId = NONE();
        memName = Name.NONE;
    }


    public enum Name
    {
        NONE,
        CW_60,
        CCW_60
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



    static public int NONE()
    {
        return 0;
    }
    static public int CW_60()
    {
        return 1;
    }
    static public int CCW_60()
    {
        return 2;
    }



    static public Name ToName (int value)
    {
        return value == 0 ? Name.NONE
                : value == 1 ? Name.CW_60
                : Name.CCW_60;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case NONE: return 0;
            case CW_60:   return 1;
            default:       return 2;
        }
    }
}
