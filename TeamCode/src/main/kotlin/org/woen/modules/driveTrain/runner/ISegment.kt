package org.woen.modules.driveTrain.runner

import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

interface ISegment {
    fun isEnd(time: Double): Boolean

    fun translateVelocity(time: Double): Vec2

    fun rotateVelocity(time: Double): Double

    fun targetOrientation(time: Double): Orientation
}