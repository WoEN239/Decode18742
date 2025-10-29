package org.woen.enumerators;


public class StorageSlot
{
    static public final int BOTTOM = 0, CENTER = 1, OUTSIDE_MOBILE = 2,
            MOBILE_OUT = 3, MOBILE_IN = 4, UNDEFINED = 5;

    private Name _memName;
    private int  _memId;


    public StorageSlot(int value)
    {
        Set(value, ToName(value));
    }
    public StorageSlot(Name name)
    {
        Set(ToInt(name), name);
    }
    public StorageSlot(int value, Name name)
    {
        Set(value, name);
    }
    public StorageSlot(Name name, int value)
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
        OUTSIDE_MOBILE,
        MOBILE_OUT,
        MOBILE_IN,
        UNDEFINED
    }



    public void SetMobileOut()
    {
        Set(MOBILE_OUT, Name.MOBILE_OUT);
    }
    public void SetMobileIn()
    {
        Set(MOBILE_IN, Name.MOBILE_IN);
    }
    public void SetOutsideMobile()
    {
        Set(OUTSIDE_MOBILE, Name.OUTSIDE_MOBILE);
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
    public boolean Is_OUTSIDE_MOBILE()
    {
        return _memId == OUTSIDE_MOBILE;
    }

    public boolean IsNot_MOBILE_FAMILY()
    {
        return !Is_MOBILE_FAMILY();
    }
    public boolean Is_MOBILE_FAMILY()
    {
        return Is_MOBILE_OUT() || Is_MOBILE_IN() || Is_OUTSIDE_MOBILE();
    }


    static public MobileRotationResult MobileCanBeRotated(StorageSlot oldSlot, StorageSlot newSlot)
    {
        if (oldSlot.IsNot_MOBILE_FAMILY() || newSlot.IsNot_MOBILE_FAMILY())
            return new MobileRotationResult
                (
                    MobileRotationResult.FAIL_ILLEGAL_ARGUMENTS,
                    MobileRotationResult.Name.FAIL_ILLEGAL_ARGUMENTS
                );

        if (oldSlot.Id() == newSlot.Id())
            new MobileRotationResult
                (
                    MobileRotationResult.FAIL_SAME_POSITION,
                    MobileRotationResult.Name.FAIL_SAME_POSITION
                );

        switch (oldSlot.Id())
        {
            case OUTSIDE_MOBILE:
                return newSlot.Id() == MOBILE_OUT ?
                    new MobileRotationResult
                        (
                            MobileRotationResult.SUCCESS_IN,
                            MobileRotationResult.Name.SUCCESS_IN
                        )
                    : new MobileRotationResult
                        (
                            MobileRotationResult.SUCCESS_IN_DOUBLE,
                            MobileRotationResult.Name.SUCCESS_IN_DOUBLE
                        );

            case MOBILE_OUT:
                return newSlot.Id() == MOBILE_IN ?
                    new MobileRotationResult
                        (
                            MobileRotationResult.SUCCESS,
                            MobileRotationResult.Name.SUCCESS
                        )
                    : new MobileRotationResult
                        (
                            MobileRotationResult.SUCCESS_OUT_DOUBLE,
                            MobileRotationResult.Name.SUCCESS_OUT_DOUBLE
                        );

            case MOBILE_IN:
                return newSlot.Id() == OUTSIDE_MOBILE ?
                    new MobileRotationResult
                        (
                            MobileRotationResult.SUCCESS_OUT,
                            MobileRotationResult.Name.SUCCESS_OUT
                        )
                    : new MobileRotationResult
                        (
                            MobileRotationResult.FAIL_CANT_ROTATE_BACKWARDS,
                            MobileRotationResult.Name.FAIL_CANT_ROTATE_BACKWARDS
                        );
        }

        return new MobileRotationResult
            (
                MobileRotationResult.FAIL_UNKNOWN,
                MobileRotationResult.Name.FAIL_UNKNOWN
            );
    }



    static public Name ToName (int value)
    {
        switch (value)
        {
            case BOTTOM: return Name.BOTTOM;
            case CENTER: return Name.CENTER;
            case OUTSIDE_MOBILE: return Name.OUTSIDE_MOBILE;
            case MOBILE_OUT:     return Name.MOBILE_OUT;
            case MOBILE_IN:      return Name.MOBILE_IN;
            default:  return Name.UNDEFINED;
        }
    }
    static public int ToInt (Name name)
    {
        switch (name)
        {
            case BOTTOM: return BOTTOM;
            case CENTER: return CENTER;
            case OUTSIDE_MOBILE: return OUTSIDE_MOBILE;
            case MOBILE_OUT:     return MOBILE_OUT;
            case MOBILE_IN:      return MOBILE_IN;
            default:  return UNDEFINED;
        }
    }
}