package org.woen.modules.driveTrain.odometry.pinpointOdometry

import org.woen.hotRun.HotRun
import org.woen.modules.driveTrain.odometry.IOdometry
import org.woen.modules.driveTrain.odometry.OdometryTick
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Angle

class PinpointOdometry : IOdometry {
    val hardwarePinpoint = HardwarePinpointOdometry("")

    private var _oldOrientation = HotRun.LAZY_INSTANCE.currentRunColor.get().startOrientation

    override fun update(rotation: Angle): OdometryTick {
        val currentOrientation = hardwarePinpoint.orientation.get()

        val deltaOrientation = currentOrientation - _oldOrientation

        _oldOrientation = currentOrientation

        return OdometryTick(
            deltaOrientation.pos,
            hardwarePinpoint.positionVelocity.get(),
            deltaOrientation.angle,
            hardwarePinpoint.headingVelocity.get()
        )
    }

    override fun dispose() {

    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(hardwarePinpoint)
    }
}