package org.woen.modules.driveTrain.odometry

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.hotRun.HotRun
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.motor.EncoderOnly
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI

class HardwareOdometry(
    private val _leftOdometerName: String,
    private val _rightOdometerName: String
) : IHardwareDevice {
    private lateinit var _leftOdometer: DcMotorEx
    private lateinit var _rightOdometer: DcMotorEx

    private val _filterMutex = Mutex()

    private val _leftFilter = ExponentialFilter(ThreadedConfigs.VELOCITY_FILTER_K.get())
    private val _rightFilter = ExponentialFilter(ThreadedConfigs.VELOCITY_FILTER_K.get())

    val leftPosition = AtomicReference(0.0)
    val rightPosition = AtomicReference(0.0)

    val leftVelocity = AtomicReference(0.0)
    val rightVelocity = AtomicReference(0.0)

    private var _oldLeftPosition = 0.0
    private var _oldRightPosition = 0.0

    override fun update() {
        val currentLeftPosition = _leftOdometer.currentPosition.toDouble() /
                ThreadedConfigs.ODOMETRY_TICKS.get() * PI * ThreadedConfigs.ODOMETRY_DIAMETER.get()
        val currentRightPosition = _rightOdometer.currentPosition.toDouble() /
                ThreadedConfigs.ODOMETRY_TICKS.get() * PI * ThreadedConfigs.ODOMETRY_DIAMETER.get()

        leftPosition.set(currentLeftPosition)
        rightPosition.set(currentRightPosition)

        val rawLeftVelocity = currentLeftPosition - _oldLeftPosition
        val rawRightVelocity = currentRightPosition - _oldRightPosition

        val oldLeftVelocity = leftVelocity.get()
        val oldRightVelocity = rightVelocity.get()

        runBlocking {
            _filterMutex.withLock {
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
        }

        _oldRightPosition = currentRightPosition
        _oldLeftPosition = currentLeftPosition
    }

    override fun init(hardwareMap: HardwareMap) {
        _leftOdometer = EncoderOnly(hardwareMap.get(_leftOdometerName) as DcMotorEx)
        _rightOdometer = EncoderOnly(hardwareMap.get(_rightOdometerName) as DcMotorEx)

        ThreadedConfigs.VELOCITY_FILTER_K.onSet += {
            runBlocking {
                _filterMutex.withLock {
                    _rightFilter.coef = it
                    _leftFilter.coef = it
                }
            }
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            runBlocking {
                _filterMutex.withLock {
                    _rightFilter.start()
                    _leftFilter.start()
                }
            }
        }
    }

    override fun dispose() {

    }
}