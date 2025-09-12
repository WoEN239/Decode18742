package org.woen.utils.units

import com.acmerobotics.roadrunner.Vector2d
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec2(@JvmField val x: Double, @JvmField val y: Double) {
    companion object {
        val ZERO = Vec2(0.0, 0.0)
    }

    constructor(rrVec: Vector2d) : this(rrVec.x, rrVec.y)
    constructor(x: Double) : this(x, x)

    fun length() = sqrt(x * x + y * y)

    fun rot() = atan2(y, x)

    fun setRot(rot: Double): Vec2 {
        val l = length()

        return Vec2(cos(rot) * l, sin(rot) * l)
    }

    fun turn(rot: Double): Vec2 {
        val currentRot = rot()

        return setRot(currentRot + rot)
    }

    fun normalized() = Vec2(1.0, 0.0).setRot(rot())

    fun rrVec() = Vector2d(x, y)

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        if (other !is Vec2)
            return false

        if (other.x == x && other.y == y)
            return true

        return false
    }

    operator fun plus(vec: Vec2) = Vec2(x + vec.x, y + vec.y)
    operator fun minus(vec: Vec2) = Vec2(x - vec.x, y - vec.y)
    operator fun times(vec: Vec2) = Vec2(x * vec.x, y * vec.y)
    operator fun div(vec: Vec2) = Vec2(x / vec.x, y / vec.y)

    operator fun times(value: Double) = Vec2(x * value, y * value)
    operator fun div(value: Double) = Vec2(x / value, y / value)
    operator fun plus(value: Double) = Vec2(x + value, y + value)
    operator fun minus(value: Double) = Vec2(x - value, y - value)

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    override fun toString(): String {
        return "($x, $y)"
    }
}