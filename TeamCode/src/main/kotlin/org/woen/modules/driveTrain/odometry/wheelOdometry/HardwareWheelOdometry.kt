package org.woen.modules.driveTrain.odometry.wheelOdometry

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.motor.EncoderOnly
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI

class HardwareWheelOdometry(
    private val _leftForwardEncoderName: String,
    private val _rightForwardEncoderName: String,
    private val _leftBackEncoderName: String,
    private val _rightBackEncoderName: String
) : IHardwareDevice {
    private lateinit var _leftForwardEncoder: EncoderOnly
    private lateinit var _rightForwardEncoder: EncoderOnly
    private lateinit var _leftBackEncoder: EncoderOnly
    private lateinit var _rightBackEncoder: EncoderOnly

    var leftForwardPosition = AtomicReference(0.0)
    var rightForwardPosition = AtomicReference(0.0)
    var leftBackPosition = AtomicReference(0.0)
    var rightBackPosition = AtomicReference(0.0)

    private var _oldLeftForwardPosition = 0.0
    private var _oldRightForwardPosition = 0.0
    private var _oldLeftBackPosition = 0.0
    private var _oldRightBackPosition = 0.0

    var leftForwardVelocity = AtomicReference(0.0)
    var rightForwardVelocity = AtomicReference(0.0)
    var leftBackVelocity = AtomicReference(0.0)
    var rightBackVelocity = AtomicReference(0.0)

    private var _leftForwardFilter = ExponentialFilter(Configs.DRIVE_TRAIN.ENCODER_VELOCITY_FILTER_K.get())
    private var _rightForwardFilter = ExponentialFilter(Configs.DRIVE_TRAIN.ENCODER_VELOCITY_FILTER_K.get())
    private var _leftBackFilter = ExponentialFilter(Configs.DRIVE_TRAIN.ENCODER_VELOCITY_FILTER_K.get())
    private var _rightBackFilter = ExponentialFilter(Configs.DRIVE_TRAIN.ENCODER_VELOCITY_FILTER_K.get())

    private val _filterMutex = SmartMutex()

    private val _deltaTime = ElapsedTime()

    override fun update() {
        val currentLeftForwardPosition = _leftForwardEncoder.currentPosition.toDouble() /
                Configs.DRIVE_TRAIN.ENCODER_TICKS * PI * Configs.DRIVE_TRAIN.WHEEL_DIAMETER

        leftForwardPosition.set(currentLeftForwardPosition)

        val currentRightForwardPosition = _rightForwardEncoder.currentPosition.toDouble() /
                Configs.DRIVE_TRAIN.ENCODER_TICKS * PI * Configs.DRIVE_TRAIN.WHEEL_DIAMETER

        rightForwardPosition.set(currentRightForwardPosition)

        val currentLeftBackPosition = _leftBackEncoder.currentPosition.toDouble() /
                Configs.DRIVE_TRAIN.ENCODER_TICKS * PI * Configs.DRIVE_TRAIN.WHEEL_DIAMETER

        leftBackPosition.set(currentLeftBackPosition)

        val currentRightBackPosition = _rightBackEncoder.currentPosition.toDouble() /
                Configs.DRIVE_TRAIN.ENCODER_TICKS * PI * Configs.DRIVE_TRAIN.WHEEL_DIAMETER

        rightBackPosition.set(currentRightBackPosition)

        val oldLeftForwardVelocity = leftForwardVelocity.get()
        val oldRightForwardVelocity = rightForwardVelocity.get()
        val oldLeftBackVelocity = leftBackVelocity.get()
        val oldRightBackVelocity = rightBackVelocity.get()

        val rawLeftForwardVelocity = (currentLeftForwardPosition - _oldLeftForwardPosition) / _deltaTime.seconds()
        val rawRightForwardVelocity = (currentRightForwardPosition - _oldRightForwardPosition) / _deltaTime.seconds()
        val rawLeftBackVelocity = (currentLeftBackPosition - _oldLeftBackPosition) / _deltaTime.seconds()
        val rawRightBackVelocity = (currentRightBackPosition - _oldRightBackPosition) / _deltaTime.seconds()

        _filterMutex.smartLock {
            leftForwardVelocity.set(_leftForwardFilter.updateRaw(oldLeftForwardVelocity, rawLeftForwardVelocity - oldLeftForwardVelocity))
            rightForwardVelocity.set(_rightForwardFilter.updateRaw(oldRightForwardVelocity, rawRightForwardVelocity - oldRightForwardVelocity))
            leftBackVelocity.set(_leftBackFilter.updateRaw(oldLeftBackVelocity, rawLeftBackVelocity - oldLeftBackVelocity))
            rightBackVelocity.set(_rightBackFilter.updateRaw(oldRightBackVelocity, rawRightBackVelocity - oldRightBackVelocity))
        }

        _oldLeftForwardPosition = currentLeftForwardPosition
        _oldRightForwardPosition = currentRightForwardPosition
        _oldLeftBackPosition = currentLeftBackPosition
        _oldRightBackPosition = currentRightBackPosition

        _deltaTime.reset()
    }

    override fun init(hardwareMap: HardwareMap) {
        _leftForwardEncoder = EncoderOnly(hardwareMap.get(_leftForwardEncoderName) as DcMotorEx)
        _rightForwardEncoder = EncoderOnly(hardwareMap.get(_rightForwardEncoderName) as DcMotorEx)
        _leftBackEncoder = EncoderOnly(hardwareMap.get(_leftBackEncoderName) as DcMotorEx)
        _rightBackEncoder = EncoderOnly(hardwareMap.get(_rightBackEncoderName) as DcMotorEx)

        Configs.DRIVE_TRAIN.ENCODER_VELOCITY_FILTER_K.onSet += {
            _filterMutex.smartLock {
                _leftForwardFilter.coef = it
                _rightForwardFilter.coef = it
                _leftBackFilter.coef = it
                _rightBackFilter.coef = it
            }
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("leftForwardPos", leftForwardPosition.get())
            it.addData("rightForwardPosition", rightForwardPosition.get())
            it.addData("leftBackPosition", leftBackPosition.get())
            it.addData("rightBackPosition", rightBackPosition.get())
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _rightForwardEncoder.direction = DcMotorSimple.Direction.REVERSE
            _rightBackEncoder.direction = DcMotorSimple.Direction.REVERSE
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _filterMutex.smartLock {
                _leftForwardFilter.start()
                _rightForwardFilter.start()
                _leftBackFilter.start()
                _rightBackFilter.start()
            }

            _deltaTime.reset()

            if(HotRun.LAZY_INSTANCE.currentRunMode.get() == HotRun.RunMode.AUTO){
                _leftForwardEncoder.resetEncoder()
                _rightForwardEncoder.resetEncoder()
                _leftBackEncoder.resetEncoder()
                _rightBackEncoder.resetEncoder()
            }
        }
    }

    override fun dispose() {

    }
}