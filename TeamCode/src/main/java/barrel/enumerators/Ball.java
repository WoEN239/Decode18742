package barrel.enumerators;


public class Ball
{
    static public final int NONE = 0, PURPLE = 1, GREEN = 2;

    private Name _memName;
    private int _memId;


    public Ball(int value)
    {
        Set(ToName(value), value);
    }
    public Ball(Ball.Name name)
    {
        Set(name, ToInt(name));
    }
    public Ball(int value, Ball.Name name)
    {
        Set(name, value);
    }
    public Ball(Ball.Name name, int value)
    {
        Set(name, value);
    }
    public Ball()
    {
        Set(Ball.Name.NONE, NONE);
    }
    public enum Name
    {
        NONE,
        PURPLE,
        GREEN
    }




    public void Set(int value)
    {
        Set(ToName(value), value);
    }
    public void Set(Ball.Name name)
    {
        Set(name, ToInt(name));
    }
    public void Set(int value, Ball.Name name)
    {
        Set(name, value);
    }
    public void Set(Ball.Name name, int value)
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
        return value == NONE ? Name.NONE
                : value == PURPLE ? Name.PURPLE
                : Name.GREEN;
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case NONE:   return NONE;
            case PURPLE: return PURPLE;
            default:     return GREEN;
        }
    }
}