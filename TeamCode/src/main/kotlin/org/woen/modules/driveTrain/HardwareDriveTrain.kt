package org.woen.modules.driveTrain

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.hotRun.HotRun
import org.woen.telemetry.configs.Configs
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

class HardwareDriveTrain : IHardwareDevice {
    private lateinit var _leftForwardMotor: MotorOnly
    private lateinit var _leftBackMotor: MotorOnly
    private lateinit var _rightBackMotor: MotorOnly
    private lateinit var _rightForwardMotor: MotorOnly

    private val _forwardRegulator = Regulator(Configs.DRIVE_TRAIN.DRIVE_FORWARD_REGULATOR_PARAMS)
    private val _sideRegulator = Regulator(Configs.DRIVE_TRAIN.DRIVE_SIDE_REGULATOR_PARAMS)
    private val _rotateRegulator = Regulator(Configs.DRIVE_TRAIN.DRIVE_ROTATE_REGULATOR_PARAMS)

    private val _regulatorMutex = SmartMutex()

    private var _targetTranslateVelocity = Vec2.ZERO
    private var _targetRotateVelocity = 0.0

    private val _driveTrainMutex = SmartMutex()

    override fun update() {
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
        _leftForwardMotor = MotorOnly(hardwareMap.get("leftForwardDrive") as DcMotorEx)
        _leftBackMotor = MotorOnly(hardwareMap.get("leftBackDrive") as DcMotorEx)
        _rightBackMotor = MotorOnly(hardwareMap.get("rightBackDrive") as DcMotorEx)
        _rightForwardMotor = MotorOnly(hardwareMap.get("rightForwardDrive") as DcMotorEx)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _leftBackMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            _leftForwardMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            _rightBackMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            _rightForwardMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

            _leftForwardMotor.direction = DcMotorSimple.Direction.REVERSE
            _leftBackMotor.direction = DcMotorSimple.Direction.REVERSE
            _rightBackMotor.direction = DcMotorSimple.Direction.REVERSE
            _rightForwardMotor.direction = DcMotorSimple.Direction.REVERSE
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            _driveTrainMutex.smartLock {
                it.addData("targetXVel", _targetTranslateVelocity.x)
                it.addData("targetYVel", _targetTranslateVelocity.y)
                it.addData("targetRotationVel", _targetRotateVelocity)
            }

            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            it.addData("currentXVel", odometry.odometryVelocity.x)
            it.addData("currentYVel", odometry.odometryVelocity.y)
            it.addData("currentRotationVelocity", odometry.odometryRotateVelocity)
        }
    }

    override fun opModeStart() {
        _regulatorMutex.smartLock {
            _forwardRegulator.start()
            _sideRegulator.start()
            _rotateRegulator.start()
            _forwardRegulator.resetIntegral()
            _sideRegulator.resetIntegral()
            _rotateRegulator.resetIntegral()
        }
    }

    override fun opModeStop() {

    }

    fun drive(targetTranslateVelocity: Vec2, targetRotationVelocity: Double) {
        _driveTrainMutex.smartLock {
            _targetTranslateVelocity = targetTranslateVelocity
            _targetRotateVelocity = targetRotationVelocity
        }
    }

    private fun setPowers(
        leftFrontPower: Double,
        rightBackPower: Double,
        leftBackPower: Double,
        rightForwardPower: Double
    ) {
        var lfPower = leftFrontPower
        var rbPower = rightBackPower
        var lbPower = leftBackPower
        var rfPower = rightForwardPower

        val absMax = max(
            abs(lfPower),
            max(
                abs(rbPower),
                max(
                    abs(lbPower),
                    abs(rfPower)
                )
            )
        )

        if (absMax > 1.0) {
            lfPower /= absMax
            rbPower /= absMax
            lbPower /= absMax
            rfPower /= absMax
        }

        _leftForwardMotor.power = lfPower
        _rightBackMotor.power = rbPower
        _leftBackMotor.power = lbPower
        _rightForwardMotor.power = rfPower
    }

    private fun setVoltage(direction: Vec2, rotate: Double) {
        setPowers(
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x - direction.y - rotate),
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x - direction.y + rotate),
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x + direction.y - rotate),
            ThreadedBattery.LAZY_INSTANCE.voltageToPower(direction.x + direction.y + rotate)
        )
    }

    override fun dispose() {

    }
}