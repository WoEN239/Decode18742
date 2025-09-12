package org.woen.utils.units

data class Orientation(var pos: Vec2, var angl: Angle) {
    companion object {
        val ZERO = Orientation(0.0)
    }

    val x
        get() = pos.x

    val y
        get() = pos.y

    constructor(x: Double) : this(Vec2(x), Angle(x))
    constructor(x: Vec2) : this(x, Angle.ZERO)
    constructor(x: Angle) : this(Vec2.ZERO, x)
    constructor() : this(Vec2.ZERO, Angle.ZERO)

    operator fun plus(orientation: Orientation) =
        Orientation(pos + orientation.pos, angl + orientation.angl)

    operator fun plus(orientation: Vec2) = Orientation(pos + orientation, angl)
    operator fun plus(orientation: Angle) = Orientation(pos, angl + orientation)

    operator fun minus(orientation: Orientation) =
        Orientation(pos - orientation.pos, angl - orientation.angl)

    operator fun minus(orientation: Vec2) = Orientation(pos - orientation, angl)
    operator fun minus(orientation: Angle) = Orientation(pos, angl - orientation)

    operator fun times(orientation: Orientation) =
        Orientation(pos * orientation.pos, angl * orientation.angl)

    operator fun times(orientation: Vec2) = Orientation(pos * orientation, angl)
    operator fun times(orientation: Angle) = Orientation(pos, angl * orientation)

    operator fun div(orientation: Orientation) =
        Orientation(pos / orientation.pos, angl / orientation.angl)

    operator fun div(orientation: Vec2) = Orientation(pos / orientation, angl)
    operator fun div(orientation: Angle) = Orientation(pos, angl / orientation)

    override fun toString(): String {
        return "$pos $angl"
    }

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        if (other is Orientation && other.pos == pos && other.angl == angl)
            return true

        return false
    }

    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + angl.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}