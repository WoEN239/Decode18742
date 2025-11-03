package org.woen.utils.units

class Triangle {
    val p1
        get() = l1.p1
    val p2
        get() = l2.p1
    val p3
        get() = l3.p1

    val points
        get() = arrayOf(p1, p2, p3)

    val l1: Line
    val l2: Line
    val l3: Line

    val lines
        get() = arrayOf(l1, l2, l3)

    constructor(p1: Vec2, p2: Vec2, p3: Vec2) : this(Line(p1, p2), Line(p2, p3), Line(p3, p1))

    constructor(l1: Line, l2: Line, l3: Line) {
        this.l1 = l1
        this.l2 = l2
        this.l3 = l3
    }

    fun isPointLocated(p: Vec2): Boolean {
        val denom = (p2.y - p3.y) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.y - p3.y)

        val u = ((p2.y - p3.y) * (p.x - p3.x) + (p3.x - p2.x) * (p.y - p3.y)) / denom

        val v = ((p3.y - p1.y) * (p.x - p3.x) + (p1.x - p3.x) * (p.y - p3.y)) / denom

        val w = 1 - u - v

        return u >= 0 && v >= 0 && w >= 0
    }
}