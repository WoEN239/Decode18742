package org.woen.enumerators



class SortingPhase
{
    private var _name : Name

    constructor(name: Name = Name.NOT_SORTING) { _name = name }


    enum class Name
    {
        NOT_SORTING,

        P1_CLOSING_TURRET_GATE,
        P2_REALIGNING_UPWARDS,
        P3_REALIGNING_DOWNWARDS,

        P4_OPENING_GATE,
        P5_OPENING_PUSH,

        P6_CLOSING_GATE_AND_PUSH,
        P7_ROTATING_BELTS,
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
            Name.NOT_SORTING              -> Name.P1_CLOSING_TURRET_GATE
            Name.P1_CLOSING_TURRET_GATE   -> Name.P2_REALIGNING_UPWARDS
            Name.P2_REALIGNING_UPWARDS    -> Name.P3_REALIGNING_DOWNWARDS
            Name.P3_REALIGNING_DOWNWARDS  -> Name.P4_OPENING_GATE
            Name.P4_OPENING_GATE          -> Name.P5_OPENING_PUSH
            Name.P5_OPENING_PUSH          -> Name.P6_CLOSING_GATE_AND_PUSH
            Name.P6_CLOSING_GATE_AND_PUSH -> Name.P7_ROTATING_BELTS
            Name.P7_ROTATING_BELTS        -> Name.NOT_SORTING
        }
    }


    fun isNotSorting() = _name == Name.NOT_SORTING
    fun isSorting() = !isNotSorting()
}