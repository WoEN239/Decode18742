package org.woen.enumerators



class SortingPhase
{
    private var _name : Name

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
    fun set (name: Name)
    {
        _name = name
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


    fun isNotSorting() = _name == Name.NOT_ACTIVE
    fun isSorting() = !isNotSorting()
}