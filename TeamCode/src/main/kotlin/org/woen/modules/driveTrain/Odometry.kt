package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.motor.EncoderOnly
import java.util.concurrent.atomic.AtomicReference

class Odometry(val leftOdometerName: String, val rightOdometerName: String): IHardwareDevice {
    private lateinit var _leftOdometer: DcMotorEx
    private lateinit var _rightOdometer: DcMotorEx

    private val _leftFilter = ExponentialFilter(ThreadedConfigs.VELOCITY_FILTER_K.get())
    private val _rightFilter = ExponentialFilter(ThreadedConfigs.VELOCITY_FILTER_K.get())

    val leftPosition = AtomicReference(0.0)
    val rightPosition = AtomicReference(0.0)

    val leftVelocity = AtomicReference(0.0)
    val rightVelocity = AtomicReference(0.0)

    private var _oldLeftPosition = 0.0
    private var _oldRightPosition = 0.0

    override fun update() {
        val currentLeftPosition = _leftOdometer.currentPosition.toDouble()
        val currentRightPosition = _rightOdometer.currentPosition.toDouble()

        leftPosition.set(currentLeftPosition)
        rightPosition.set(currentRightPosition)

        val rawLeftVelocity = currentLeftPosition - _oldLeftPosition
        val rawRightVelocity = currentRightPosition - _oldRightPosition

        val oldLeftVelocity = leftVelocity.get()
        val oldRightVelocity = rightVelocity.get()

        leftVelocity.set(_leftFilter.updateRaw(oldLeftVelocity, rawLeftVelocity - oldLeftVelocity))
        rightVelocity.set(_rightFilter.updateRaw(oldRightVelocity, rawRightVelocity - oldRightVelocity))

        _oldRightPosition = currentRightPosition
        _oldLeftPosition = currentLeftPosition
    }

    override fun init(hardwareMap: HardwareMap) {
        _leftOdometer = EncoderOnly(hardwareMap.get(leftOdometerName) as DcMotorEx)
        _rightOdometer = EncoderOnly(hardwareMap.get(rightOdometerName) as DcMotorEx)

        ThreadedConfigs.VELOCITY_FILTER_K.onSet += {
            _rightFilter.coef = it
            _leftFilter.coef = it
        }

        _rightFilter.start()
        _leftFilter.start()
    }

    override fun dispose() {

    }
}