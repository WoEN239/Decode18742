package org.woen.enumerators


class ShootingPhase
{
    private var _name : Name

    constructor(name: Name = Name.NOT_ACTIVE) { _name = name }


    enum class Name
    {
        NOT_ACTIVE,

        P1_OPENING_TURRET_GATE,
        P2_SHOOTING,
        P3_OPENING_LAUNCHER,
        P4_CALIBRATING
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
            Name.NOT_ACTIVE             -> Name.P1_OPENING_TURRET_GATE
            Name.P1_OPENING_TURRET_GATE -> Name.P2_SHOOTING
            Name.P2_SHOOTING            -> Name.P3_OPENING_LAUNCHER
            Name.P3_OPENING_LAUNCHER    -> Name.P4_CALIBRATING
            Name.P4_CALIBRATING         -> Name.NOT_ACTIVE
        }
    }


    fun isNotShooting() = _name == Name.NOT_ACTIVE
    fun isShooting() = !isNotShooting()
}