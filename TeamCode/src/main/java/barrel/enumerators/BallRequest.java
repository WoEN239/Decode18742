package barrel.enumerators;


public class BallRequest
{
    public enum Name
    {
        NONE,
        PURPLE,
        GREEN,
        ANY
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
    static public int ANY()
    {
        return 3;
    }


    static public Name ToName (int value)
    {
        return value > 1 ?
                value < 0 ?
                    Name.NONE : Name.PURPLE
                : value < 2 ?
                    Name.GREEN : Name.ANY;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case NONE:   return 0;
            case PURPLE: return 1;
            case GREEN:  return 2;
            default:     return 3;
        }
    }
}
