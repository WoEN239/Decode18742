package org.woen.enumerators



enum class BallRequest
{
    PURPLE,
    GREEN,

    PREFER_PURPLE,
    PREFER_GREEN,
    ANY_CLOSEST,

    NONE
}


class Ball
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

        UNKNOWN_COLOR,
        NONE
    }


    fun empty() = set(NONE, Name.NONE)

    fun set(name: Name,  id: Int  = toInt(name)) = set(id, name)
    fun set(id:   Int, name: Name = toName(id))
    {
        _id   = id
        _name = name
    }
    fun set(ball: Ball)
    {
        _id   = ball.id
        _name = ball.name
    }


    val id   get() = _id
    val name get() = _name

    fun formattedName(): String
    {
        return when (_name)
        {
            Name.PURPLE        -> "+ PURPLE +"
            Name.GREEN         -> "+ GREEN + "

            Name.UNKNOWN_COLOR -> "  + ?? +  "
            Name.NONE          -> "    --    "
        }
    }


    fun isFilled() = _id != NONE
    fun isEmpty()  = _id == NONE


    fun hasBall(name: Name) = _name == name


    fun isPseudoMatch(name: BallRequest)
        = !isEmpty() &&
            (
                isAbstractAny(name) ||
                hasBall(toBall(name))
            )
    fun isTrueMatch(name: BallRequest)
        = !isEmpty() && hasBall(toBall(name))



    companion object
    {
        const val PURPLE: Int = 0
        const val GREEN:  Int = 1

        const val UNKNOWN_COLOR: Int = 2
        const val NONE:          Int = 3


        fun toName(id:  Int): Name
            = when (id)
            {
                PURPLE -> Name.PURPLE
                GREEN  -> Name.GREEN

                UNKNOWN_COLOR -> Name.UNKNOWN_COLOR
                else          -> Name.NONE
            }
        fun toInt(name: Name): Int
            = when (name)
            {
                Name.PURPLE -> PURPLE
                Name.GREEN  -> GREEN

                Name.UNKNOWN_COLOR -> UNKNOWN_COLOR
                Name.NONE          -> NONE
            }
        fun toBall(name: BallRequest): Name
            = when (name)
                {
                    BallRequest.PURPLE, BallRequest.PREFER_PURPLE -> Name.PURPLE
                    BallRequest.GREEN,  BallRequest.PREFER_GREEN  -> Name.GREEN

                    BallRequest.ANY_CLOSEST -> Name.UNKNOWN_COLOR
                    BallRequest.NONE        -> Name.NONE
                }

        fun isAbstractAny(name: BallRequest)
            =   name == BallRequest.PREFER_PURPLE ||
                name == BallRequest.PREFER_GREEN  ||
                name == BallRequest.ANY_CLOSEST
    }
}