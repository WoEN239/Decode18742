package org.woen.utils.units

class Triangle {
    private val _p1: Vec2
    private val _p2: Vec2
    private val _p3: Vec2

    constructor(p1: Vec2, p2: Vec2, p3: Vec2){
        _p1 = p1
        _p2 = p2
        _p3 = p3
    }

    constructor(l1: Line, l2: Line, l3: Line) : this(l1.p1, l1.p2, l2.p2)

    fun isPointLocated(p: Vec2): Boolean{
        val denom = (_p2.y - _p3.y) * (_p1.x - _p3.x) + (_p3.x - _p2.x) * (_p1.y - _p3.y)

        val u = ((_p2.y - _p3.y) * (p.x - _p3.x) + (_p3.x - _p2.x) * (p.y - _p3.y)) / denom

        val v = ((_p3.y - _p1.y) * (p.x - _p3.x) + (_p1.x - _p3.x) * (p.y - _p3.y)) / denom

        val w = 1 - u - v

        return u >= 0 && v >= 0 && w >= 0
    }
}