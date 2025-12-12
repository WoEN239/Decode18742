package org.woen.utils.regulator

import com.acmerobotics.roadrunner.clamp
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.threading.hardware.ThreadedBattery
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

data class RegulatorParameters(
    @JvmField var kP: Double = 0.0,
    @JvmField var kD: Double = 0.0,
    @JvmField var kI: Double = 0.0,
    @JvmField var kPow: Double = 0.0,
    @JvmField var kF: Double = 0.0,
    @JvmField var kG: Double = 0.0,
    @JvmField var kSG: Double = 0.0,
    @JvmField var limitU: Double = -1.0,
    @JvmField var resetI: Boolean = false
)

class Regulator(val parameters: RegulatorParameters) {
    private val _deltaTime = ElapsedTime()

    private var _integral = 0.0
    private var _errOld = 0.0
    private var _oldKi = parameters.kI

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

        val limitU = parameters.limitU

        if (
            (limitU > 0.0 && u < limitU && u > -limitU) ||
            (limitU < 0.0 && u < volts && u > -volts) ||
            (err * u < 0.0f)
        )
            _integral += err * _deltaTime.seconds()

        if (abs(_oldKi - parameters.kI) > 0.00001)
            _integral = 0.0

        _oldKi = parameters.kI

        _deltaTime.reset()
        _errOld = err

        if (limitU >= 0.0)
            u = clamp(u, -limitU, limitU)

        return u
    }

    @Synchronized
    fun resetIntegral() {
        _integral = 0.0
    }

    fun update(err: Double) = update(err, 0.0)
}