package org.woen.modules.driveTrain.odometry.odometersOdometry

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.motor.EncoderOnly
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI

class HardwareOdometersOdometry(
    private val _leftOdometerName: String,
    private val _rightOdometerName: String
) : IHardwareDevice {
    private lateinit var _leftOdometer: EncoderOnly
    private lateinit var _rightOdometer: EncoderOnly

    private val _filterMutex = SmartMutex()

    private val _leftFilter = ExponentialFilter(Configs.ODOMETRY.VELOCITY_FILTER_K.get())
    private val _rightFilter = ExponentialFilter(Configs.ODOMETRY.VELOCITY_FILTER_K.get())

    val leftPosition = AtomicReference(0.0)
    val rightPosition = AtomicReference(0.0)

    val leftVelocity = AtomicReference(0.0)
    val rightVelocity = AtomicReference(0.0)

    private var _oldLeftPosition = 0.0
    private var _oldRightPosition = 0.0

    override fun update() {
        if(HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
            return

        val currentLeftPosition = (_leftOdometer.currentPosition.toDouble() /
                Configs.ODOMETRY.ODOMETRY_TICKS) * PI * Configs.ODOMETRY.ODOMETRY_DIAMETER
        val currentRightPosition = (_rightOdometer.currentPosition.toDouble() /
                Configs.ODOMETRY.ODOMETRY_TICKS) * PI * Configs.ODOMETRY.ODOMETRY_DIAMETER

        leftPosition.set(currentLeftPosition)
        rightPosition.set(currentRightPosition)

        val rawLeftVelocity = currentLeftPosition - _oldLeftPosition
        val rawRightVelocity = currentRightPosition - _oldRightPosition

        val oldLeftVelocity = leftVelocity.get()
        val oldRightVelocity = rightVelocity.get()

        _filterMutex.smartLock {
            leftVelocity.set(
                _leftFilter.updateRaw(
                    oldLeftVelocity,
                    rawLeftVelocity - oldLeftVelocity
                )
            )
            rightVelocity.set(
                _rightFilter.updateRaw(
                    oldRightVelocity,
                    rawRightVelocity - oldRightVelocity
                )
            )
        }

        _oldRightPosition = currentRightPosition
        _oldLeftPosition = currentLeftPosition
    }

    override fun init(hardwareMap: HardwareMap) {
        _leftOdometer = EncoderOnly(hardwareMap.get(_leftOdometerName) as DcMotorEx)
        _rightOdometer = EncoderOnly(hardwareMap.get(_rightOdometerName) as DcMotorEx)

        Configs.ODOMETRY.VELOCITY_FILTER_K.onSet += {
            _filterMutex.smartLock {
                _rightFilter.coef = it
                _leftFilter.coef = it
            }
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _leftOdometer.direction = DcMotorSimple.Direction.REVERSE
            _rightOdometer.direction = DcMotorSimple.Direction.REVERSE
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _filterMutex.smartLock {
                _rightFilter.start()
                _leftFilter.start()
            }

            if(HotRun.LAZY_INSTANCE.currentRunMode.get() == HotRun.RunMode.AUTO){
                _leftOdometer.resetEncoder()
                _rightOdometer.resetEncoder()
            }
        }
    }

    override fun dispose() {

    }
}