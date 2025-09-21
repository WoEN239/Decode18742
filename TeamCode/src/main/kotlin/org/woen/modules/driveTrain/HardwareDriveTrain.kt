package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.hotRun.HotRun
import org.woen.modules.driveTrain.odometry.RequireOdometryEvent
import org.woen.telemetry.ThreadedConfigs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.motor.MotorOnly
import org.woen.utils.regulator.Regulator
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Vec2
import kotlin.math.abs
import kotlin.math.max

class HardwareDriveTrain(
    private val _leftForwardMotorName: String,
    private val _leftBackMotorName: String,
    private val _rightForwardMotorName: String,
    private val _rightBackMotorName: String
) : IHardwareDevice {
    private lateinit var _leftForwardMotor: DcMotorEx
    private lateinit var _leftBackMotor: DcMotorEx
    private lateinit var _rightBackMotor: DcMotorEx
    private lateinit var _rightForwardMotor: DcMotorEx

    private val _forwardRegulator = Regulator(ThreadedConfigs.DRIVE_FORWARD_REGULATOR_PARAMS)
    private val _sideRegulator = Regulator(ThreadedConfigs.DRIVE_SIDE_REGULATOR_PARAMS)
    private val _rotateRegulator = Regulator(ThreadedConfigs.DRIVE_ROTATE_REGULATOR_PARAMS)

    private val _regulatorMutex = SmartMutex()

    private var _targetTranslateVelocity = Vec2.ZERO
    private var _targetRotateVelocity = 0.0

    private val _driveTrainMutex = SmartMutex()

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
            return

        val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())
        val currentVelocity = odometry.odometryVelocity
        val currentRotationVelocity = odometry.odometryRotateVelocity

        var targetTranslateVelocity = Vec2.ZERO
        var targetRotateVelocity = 0.0

        _driveTrainMutex.smartLock {
            targetTranslateVelocity = _targetTranslateVelocity
            targetRotateVelocity = _targetRotateVelocity
        }

        val velocityErr = targetTranslateVelocity - currentVelocity

        ThreadedTelemetry.LAZY_INSTANCE.log(velocityErr.toString())

        val velocityRotateErr = targetRotateVelocity - currentRotationVelocity

        _regulatorMutex.smartLock {
            setVoltage(
                Vec2(
                    _forwardRegulator.update(velocityErr.x, targetTranslateVelocity.x),
                    _sideRegulator.update(velocityErr.y, targetTranslateVelocity.y)
                ),
                _rotateRegulator.update(velocityRotateErr, targetRotateVelocity)
            )
        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _leftForwardMotor = MotorOnly(hardwareMap.get(_leftForwardMotorName) as DcMotorEx)
        _leftBackMotor = MotorOnly(hardwareMap.get(_leftBackMotorName) as DcMotorEx)
        _rightBackMotor = MotorOnly(hardwareMap.get(_rightBackMotorName) as DcMotorEx)
        _rightForwardMotor = MotorOnly(hardwareMap.get(_rightForwardMotorName) as DcMotorEx)

        _leftBackMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        _leftForwardMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        _rightForwardMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        _rightBackMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _rightBackMotor.direction = DcMotorSimple.Direction.REVERSE
        _rightForwardMotor.direction = DcMotorSimple.Direction.REVERSE

        _leftBackMotor.direction = DcMotorSimple.Direction.REVERSE
        _leftBackMotor.direction = DcMotorSimple.Direction.REVERSE

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _regulatorMutex.smartLock {
                _forwardRegulator.start()
                _sideRegulator.start()
                _rotateRegulator.start()
            }
        }
    }

    fun drive(targetTranslateVelocity: Vec2, targetRotationVelocity: Double) {
        _driveTrainMutex.smartLock {
            _targetTranslateVelocity = targetTranslateVelocity
            _targetRotateVelocity = targetRotationVelocity
        }
    }

    private fun setVoltage(direction: Vec2, rotate: Double) {
        var leftFrontPower =
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x - direction.y - rotate)
        var rightBackPower =
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x - direction.y + rotate)
        var leftBackPower =
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x + direction.y - rotate)
        var rightForwardPower =
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x + direction.y + rotate)

        val absMax = max(
            abs(leftFrontPower),
            max(
                abs(rightBackPower),
                max(
                    abs(leftBackPower),
                    abs(rightForwardPower)
                )
            )
        )

        if (absMax > 1.0) {
            leftFrontPower /= absMax
            rightBackPower /= absMax
            leftBackPower /= absMax
            rightForwardPower /= absMax
        }

        _leftForwardMotor.power = leftFrontPower
        _rightBackMotor.power = rightBackPower
        _leftBackMotor.power = leftBackPower
        _rightForwardMotor.power = rightForwardPower
    }

    override fun dispose() {

    }
}