package woen239.enumerators;


public class ServoGate
{
    static public final int UNDEFINED = 0, OPEN = 1, CLOSED = 2;

    private Name _memName;
    private int  _memId;


    public enum Name
    {
        UNDEFINED,
        OPEN,
        CLOSED
    }



    public ServoGate(int value)
    {
        Set(value, ToName(value));
    }
    public ServoGate(Name name)
    {
        Set(ToInt(name), name);
    }
    public ServoGate(int value, Name name)
    {
        Set(value, name);
    }
    public ServoGate(Name name, int value)
    {
        Set(value, name);
    }
    public ServoGate()
    {
        Set(UNDEFINED, Name.UNDEFINED);
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



    static public Name ToName (int value)
    {
        switch (value)
        {
            case UNDEFINED: return Name.UNDEFINED;
            case OPEN: return Name.OPEN;
            default:   return Name.CLOSED;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case UNDEFINED: return UNDEFINED;
            case OPEN: return OPEN;
            default:   return CLOSED;
        }
    }
}