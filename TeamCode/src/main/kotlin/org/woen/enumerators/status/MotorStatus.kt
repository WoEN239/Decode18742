package org.woen.enumerators.status


class MotorStatus
{
    private var _name : Name

    constructor(name: Name = Name.IDLE) { _name = name }


    enum class Name
    {
        IDLE,

        FORWARD_TIME,
        REVERSE_TIME,

        FORWARD_INFINITE,
        REVERSE_INFINITE,
    }


    val name get() = _name


    fun setIdle()
    {
        _name = Name.IDLE
    }
    fun setForward(onTime: Boolean)
    {
        _name = if (onTime) Name.FORWARD_TIME
                else Name.FORWARD_INFINITE
    }
    fun setReverse(onTime: Boolean)
    {
        _name = if (onTime) Name.REVERSE_TIME
        else Name.REVERSE_INFINITE
    }



    fun isForward() = _name == Name.FORWARD_INFINITE || isForwardOnTime()
    fun isReverse() = _name == Name.REVERSE_INFINITE || isReverseOnTime()
    fun isForwardOnTime() = _name == Name.FORWARD_TIME
    fun isReverseOnTime() = _name == Name.REVERSE_TIME
    fun isOnTime() = isForwardOnTime() || isReverseOnTime()

    fun isIdle() = _name == Name.IDLE

    fun notOnTime() = !isOnTime()
}
