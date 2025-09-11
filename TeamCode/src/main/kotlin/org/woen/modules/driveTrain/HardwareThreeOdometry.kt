package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.motor.EncoderOnly
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI

class HardwareThreeOdometry(private val _odometryName: String) : IHardwareDevice {
    override fun update() {
        val currentOdometerPosition = _odometer.currentPosition.toDouble() /
                ThreadedConfigs.ODOMETRY_TICKS.get() * PI * ThreadedConfigs.ODOMETRY_DIAMETER.get()

        odometerPosition.set(currentOdometerPosition)

        val rawVelocity = currentOdometerPosition - _oldOdometerPosition
        val oldVelocity = odometerVelocity.get()

        _oldOdometerPosition = currentOdometerPosition

        odometerVelocity.set(_filter.updateRaw(oldVelocity, rawVelocity - oldVelocity))
    }

    private lateinit var _odometer: DcMotorEx

    val odometerPosition = AtomicReference(0.0)
    val odometerVelocity = AtomicReference(0.0)

    private var _oldOdometerPosition = 0.0
    private val _filter = ExponentialFilter(ThreadedConfigs.VELOCITY_FILTER_K.get())

    override fun init(hardwareMap: HardwareMap) {
        _odometer = EncoderOnly(hardwareMap.get(_odometryName) as DcMotorEx)
        _filter.start()

        ThreadedConfigs.VELOCITY_FILTER_K.onSet += {
            _filter.coef = it
        }
    }

    override fun dispose() {

    }
}