package org.woen.enumerators


class CalibrationPhase
{
    private var _name : Name

    constructor(name: Name = Name.NOT_ACTIVE) { _name = name }


    enum class Name
    {
        NOT_ACTIVE,

        P1_REVERSING_BELTS,
        P2_CLOSING_ALL_SERVOS,
        P3_REALIGNING_FORWARDS
    }

    val name get() = _name
    fun set (name: Name)
    {
        _name = name
    }


    fun switchToNextPhase()
    {
        _name = when (_name)
        {
            Name.NOT_ACTIVE             -> Name.P1_REVERSING_BELTS
            Name.P1_REVERSING_BELTS     -> Name.P2_CLOSING_ALL_SERVOS
            Name.P2_CLOSING_ALL_SERVOS  -> Name.P3_REALIGNING_FORWARDS
            Name.P3_REALIGNING_FORWARDS -> Name.NOT_ACTIVE
        }
    }


    fun isInactive() = _name == Name.NOT_ACTIVE
    fun isActive()   = !isInactive()
}