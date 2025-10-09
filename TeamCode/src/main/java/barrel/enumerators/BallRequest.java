package barrel.enumerators;


public class BallRequest
{
    static public final int NONE = 0, PURPLE = 1, GREEN = 2, ANY = 3;

    private Name _memName;
    private int  _memId;


    public BallRequest(int value)
    {
        Set(ToName(value), value);
    }
    public BallRequest(Name name)
    {
        Set(name, ToInt(name));
    }
    public BallRequest(int value, Name name)
    {
        Set(name, value);
    }
    public BallRequest(Name name, int value)
    {
        Set(name, value);
    }
    public BallRequest()
    {
        Set(Name.NONE, NONE);
    }


    public enum Name
    {
        NONE,
        PURPLE,
        GREEN,
        ANY
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


    public Name GetName()
    {
        return _memName;
    }
    public int  GetId()
    {
        return _memId;
    }



    static public Name ToName (int value)
    {
        switch (value)
        {
            case NONE:   return Name.NONE;
            case PURPLE: return Name.PURPLE;
            case GREEN:  return Name.GREEN;
            default:     return Name.ANY;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case NONE:   return NONE;
            case PURPLE: return PURPLE;
            case GREEN:  return GREEN;
            default:     return ANY;
        }
    }
}