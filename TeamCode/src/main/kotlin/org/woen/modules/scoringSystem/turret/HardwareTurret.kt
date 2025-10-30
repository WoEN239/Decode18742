package org.woen.modules.scoringSystem.turret

import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.motor.EncoderOnly
import org.woen.utils.regulator.Regulator
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.abs

class HardwareTurret(
    private val _motorName: String,
    private var _servoName: String,
    private var _rotationServoName: String,
    private var _rotationEncoderName: String
) :
    IHardwareDevice {
    private lateinit var _motor: DcMotorEx
    private lateinit var _angleSevo: Servo
    private lateinit var _rotationServo: CRServo
    private lateinit var _rotationEncoder: EncoderOnly

    var targetVelocity: Double
        get() = _realTargetVelocity.get() /
                Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION * (2.0 * PI * Configs.TURRET.PULLEY_RADIUS)
        set(value) {
            _realTargetVelocity.set(
                value /
                        (2.0 * PI * Configs.TURRET.PULLEY_RADIUS) * Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION
            )
        }

    var anglePosition = AtomicReference(0.0)
    var rotationPosition = AtomicReference(0.0)

    private var _realTargetVelocity = AtomicReference(0.0)

    private val _pulleyRegulator = Regulator(Configs.TURRET.PULLEY_REGULATOR)

    private val _pulleyRegulatorMutex = SmartMutex()

    private val _velocityFilterMutex = SmartMutex()

    private val _velocityFilter =
        ExponentialFilter(Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.get())

    private var _oldMotorPosition = 0.0

    private var _motorVelocity = 0.0

    var velocityAtTarget = AtomicBoolean(false)

    private var _rotationRegulator = Regulator(Configs.TURRET.ROTATION_PID_CONFIG)

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
            return

        _angleSevo.position = anglePosition.get()

        val currentMotorPosition = _motor.currentPosition.toDouble()

        val rawVelocity = currentMotorPosition - _oldMotorPosition

        _velocityFilterMutex.smartLock {
            _motorVelocity =
                _velocityFilter.updateRaw(_motorVelocity, rawVelocity - _motorVelocity)
        }

        _oldMotorPosition = currentMotorPosition

        if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
            return

        val target = _realTargetVelocity.get()
        val velErr = target - _motorVelocity

        if (abs(velErr) > Configs.TURRET.PULLEY_TARGET_SENS)
            velocityAtTarget.set(false)
        else
            velocityAtTarget.set(true)

        _rotationServo.power =
            _rotationRegulator.update(rotationPosition.get() / Configs.TURRET.ROTATION_TICKS_IN_REVOLUTION - _rotationEncoder.currentPosition)

        _pulleyRegulatorMutex.smartLock {
            _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(
                _pulleyRegulator.update(
                    velErr,
                    target
                )
            )
        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _motor = hardwareMap.get(_motorName) as DcMotorEx
        _angleSevo = hardwareMap.get(_servoName) as Servo

        _rotationServo = hardwareMap.get(_rotationServoName) as CRServo

        _rotationEncoder = EncoderOnly(hardwareMap.get(_rotationEncoderName) as DcMotorEx)

        _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _motor.mode = DcMotor.RunMode.RESET_ENCODERS
        _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.onSet += {
            _velocityFilterMutex.smartLock {
                _velocityFilter.coef = it
            }
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _pulleyRegulatorMutex.smartLock {
                _pulleyRegulator.start()
            }

            _rotationRegulator.start()

            if (HotRun.LAZY_INSTANCE.currentRunMode.get() == HotRun.RunMode.AUTO)
                _rotationEncoder.resetEncoder()
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _velocityFilterMutex.smartLock {
                _velocityFilter.start()
            }
        }
    }

    override fun dispose() {

    }
}