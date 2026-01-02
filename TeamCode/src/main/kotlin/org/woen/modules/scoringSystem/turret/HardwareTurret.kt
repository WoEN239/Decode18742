package org.woen.modules.scoringSystem.turret

import androidx.core.math.MathUtils.clamp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.motor.EncoderOnly
import org.woen.utils.regulator.Regulator
import kotlin.math.PI
import kotlin.math.abs

class HardwareTurret :
    IHardwareDevice {
    private lateinit var _motor: DcMotorEx
    private lateinit var _angleSevo: Servo
    private lateinit var _rotateServo: Servo

    private lateinit var _rotateEncoder: EncoderOnly

    var anglePosition: Double
        get() = (_anglePosition - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) /
                (Configs.TURRET.MAX_TURRET_ANGLE_SERVO - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) *
                (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) + Configs.TURRET.MIN_TURRET_ANGLE
        set(value) {
            _anglePosition = (clamp(
                value, Configs.TURRET.MIN_TURRET_ANGLE, Configs.TURRET.MAX_TURRET_ANGLE
            )
                    - Configs.TURRET.MIN_TURRET_ANGLE) /
                    (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) *
                    (Configs.TURRET.MAX_TURRET_ANGLE_SERVO - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) +
                    Configs.TURRET.MIN_TURRET_ANGLE_SERVO
        }

    private var _anglePosition = 0.0

    private var _oldMotorPosition = 0.0
    private var _motorVelocity = 0.0
    var targetVelocity: Double
        get() = (_targetTicksVelocity * 2.0 * PI * Configs.TURRET.PULLEY_RADIUS) / Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION
        set(value) {
            _targetTicksVelocity =
                (value * Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION) / (2.0 * PI * Configs.TURRET.PULLEY_RADIUS)
        }

    private var _targetTicksVelocity = 0.0

    val currentVelocity: Double
        get() = (_motorVelocity * 2.0 * PI * Configs.TURRET.PULLEY_RADIUS) / Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION

    var shotWasFired = false
        private set
    private var _motorAmps = 0.0

    private val _regulator = Regulator(Configs.TURRET.PULLEY_REGULATOR)
    private val _velocityFilter =
        ExponentialFilter(Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.get())

    var velocityAtTarget = false
        private set

    private val _targetTimer = ElapsedTime()
    private val _deltaTime = ElapsedTime()
    private val _shootTriggerTimer = ElapsedTime()
    private var _isServoZeroed = false
    private var _currentRotate = 0.0
    private var _targetRotationPosition = 0.0
    private var _pulleyU = 0.0

    val currentRotatePosition
        get() = _currentRotate / Configs.TURRET.ENCODER_TICKS_IN_REVOLUTION * Configs.TURRET.ROTATE_ENCODER_RATIO * 2.0 * PI

    var targetRotatePosition: Double
        get() =
            (_targetRotationPosition - Configs.TURRET.ZERO_ROTATE_POS) * Configs.TURRET.ROTATE_SERVO_TURNS * Configs.TURRET.ROTATE_SERVO_RATIO
        set(value) {
            _targetRotationPosition = clamp(
                value, Configs.TURRET.MIN_ROTATE,
                Configs.TURRET.MAX_ROTATE
            ) / Configs.TURRET.ROTATE_SERVO_RATIO / Configs.TURRET.ROTATE_SERVO_TURNS +
                    Configs.TURRET.ZERO_ROTATE_POS
        }

    override fun update() {
        _motorAmps = _motor.getCurrent(CurrentUnit.AMPS)

        if (_motorAmps > Configs.TURRET.TURRET_SHOOT_DETECT_CURRENT)
            shotWasFired = _shootTriggerTimer.seconds() > Configs.TURRET.SHOOT_TRIGGER_DELAY
        else {
            _shootTriggerTimer.reset()
            shotWasFired = false
        }

//        _angleSevo.position = _anglePosition

        if (_isServoZeroed) {
//            targetRotatePosition = 0.0
            _currentRotate = _rotateEncoder.currentPosition.toDouble()
            _rotateServo.position = _targetRotationPosition
        }

        val currentMotorPosition = _motor.currentPosition.toDouble()

        val rawVelocity = (currentMotorPosition - _oldMotorPosition) / _deltaTime.seconds()

        _motorVelocity =
            _velocityFilter.updateRaw(_motorVelocity, rawVelocity - _motorVelocity)

        _oldMotorPosition = currentMotorPosition

        if (abs(currentVelocity - targetVelocity) < Configs.TURRET.PULLEY_TARGET_SENS) {
            velocityAtTarget = _targetTimer.seconds() > Configs.TURRET.PULLEY_TARGET_TIMER
        } else {
            _targetTimer.reset()

            velocityAtTarget = false
        }

        val err = _targetTicksVelocity - _motorVelocity

        _pulleyU = _regulator.update(
            err,
            _targetTicksVelocity
        )

        _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(
//            if(err < Configs.TURRET.ACCEL_THRESHOLD)
            _pulleyU
//            else{
//                _regulator.start()
//
//                Configs.TURRET.ACCEL_K
//            }
        )

        _deltaTime.reset()
    }

    override fun init(hardwareMap: HardwareMap) {
        _motor = hardwareMap.get("pulleyMotor") as DcMotorEx
//        _angleSevo = hardwareMap.get("turretAngleServo") as Servo

        _rotateEncoder = EncoderOnly(hardwareMap.get("brushMotor") as DcMotorEx)

        _rotateServo = hardwareMap.get("turretRotateServo") as Servo

        Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.onSet += {
            _velocityFilter.coef = it
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _motor.mode = DcMotor.RunMode.RESET_ENCODERS
            _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

            _rotateServo.direction = Servo.Direction.REVERSE

//            _angleSevo.direction = Servo.Direction.REVERSE
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("current pulley velocity", currentVelocity)
            it.addData("target pulley velocity", targetVelocity)
            it.addData("current ticks pulley velocity", _motorVelocity)
            it.addData("target ticks pulley velocity", _targetTicksVelocity)
            it.addData("pulley amps", _motorAmps)
            it.addData("angle position", anglePosition)
            it.addData("pulleyU", _pulleyU)
        }

        targetRotatePosition = 0.0
    }

    override fun opModeStart() {
        _deltaTime.reset()
        _shootTriggerTimer.reset()
        _targetTimer.reset()

        _velocityFilter.start()
        _regulator.start()

        if (!_isServoZeroed) {
            ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                _rotateServo.position = Configs.TURRET.ZERO_ROTATE_POS

                delay(100)

                _isServoZeroed = true
            }
        }
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}