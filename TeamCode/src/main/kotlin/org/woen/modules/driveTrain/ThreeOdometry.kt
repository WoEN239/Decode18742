package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.motor.EncoderOnly
import java.util.concurrent.atomic.AtomicReference

class ThreeOdometry(val odometryName: String) : IHardwareDevice {
    override fun update() {
        odometerPosition.set(_odometer.currentPosition.toDouble())
    }

    private lateinit var _odometer: DcMotorEx

    val odometerPosition = AtomicReference(0.0)
    val odometerVelocity = AtomicReference(0.0)

    override fun init(hardwareMap: HardwareMap) {
        _odometer = EncoderOnly(hardwareMap.get(odometryName) as DcMotorEx)
    }

    override fun dispose() {

    }
}