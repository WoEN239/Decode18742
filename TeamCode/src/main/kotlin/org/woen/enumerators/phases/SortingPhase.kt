package org.woen.enumerators.phases



class SortingPhase
{
    private var _name : Name
    var remainingRotations: Int = 0


    constructor(name: Name = Name.NOT_ACTIVE) { _name = name }


    enum class Name
    {
        NOT_ACTIVE,

        P1_CLOSING_TURRET_GATE,
        P2_REALIGN_STORAGE,
        P3_REALIGNING_UPWARDS,
        P4_REALIGNING_DOWNWARDS,

        P5_OPENING_GATE,
        P6_OPENING_PUSH,

        P7_CLOSING_GATE_AND_PUSH,
        P8_REALIGN_STORAGE,
    }

    val name get() = _name


    fun setInactive()
    {
        _name = Name.NOT_ACTIVE
    }
    fun startPhase1()
    {
        _name = Name.P1_CLOSING_TURRET_GATE
    }
    fun startPhase3()
    {
        _name = Name.P3_REALIGNING_UPWARDS
    }
    fun startPhase4()
    {
        _name = Name.P4_REALIGNING_DOWNWARDS
    }
    fun startPhase5()
    {
        _name = Name.P5_OPENING_GATE
    }
    fun startPhase6()
    {
        _name = Name.P6_OPENING_PUSH
    }
    fun startPhase7()
    {
        _name = Name.P7_CLOSING_GATE_AND_PUSH
    }
    fun startPhase8()
    {
        _name = Name.P8_REALIGN_STORAGE
    }
    fun switchToNextPhase()
    {
        _name = when (_name)
        {
            Name.NOT_ACTIVE               -> Name.P1_CLOSING_TURRET_GATE
            Name.P1_CLOSING_TURRET_GATE   -> Name.P2_REALIGN_STORAGE
            Name.P2_REALIGN_STORAGE       -> Name.P3_REALIGNING_UPWARDS
            Name.P3_REALIGNING_UPWARDS    -> Name.P4_REALIGNING_DOWNWARDS
            Name.P4_REALIGNING_DOWNWARDS  -> Name.P5_OPENING_GATE
            Name.P5_OPENING_GATE          -> Name.P6_OPENING_PUSH
            Name.P6_OPENING_PUSH          -> Name.P7_CLOSING_GATE_AND_PUSH
            Name.P7_CLOSING_GATE_AND_PUSH -> Name.P8_REALIGN_STORAGE
            Name.P8_REALIGN_STORAGE       -> Name.NOT_ACTIVE
        }
    }


    fun isInactive() = _name == Name.NOT_ACTIVE
    fun isActive()   = !isInactive()
}