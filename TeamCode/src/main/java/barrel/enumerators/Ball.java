package barrel.enumerators;

public class Ball
{
    public enum Name
    {
        NONE,
        PURPLE,
        GREEN
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
    static public int PURPLE()
    {
        return 1;
    }
    static public int GREEN()
    {
        return 2;
    }


    static public Name ToName (int value)
    {
        return value == 0 ? Name.NONE
                : value == 1 ? Name.PURPLE
                : Name.GREEN;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case NONE:   return 0;
            case PURPLE: return 1;
            default:     return 2;
        }
    }
}