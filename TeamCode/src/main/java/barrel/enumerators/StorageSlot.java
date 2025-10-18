package barrel.enumerators;


public class StorageSlot
{
    static public final int BOTTOM = 0, CENTER = 1, MOBILE_OUT = 2, MOBILE_IN = 3, MOBILE = 4, UNDEFINED = 5;

    private Name _memName;
    private int  _memId;


    StorageSlot(int value)
    {
        Set(value, ToName(value));
    }
    StorageSlot(Name name)
    {
        Set(ToInt(name), name);
    }
    StorageSlot(int value, Name name)
    {
        Set(value, name);
    }
    StorageSlot(Name name, int value)
    {
        Set(value, name);
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


    public void Set(Name name)
    {
        Set(ToInt(name), name);
    }
    public void Set(int value)
    {
        Set(value, ToName(value));
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

    public boolean Is_MOBILE_OUT()
    {
        return _memId == MOBILE_OUT;
    }
    public boolean Is_MOBILE_IN()
    {
        return _memId == MOBILE_IN;
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