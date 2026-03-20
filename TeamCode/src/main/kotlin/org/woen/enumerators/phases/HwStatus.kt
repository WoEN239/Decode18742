package org.woen.enumerators.phases


enum class MotorStatus
{
    IDLE,
    FORWARD,
    REVERSE,
    LAZY_FORWARD,
    LAZY_REVERSE
}



class ServoStatus
{
    private var _name : Name

    constructor(name: Name = Name.CLOSING) { _name = name }


    enum class Name
    {
        CLOSED,
        CLOSING,

        OPENED,
        OPENING,
    }

    val name get() = _name
    fun set (name: Name)
    {
        _name = name
    }
    fun tryUpdate(b: Boolean) { if (b) updateToFinished() }
    fun updateToFinished()
    {
        _name = when (_name)
        {
            Name.OPENING -> Name.OPENED
            Name.CLOSING -> Name.CLOSED
            else -> _name
        }
    }


    fun isClosed() = _name == Name.CLOSED
    fun isClosingOrClosed() = isClosed()  || _name == Name.CLOSING
    fun isFinished()    = _name == Name.CLOSED  || _name == Name.OPENED
    fun isNotFinished() = _name == Name.CLOSING || _name == Name.OPENING
}