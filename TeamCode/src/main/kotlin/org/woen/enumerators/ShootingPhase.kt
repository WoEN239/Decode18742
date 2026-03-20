package org.woen.enumerators


class ShootingPhase
{
    private var _name : Name
    var ballCountForPhase1 : Int = 0

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


    fun setInactive()
    {
        _name = Name.NOT_ACTIVE
    }
    fun startPhase1()
    {
        _name = Name.P1_OPENING_TURRET_GATE
    }
    fun startPhase4()
    {
        _name = Name.P4_CALIBRATING
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


    fun isNotActive()  = _name == Name.NOT_ACTIVE
    fun isActive()     = !isNotActive()

    fun isShootingPhase2() = _name == Name.P2_SHOOTING
    fun isShootingPhase3() = _name == Name.P3_OPENING_LAUNCHER
    fun isShootingPhase4() = _name == Name.P4_CALIBRATING
}