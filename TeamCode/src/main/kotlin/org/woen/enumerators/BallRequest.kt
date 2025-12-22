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
        PURPLE,
        GREEN,

        PREFER_PURPLE,
        PREFER_GREEN,
        ANY_CLOSEST,

        NONE
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
    fun isAbstractAny() = _id > GREEN && _id < NONE


    fun toBall(): Ball.Name
    {
        return when (_id)
        {
            PURPLE, PREFER_PURPLE -> Ball.Name.PURPLE
            GREEN,  PREFER_GREEN  -> Ball.Name.GREEN

            ANY_CLOSEST -> Ball.Name.UNKNOWN_COLOR
            else        -> Ball.Name.NONE
        }
    }



    companion object
    {
        const val PURPLE: Int = 0
        const val GREEN:  Int = 1

        const val PREFER_PURPLE: Int = 2
        const val PREFER_GREEN:  Int = 3
        const val ANY_CLOSEST:   Int = 4

        const val NONE: Int = 5


        fun isAbstractAny(id:   Int)  = id > GREEN && id < NONE
        fun isAbstractAny(name: Name) = isAbstractAny(toInt(name))


        fun isNone(id:   Int)  = id   == NONE
        fun isNone(name: Name) = name == Name.NONE


        fun toName(id:  Int): Name
        {
            return when (id)
            {
                PURPLE -> Name.PURPLE
                GREEN  -> Name.GREEN

                PREFER_PURPLE -> Name.PREFER_PURPLE
                PREFER_GREEN  -> Name.PREFER_GREEN

                ANY_CLOSEST -> Name.ANY_CLOSEST
                else        -> Name.NONE
            }
        }
        fun toInt(name: Name): Int
        {
            return when (name)
            {
                Name.PURPLE -> PURPLE
                Name.GREEN  -> GREEN

                Name.PREFER_PURPLE -> PREFER_PURPLE
                Name.PREFER_GREEN  -> PREFER_GREEN

                Name.ANY_CLOSEST -> ANY_CLOSEST
                Name.NONE        -> NONE
            }
        }

        fun toBall(id: Int): Ball.Name
        {
            return when (id)
            {
                PURPLE, PREFER_PURPLE -> Ball.Name.PURPLE
                GREEN,  PREFER_GREEN  -> Ball.Name.GREEN

                ANY_CLOSEST -> Ball.Name.UNKNOWN_COLOR
                else        -> Ball.Name.NONE
            }
        }
        fun toBall(name: Name): Ball.Name
        {
            return when (name)
            {
                Name.PURPLE, Name.PREFER_PURPLE -> Ball.Name.PURPLE
                Name.GREEN,  Name.PREFER_GREEN  -> Ball.Name.GREEN

                Name.ANY_CLOSEST -> Ball.Name.UNKNOWN_COLOR
                Name.NONE        -> Ball.Name.NONE
            }
        }

        fun toAbstractBallId(name: Name): Int
        {
            return when (name)
            {
                Name.PURPLE -> Ball.PURPLE
                Name.GREEN  -> Ball.GREEN

                Name.PREFER_PURPLE,
                Name.PREFER_GREEN,
                Name.ANY_CLOSEST -> Ball.UNKNOWN_COLOR

                Name.NONE        -> Ball.NONE
            }
        }
    }
}