package org.woen.enumerators.status


import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sign
import kotlin.math.sqrt
import org.woen.configs.Hardware



class MotorStatus
{
    private var _name : Name
     var _accelerationPhase: Double = 0.0  // 0 = stop, Positive = Forward, Negative = Backward
    var motorSpeed: Double = 0.0


    constructor(name: Name = Name.IDLE, motorSpeed: Double = 0.0)
    {
        _name = name
        this.motorSpeed = motorSpeed
    }


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
        _accelerationPhase = 0.0
        motorSpeed = 0.0
    }
    fun setForward(onTime: Boolean)
    {
        _name = if (onTime) Name.FORWARD_TIME
                else Name.FORWARD_INFINITE
        if (_accelerationPhase <= 0) _accelerationPhase = 1.0
    }
    fun setReverse(onTime: Boolean)
    {
        _name = if (onTime) Name.REVERSE_TIME
        else Name.REVERSE_INFINITE
        if (_accelerationPhase >= 0) _accelerationPhase = -1.0
    }


    val motorSpeedForAcceleration: Double get()
    {
        val k = Hardware.MOTOR.ACC_PHASE_K
        val phase = _accelerationPhase / Hardware.MOTOR.ACCELERATION_PHASES_LIMIT
        return (min(
            1.0,
            max(
                abs (sin(1.5 * phase)),
                sqrt(abs(phase))
            )   ) + k - 1) * motorSpeed / k
    }
    fun updateAccelerationPhase()
    {
        _accelerationPhase += sign(_accelerationPhase)
    }


    fun isForwardOnTime() = _name == Name.FORWARD_TIME
    fun isReverseOnTime() = _name == Name.REVERSE_TIME
    fun isOnTime() = isForwardOnTime() || isReverseOnTime()

    fun isIdle() = _name == Name.IDLE

    fun notOnTime() = !isOnTime()
}
