package org.woen.modules.scoringSystem.turret

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.regulator.Regulator
import kotlin.math.PI
import kotlin.math.abs


class HardwareTurret :
    IHardwareDevice {
    private lateinit var _motor: DcMotorEx
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


    private val _regulator = Regulator(Configs.TURRET.PULLEY_REGULATOR)
    private val _velocityFilter =
        ExponentialFilter(Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.get())

    private val _deltaTime = ElapsedTime()

    private var _pulleyU = 0.0

    private val _targetTimer = ElapsedTime()

    var pulleyAtTarget = false
        private set

    override fun update() {
        val currentMotorPosition = _motor.currentPosition.toDouble()

        val rawVelocity = (currentMotorPosition - _oldMotorPosition) / _deltaTime.seconds()

        _motorVelocity =
            _velocityFilter.updateRaw(_motorVelocity, rawVelocity - _motorVelocity)

        _oldMotorPosition = currentMotorPosition

        val err = _targetTicksVelocity - _motorVelocity

        if(abs(err) > Configs.TURRET.REGULATOR_SENS)
            pulleyAtTarget = _targetTimer.seconds() > Configs.TURRET.TARGET_TIMER
        else {
            _targetTimer.reset()
            pulleyAtTarget = false
        }

        _pulleyU = _regulator.update(
            err,
            _targetTicksVelocity
        )

        _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_pulleyU)

        _deltaTime.reset()
    }

    override fun init(hardwareMap: HardwareMap) {
        _motor = hardwareMap.get("pulleyMotor") as DcMotorEx
        _motor.direction = DcMotorSimple.Direction.REVERSE

        Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.onSet += {
            _velocityFilter.coef = it
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("current pulley velocity", currentVelocity)
            it.addData("target pulley velocity", targetVelocity)
            it.addData("current ticks pulley velocity", _motorVelocity)
            it.addData("target ticks pulley velocity", _targetTicksVelocity)
            it.addData("pulleyU", _pulleyU)
        }
    }

    override fun opModeStart() {
        _deltaTime.reset()

        _velocityFilter.start()
        _regulator.start()
        _regulator.resetIntegral()
        _targetTimer.reset()

        _oldMotorPosition = 0.0

        _motor.mode = DcMotor.RunMode.RESET_ENCODERS
        _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}