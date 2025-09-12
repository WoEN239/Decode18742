package org.woen.modules.runner.segment

import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

interface ISegment {
    fun isEnd(time: Double): Boolean

    fun translateVelocity(time: Double): Vec2

    fun rotateVelocity(time: Double): Double

    fun targetOrientation(time: Double): Orientation

    fun duration(): Double
}