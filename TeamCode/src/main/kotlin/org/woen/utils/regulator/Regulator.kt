package org.woen.utils.regulator

import com.acmerobotics.roadrunner.clamp
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.threading.hardware.ThreadedBattery
import kotlin.math.pow
import kotlin.math.sign

data class RegulatorParameters(
    @JvmField val kP: Double = 0.0,
    @JvmField val kD: Double = 0.0,
    @JvmField val kI: Double = 0.0,
    @JvmField val kPow: Double = 0.0,
    @JvmField val kF: Double = 0.0,
    @JvmField val kG: Double = 0.0,
    @JvmField val kSG: Double = 0.0,
    @JvmField val limitU: Double = -1.0,
    @JvmField val resetI: Boolean = false
)

class Regulator(val parameters: RegulatorParameters) {
    private val _deltaTime = ElapsedTime()

    private var _integral = 0.0
    private var _errOld = 0.0

    @Synchronized
    fun start() {
        _deltaTime.reset()
    }

    @Synchronized
    fun update(err: Double, target: Double): Double {
        val uP = err * parameters.kP

        val uD = (err - _errOld) / _deltaTime.seconds() * parameters.kD

        val uF = target * parameters.kF

        val uG = parameters.kG

        val uSG = parameters.kSG * sign(target)

        val uPow = err.pow(2.0) * parameters.kPow * sign(err)

        val uI = _integral * parameters.kI

        var u = uP + uI + uD + uF + uG + uSG + uPow

        if (err * _errOld < 0.0f && parameters.resetI)
            resetIntegral()

        val volts = ThreadedBattery.LAZY_INSTANCE.currentVoltage

        if (
            (parameters.limitU > 0.0 && u < parameters.limitU && u > -parameters.limitU) ||
            (parameters.limitU < 0.0 && u < volts && u > -volts) ||
            (err * u < 0.0f)
        )
            _integral += err * _deltaTime.seconds()

        _deltaTime.reset()
        _errOld = err

        if(parameters.limitU >= 0.0)
            u = clamp(u, -parameters.limitU, parameters.limitU)

        return u
    }

    @Synchronized
    fun resetIntegral() {
        _integral = 0.0
    }

    fun update(err: Double) = update(err, 0.0)
}