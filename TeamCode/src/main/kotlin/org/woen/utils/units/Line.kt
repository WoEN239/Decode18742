package org.woen.utils.units

data class Line(@JvmField val p1: Vec2, @JvmField val p2: Vec2) {
    fun isIntersects(l: Line) = k != l.k

    fun getIntersects(l: Line): Vec2 {
        val x = (l.b - b) / (k - l.k)

        return Vec2(x, k * x + b)
    }

    fun isPointOnLine(p: Vec2) = (p.x - p1.x) * (p.x - p2.x) <= 0 &&
            (p.y - p1.y) * (p.y - p2.y) <= 0

    val b: Double
    val k: Double

    init {
        b = (p2.y * p1.x - p2.x * p1.y) / (p1.x - p2.x)
        k = (p1.y - b) / p1.x
    }
}