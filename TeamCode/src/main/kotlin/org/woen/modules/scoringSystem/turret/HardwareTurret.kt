package org.woen.modules.scoringSystem.turret

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.regulator.Regulator
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.abs

class HardwareTurret(
    private val _motorName: String,
    private var _servoName: String
) :
    IHardwareDevice {
    private lateinit var _motor: DcMotorEx
    private lateinit var _angleSevo: Servo

    var targetVelocity: Double
        get() = _realTargetVelocity.get() /
                Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION * (2.0 * PI * Configs.TURRET.PULLEY_RADIUS)
        set(value) {
            _realTargetVelocity.set(
                (value * Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION) / (2.0 * PI * Configs.TURRET.PULLEY_RADIUS)
            )
        }

    var anglePosition = AtomicReference(0.0)

    private var _realTargetVelocity = AtomicReference(0.0)

    private val _pulleyRegulator = Regulator(Configs.TURRET.PULLEY_REGULATOR)

    private val _pulleyRegulatorMutex = SmartMutex()

    private val _velocityFilterMutex = SmartMutex()

    private val _velocityFilter =
        ExponentialFilter(Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.get())

    private var _oldMotorPosition = 0.0

    private var _motorVelocity = 0.0

    var velocityAtTarget = AtomicBoolean(false)

    private val _deltaTime = ElapsedTime()

    private var _rotationRegulator = Regulator(Configs.TURRET.ROTATION_PID_CONFIG)

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
            return

//        _angleSevo.position = anglePosition.get()

        val currentMotorPosition = _motor.currentPosition.toDouble()

        val rawVelocity = (currentMotorPosition - _oldMotorPosition) / _deltaTime.seconds()

        _velocityFilterMutex.smartLock {
            _motorVelocity =
                _velocityFilter.updateRaw(_motorVelocity, rawVelocity - _motorVelocity)
        }

        _oldMotorPosition = currentMotorPosition

        val target = _realTargetVelocity.get()
        val velErr = target - _motorVelocity

        if (abs(velErr) > Configs.TURRET.PULLEY_TARGET_SENS)
            velocityAtTarget.set(false)
        else
            velocityAtTarget.set(true)

        _pulleyRegulatorMutex.smartLock {
            _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(
                _pulleyRegulator.update(
                    velErr,
                    target
                )
            )
        }

        _deltaTime.reset()
    }

    override fun init(hardwareMap: HardwareMap) {
        _motor = hardwareMap.get(_motorName) as DcMotorEx
        _angleSevo = hardwareMap.get(_servoName) as Servo

        Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.onSet += {
            _velocityFilterMutex.smartLock {
                _velocityFilter.coef = it
            }
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _velocityFilterMutex.smartLock {
                _velocityFilter.start()
            }

            _pulleyRegulatorMutex.smartLock {
                _pulleyRegulator.start()
            }

            _rotationRegulator.start()

            _deltaTime.reset()
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _motor.mode = DcMotor.RunMode.RESET_ENCODERS
            _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

            _motor.direction = DcMotorSimple.Direction.REVERSE
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("currentTurretVelocity", _motorVelocity)
            it.addData("targetTurretVelocity", _realTargetVelocity.get())
        }
    }

    override fun dispose() {

    }
}