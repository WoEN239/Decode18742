package org.woen.modules.driveTrain.odometry

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.hotRun.HotRun
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.motor.EncoderOnly
import org.woen.utils.smartMutex.SmartMutex
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

        _filterMutex.smartLock {
            odometerVelocity.set(_filter.updateRaw(oldVelocity, rawVelocity - oldVelocity))
        }
    }

    private lateinit var _odometer: DcMotorEx

    val odometerPosition = AtomicReference(0.0)
    val odometerVelocity = AtomicReference(0.0)

    private var _oldOdometerPosition = 0.0
    private val _filter = ExponentialFilter(ThreadedConfigs.VELOCITY_FILTER_K.get())

    private val _filterMutex = SmartMutex()

    override fun init(hardwareMap: HardwareMap) {
        _odometer = EncoderOnly(hardwareMap.get(_odometryName) as DcMotorEx)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _filterMutex.smartLock {
                _filter.start()
            }
        }

        ThreadedConfigs.VELOCITY_FILTER_K.onSet += {
            _filterMutex.smartLock {
                _filter.coef = it
            }
        }
    }

    override fun dispose() {

    }
}