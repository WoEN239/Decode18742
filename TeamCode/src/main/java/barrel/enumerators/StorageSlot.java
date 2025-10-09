package barrel.enumerators;


public class StorageSlot
{
    static public final int BOTTOM = 0, CENTER = 1, MOBILE_OUT = 2, MOBILE_IN = 3, MOBILE = 4, UNDEFINED = 5;

    private Name _memName;
    private int  _memId;


    StorageSlot(int value)
    {
        Set(ToName(value), value);
    }
    StorageSlot(Name name)
    {
        Set(name, ToInt(name));
    }
    StorageSlot(int value, Name name)
    {
        Set(name, value);
    }
    StorageSlot(Name name, int value)
    {
        Set(name, value);
    }
    public StorageSlot()
    {
        Set(UNDEFINED, Name.UNDEFINED);
    }


    public enum Name
    {
        BOTTOM,
        CENTER,
        MOBILE_OUT,
        MOBILE_IN,
        MOBILE,
        UNDEFINED
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
    public int GetId()
    {
        return _memId;
    }


    static public Name ToName (int value)
    {
        switch (value)
        {
            case BOTTOM:     return Name.BOTTOM;
            case CENTER:     return Name.CENTER;
            case MOBILE_OUT: return Name.MOBILE_OUT;
            case MOBILE_IN:  return Name.MOBILE_IN;
            case MOBILE:     return Name.MOBILE;
            default:  return Name.UNDEFINED;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case BOTTOM: return BOTTOM;
            case CENTER: return CENTER;
            case MOBILE_OUT: return MOBILE_OUT;
            case MOBILE_IN:  return MOBILE_IN;
            case MOBILE:     return MOBILE;
            default:     return UNDEFINED;
        }
    }
}