package org.woen.utils.regulator

import com.acmerobotics.roadrunner.clamp
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.ThreadedBattery
import java.util.concurrent.atomic.AtomicReference
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

class Regulator(val parameters: ThreadedTelemetry.AtomicValueProvider<RegulatorParameters>) {
    private val _deltaTime = ElapsedTime()

    private var _integral = 0.0
    private var _errOld = 0.0

    @Synchronized
    fun start() {
        _deltaTime.reset()
    }

    @Synchronized
    fun update(err: Double, target: Double): Double {
        val uP = err * parameters.get().kP

        val uD = (err - _errOld) / _deltaTime.seconds() * parameters.get().kD

        val uF = target * parameters.get().kF

        val uG = parameters.get().kG

        val uSG = parameters.get().kSG * sign(target)

        val uPow = err.pow(2.0) * parameters.get().kPow * sign(err)

        val uI = _integral * parameters.get().kI

        var u = uP + uI + uD + uF + uG + uSG + uPow

        if (err * _errOld < 0.0f && parameters.get().resetI)
            resetIntegral()

        val volts = ThreadedBattery.LAZY_INSTANCE.currentVoltage

        val limitU = parameters.get().limitU

        if (
            (limitU > 0.0 && u < limitU && u > -limitU) ||
            (limitU < 0.0 && u < volts && u > -volts) ||
            (err * u < 0.0f)
        )
            _integral += err * _deltaTime.seconds()

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