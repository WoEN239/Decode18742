package barrel.enumerators;


public class StorageOffset
{
    public enum Name
    {
        ALIGNED_TO_INTAKE,
        OFFSET_60_CCW,  //  To the left
        OFFSET_60_CW    //  To the right
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



    static public int ALIGNED_TO_INTAKE()
    {
        return 0;
    }
    static public int OFFSET_60_CCW()
    {
        return 1;
    }
    static public int OFFSET_60_CW()
    {
        return 2;
    }


    static public Name ToName (int value)
    {
        return value == 0 ? Name.ALIGNED_TO_INTAKE
                : value == 1 ? Name.OFFSET_60_CCW
                : Name.OFFSET_60_CW;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case ALIGNED_TO_INTAKE: return 0;
            case OFFSET_60_CCW:     return 1;
            default:                return 2;
        }
    }
}
