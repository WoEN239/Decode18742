package org.woen.enumerators.status


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


    fun isOpened() = _name == Name.OPENED
    fun isClosed() = _name == Name.CLOSED
    fun isClosingOrClosed() = isClosed()  || _name == Name.CLOSING
    fun isFinished()    = _name == Name.CLOSED  || _name == Name.OPENED
}