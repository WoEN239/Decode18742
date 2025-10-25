package barrel.enumerators;


import java.util.Objects;

public class StorageType
{
    static public final int STREAM_STORAGE = 0, SORTING_STORAGE = 1;

    private Name _memName;
    private int  _memId;
    private boolean _typeIsStream;


    public StorageType(Name type)
    {
        Set(type);
    }
    public StorageType(int type)
    {
        Set(type);
    }


    public enum Name
    {
        STREAM_STORAGE,
        SORTING_STORAGE
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
        _typeIsStream = UpdateIsStream();
    }
    private boolean UpdateIsStream()
    {
        return _memId == STREAM_STORAGE;
    }



    public Name Name()
    {
        return _memName;
    }
    public int Id()
    {
        return _memId;
    }

    public boolean IsStream()
    {
        return _typeIsStream;
    }
    public boolean IsSorting()
    {
        return !_typeIsStream;
    }

    static public boolean IsStream(int type)
    {
        return type == STREAM_STORAGE;
    }
    static public boolean IsSorting(int type)
    {
        return type == SORTING_STORAGE;
    }



    static public Name ToName (int value)
    {
        if (value == STREAM_STORAGE)
            return Name.STREAM_STORAGE;
        return Name.SORTING_STORAGE;
    }
    static public int ToInt (Name name)
    {
        if (Objects.requireNonNull(name) == Name.STREAM_STORAGE)
            return STREAM_STORAGE;
        return SORTING_STORAGE;
    }
}