package org.woen.enumerators


import org.woen.enumerators.BallRequest.Companion.toBall
import org.woen.enumerators.BallRequest.Companion.isAbstractAny



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
    fun setUnknown() = set(UNKNOWN_COLOR, Name.UNKNOWN_COLOR)

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


    fun doesMatch(id:  Int)
        = !isEmpty() &&
            (
                isAbstractAny(id) ||
                hasBall(toBall(id))
            )
    fun doesMatch(name: BallRequest.Name)
        = !isEmpty() &&
            (
                isAbstractAny(name) ||
                hasBall(toBall(name))
            )



    companion object
    {
        const val PURPLE: Int = 0
        const val GREEN:  Int = 1

        const val UNKNOWN_COLOR: Int = 2
        const val NONE:          Int = 3


        fun toName(id:  Int): Name
        {
            return when (id)
            {
                PURPLE -> Name.PURPLE
                GREEN  -> Name.GREEN

                UNKNOWN_COLOR -> Name.UNKNOWN_COLOR
                else          -> Name.NONE
            }
        }
        fun toInt(name: Name): Int
        {
            return when (name)
            {
                Name.PURPLE -> PURPLE
                Name.GREEN  -> GREEN

                Name.UNKNOWN_COLOR -> UNKNOWN_COLOR
                Name.NONE          -> NONE
            }
        }

        fun toBallRequestName(name: Name): BallRequest.Name
        {
            return when (name)
            {
                Name.PURPLE -> BallRequest.Name.PURPLE
                Name.GREEN  -> BallRequest.Name.GREEN

                Name.UNKNOWN_COLOR -> BallRequest.Name.ANY_CLOSEST
                Name.NONE          -> BallRequest.Name.NONE
            }
        }
    }
}