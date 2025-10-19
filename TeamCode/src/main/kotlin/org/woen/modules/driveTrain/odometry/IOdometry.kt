package org.woen.modules.driveTrain.odometry

import kotlinx.coroutines.DisposableHandle
import org.woen.utils.units.Angle
import org.woen.utils.units.Vec2

data class OdometryTick(
    val deltaPos: Vec2,
    val positionVelocity: Vec2,
    val deltaRotation: Double,
    val rotationVelocity: Double
)

interface IOdometry : DisposableHandle {
    fun update(rotation: Angle): OdometryTick
}