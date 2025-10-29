package org.woen.enumerators;


public class BallRequest
{
    static public final int NONE = 0, PURPLE = 1, GREEN = 2, PREFER_PURPLE = 3, PREFER_GREEN = 4, ANY_CLOSEST = 5;

    private Name _memName;
    private int  _memId;


    public BallRequest(int value)
    {
        Set(value, ToName(value));
    }
    public BallRequest(Name name)
    {
        Set(ToInt(name), name);
    }
    public BallRequest(int value, Name name)
    {
        Set(value, name);
    }
    public BallRequest(Name name, int value)
    {
        Set(value, name);
    }
    public BallRequest()
    {
        Set(NONE, Name.NONE);
    }


    public enum Name
    {
        NONE,
        PURPLE,
        GREEN,
        PREFER_PURPLE,
        PREFER_GREEN,
        ANY_CLOSEST
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
    public int  Id()
    {
        return _memId;
    }



    public boolean IsNone()
    {
        return _memId == NONE;
    }
    static public boolean IsNone(int value)
    {
        return value == NONE;
    }
    static public boolean IsNone(Name name)
    {
        return IsNone(ToInt(name));
    }


    public boolean IsAny()
    {
        return _memId == ANY_CLOSEST;
    }
    static public boolean IsAny(int id)
    {
        return id == ANY_CLOSEST;
    }
    static public boolean IsAny(Name name)
    {
        return IsAny(ToInt(name));
    }


    public boolean IsAbstractAny()
    {
        return _memId > GREEN;
    }
    static public boolean IsAbstractAny(int id)
    {
        return id > GREEN;
    }
    static public boolean IsAbstractAny(Name name)
    {
        return IsAbstractAny(ToInt(name));
    }


    public boolean IsPreferred()
    {
        return _memId == PREFER_PURPLE || _memId == PREFER_GREEN;
    }
    static public boolean IsPreferred(int id)
    {
        return id == PREFER_PURPLE || id == PREFER_GREEN;
    }
    static public boolean IsPreferred(Name name)
    {
        return IsPreferred(ToInt(name));
    }



    public Ball.Name ToBall()
    {
        switch (_memId)
        {
            case PURPLE:
            case PREFER_PURPLE:
                return Ball.Name.PURPLE;
            case GREEN:
            case PREFER_GREEN:
                return Ball.Name.GREEN;
            default:
                return Ball.Name.NONE;
        }
    }
    static public Ball.Name ToBall(Name ballRequest)
    {
        switch (ballRequest)
        {
            case PURPLE:
            case PREFER_PURPLE:
                return Ball.Name.PURPLE;
            case GREEN:
            case PREFER_GREEN:
                return Ball.Name.GREEN;
            default:
                return Ball.Name.NONE;
        }
    }

    public Ball.Name ToInverseBall()
    {
        switch (_memId)
        {
            case PURPLE:
            case PREFER_PURPLE:
                return Ball.Name.GREEN;
            case GREEN:
            case PREFER_GREEN:
                return Ball.Name.PURPLE;
            default:
                return Ball.Name.NONE;
        }
    }
    static public Ball.Name ToInverseBall(Name ballRequest)
    {
        switch (ballRequest)
        {
            case PURPLE:
            case PREFER_PURPLE:
                return Ball.Name.GREEN;
            case GREEN:
            case PREFER_GREEN:
                return Ball.Name.PURPLE;
            default:
                return Ball.Name.NONE;
        }
    }



    static public Name ToName (int value)
    {
        switch (value)
        {
            case NONE:   return Name.NONE;
            case PURPLE: return Name.PURPLE;
            case GREEN:  return Name.GREEN;
            case PREFER_PURPLE: return Name.PREFER_PURPLE;
            case PREFER_GREEN:  return Name.PREFER_GREEN;
            default:     return Name.ANY_CLOSEST;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case NONE:   return NONE;
            case PURPLE: return PURPLE;
            case GREEN:  return GREEN;
            default:     return ANY_CLOSEST;
        }
    }
}