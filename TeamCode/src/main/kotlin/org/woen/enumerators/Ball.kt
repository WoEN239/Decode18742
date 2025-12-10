package org.woen.enumerators



class Ball
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
        _id   = ball.id()
        _name = ball.name()
    }


    fun id()   = _id
    fun name() = _name


    fun isFilled() = _id != NONE
    fun isEmpty()  = _id == NONE


    fun hasBall(id:   Int)  = _id   == id
    fun hasBall(name: Name) = _name == name



    companion object
    {
        const val NONE:   Int = 0
        const val PURPLE: Int = 1
        const val GREEN:  Int = 2


        fun toName(id:  Int): Name
        {
            return when (id)
            {
                PURPLE -> Name.PURPLE
                GREEN  -> Name.GREEN
                else   -> Name.NONE
            }
        }
        fun toInt(name: Name): Int
        {
            return when (name)
            {
                Name.PURPLE -> PURPLE
                Name.GREEN  -> GREEN
                Name.NONE   -> NONE
            }
        }


        fun toBallRequestName(name: Name): BallRequest.Name
        {
            return when (name)
            {
                Name.PURPLE -> BallRequest.Name.PURPLE
                Name.GREEN  -> BallRequest.Name.GREEN
                Name.NONE   -> BallRequest.Name.NONE
            }
        }
    }
}