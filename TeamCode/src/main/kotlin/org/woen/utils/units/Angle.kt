package org.woen.utils.units

import android.annotation.SuppressLint
import org.woen.utils.units.Angle.Companion.chop
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

data class Angle(@JvmField var angle: Double) {
    companion object {
        fun chop(ang: Double): Double {
            var chopedAng = ang

            while (abs(chopedAng) > PI)
                chopedAng -= 2 * PI * sign(chopedAng)

            return chopedAng
        }

        val ZERO
            get() = Angle(0.0)

        fun ofDeg(angle: Double) = Angle(angle / 180.0 * PI)
    }

    fun ofDegree() = angle / PI * 180

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        if (other is Angle && abs(other.angle - angle) < 0.05)
            return true

        return false
    }

    override fun hashCode(): Int {
        return angle.hashCode()
    }

    operator fun plus(ang: Angle) = Angle(chop(angle + ang.angle))
    operator fun plus(ang: Double) = Angle(chop(angle + ang))

    operator fun minus(ang: Angle) = Angle(chop(angle - ang.angle))
    operator fun minus(ang: Double) = Angle(chop(angle - ang))

    operator fun times(ang: Angle) = Angle(chop(angle * ang.angle))
    operator fun times(ang: Double) = Angle(chop(angle * ang))

    operator fun div(ang: Angle) = Angle(chop(angle / ang.angle))
    operator fun div(ang: Double) = Angle(chop(angle / ang))

    @SuppressLint("DefaultLocale")
    override fun toString(): String {
        return String.format("%.3f", ofDegree()) + "°"
    }

    init {
        angle = chop(angle)
    }

    fun clone() = Angle(angle)
}

operator fun Double.plus(ang: Angle) = Angle(chop(this + ang.angle))
operator fun Double.minus(ang: Angle) = Angle(chop(this - ang.angle))
operator fun Double.times(ang: Angle) = Angle(chop(this * ang.angle))
operator fun Double.div(ang: Angle) = Angle(chop(this / ang.angle))