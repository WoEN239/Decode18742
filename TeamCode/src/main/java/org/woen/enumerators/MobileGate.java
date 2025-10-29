package org.woen.enumerators;

public class MobileGate
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



    public MobileGate(int value)
    {
        Set(value, ToName(value));
    }
    public MobileGate(Name name)
    {
        Set(ToInt(name), name);
    }
    public MobileGate(int value, Name name)
    {
        Set(value, name);
    }
    public MobileGate(Name name, int value)
    {
        Set(value, name);
    }
    public MobileGate()
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