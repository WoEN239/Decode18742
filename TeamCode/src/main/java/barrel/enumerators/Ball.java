package barrel.enumerators;


public class Ball
{
    static public final int NONE = 0, PURPLE = 1, GREEN = 2;

    private Name _memName;
    private int  _memId;


    public Ball(int value)
    {
        Set(ToName(value), value);
    }
    public Ball(Name name)
    {
        Set(name, ToInt(name));
    }
    public Ball(int value, Name name)
    {
        Set(name, value);
    }
    public Ball(Name name, int value)
    {
        Set(name, value);
    }
    public Ball()
    {
        Empty();
    }
    public enum Name
    {
        NONE,
        PURPLE,
        GREEN
    }



    public void Empty()
    {
        Set(Name.NONE, NONE);
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


    public Name Name()
    {
        return _memName;
    }
    public int  Id()
    {
        return _memId;
    }

    public boolean IsFilled()
    {
        return _memId != NONE;
    }
    public boolean HasBall()
    {
        return  _memId != NONE;
    }
    public boolean HasBall(Ball ball)
    {
        return _memId == ball.Id();
    }
    public boolean HasBall(Ball.Name ball)
    {
        return _memName == ball;
    }
    public boolean HasPurpleBall()
    {
        return _memId == PURPLE;
    }
    public boolean HasGreenBall()
    {
        return  _memId == GREEN;
    }
    public boolean IsEmpty()
    {
        return _memId == NONE;
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