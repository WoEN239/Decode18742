package barrel.enumerators;


public class StorageSlot
{
    public enum Name
    {
        LEFT,
        CENTER,
        RIGHT
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



    static public int LEFT()
    {
        return 0;
    }
    static public int CENTER()
    {
        return 1;
    }
    static public int RIGHT()
    {
        return 2;
    }


    static public Name ToName (int value)
    {
        return value == 0 ? Name.LEFT
                : value == 1 ? Name.CENTER
                : Name.RIGHT;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case LEFT:   return 0;
            case CENTER: return 1;
            default:     return 2;
        }
    }
}