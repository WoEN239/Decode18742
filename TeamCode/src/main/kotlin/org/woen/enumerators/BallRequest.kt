package org.woen.enumerators



class BallRequest
{
    private var _id   = NONE
    private var _name = Name.NONE


    constructor() { empty() }
    constructor(id:   Int,  name: Name = toName(id))  { set(id, name) }
    constructor(name: Name, id:   Int  = toInt(name)) { set(id, name) }


    enum class Name
    {
        NONE,
        PURPLE,
        GREEN,

        PREFER_PURPLE,
        PREFER_GREEN,
        ANY_CLOSEST
    }


    fun empty() = set(NONE, Name.NONE)

    fun set(name: Name,  id: Int  = toInt(name)) = set(id, name)
    fun set(id:   Int, name: Name = toName(id))
    {
        _id   = id
        _name = name
    }


    fun id()   = _id
    fun name() = _name


    fun isNone() = _id == NONE
    fun isAny()  = _id == ANY_CLOSEST

    fun isAbstractAny() = _id > GREEN
    fun isPreferred()   = _id == PREFER_PURPLE || _id == PREFER_GREEN


    fun toBall(): Ball.Name
    {
        return when (_id)
        {
            PURPLE, PREFER_PURPLE -> Ball.Name.PURPLE
            GREEN,  PREFER_GREEN  -> Ball.Name.GREEN
            else -> Ball.Name.NONE
        }
    }
    fun toInverseBall(): Ball.Name
    {
        return when (_id)
        {
            PURPLE, PREFER_PURPLE -> Ball.Name.GREEN
            GREEN,  PREFER_GREEN  -> Ball.Name.PURPLE
            else -> Ball.Name.NONE
        }
    }


    companion object
    {
        const val NONE:   Int = 0
        const val PURPLE: Int = 1
        const val GREEN:  Int = 2

        const val PREFER_PURPLE: Int = 3
        const val PREFER_GREEN:  Int = 4
        const val ANY_CLOSEST:   Int = 5


        fun isAbstractAny(id:   Int)  = id > GREEN
        fun isAbstractAny(name: Name) = isAbstractAny(toInt(name))

        fun isPreferred(id:   Int)  = id == PREFER_PURPLE || id == PREFER_GREEN
        fun isPreferred(name: Name) = isPreferred(toInt(name))


        fun toName(id:  Int): Name
        {
            return when (id)
            {
                PURPLE -> Name.PURPLE
                GREEN  -> Name.GREEN

                PREFER_PURPLE -> Name.PREFER_PURPLE
                PREFER_GREEN  -> Name.PREFER_GREEN

                ANY_CLOSEST -> Name.ANY_CLOSEST
                else -> Name.NONE
            }
        }
        fun toInt(name: Name): Int
        {
            return when (name)
            {
                Name.PURPLE -> PURPLE
                Name.GREEN  -> GREEN

                Name.ANY_CLOSEST -> ANY_CLOSEST
                else -> NONE
            }
        }
    }
}