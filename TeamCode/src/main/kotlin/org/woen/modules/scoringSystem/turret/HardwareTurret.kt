package org.woen.modules.scoringSystem.turret

import androidx.core.math.MathUtils.clamp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoImpl
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
    private lateinit var _rotateEncoder: EncoderOnly

    private var _oldMotorPosition = 0.0
    private var _motorVelocity = 0.0
    var targetVelocity: Double
        get() = (_targetTicksVelocity * 2.0 * PI * Configs.TURRET.PULLEY_RADIUS) / Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION
        set(value) {
            _targetTicksVelocity =
                (value * Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION) / (2.0 * PI * Configs.TURRET.PULLEY_RADIUS) * Configs.TURRET.KOST_K
        }
    private var _targetTicksVelocity = 0.0
    val currentVelocity: Double
        get() = (_motorVelocity * 2.0 * PI * Configs.TURRET.PULLEY_RADIUS) / Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION

    private var _pulleyMotorAmps = 0.0

    private val _regulator = Regulator(Configs.TURRET.PULLEY_REGULATOR)
    private val _velocityFilter =
        ExponentialFilter(Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.get())
    var velocityAtTarget = false
        private set

    private val _targetTimer = ElapsedTime()
    private val _deltaTime = ElapsedTime()
    private val _shootTriggerTimer = ElapsedTime()

    private var _currentRotate = 0.0
    private var _pulleyU = 0.0
    val currentRotatePosition
        get() = _currentRotate / Configs.TURRET.ENCODER_TICKS_IN_REVOLUTION *
                Configs.TURRET.ROTATE_ENCODER_RATIO * 2.0 * PI



    override fun update() {

        _pulleyMotorAmps = _motor.getCurrent(CurrentUnit.AMPS)

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

        _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_pulleyU)

        _deltaTime.reset()
    }

    fun resetRotateEncoder(){
        _rotateEncoder.resetEncoder()
    }

    override fun init(hardwareMap: HardwareMap) {
        _motor = hardwareMap.get("pulleyMotor") as DcMotorEx

        _rotateEncoder = EncoderOnly(hardwareMap.get("brushMotor") as DcMotorEx)

        Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.onSet += {
            _velocityFilter.coef = it
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _motor.mode = DcMotor.RunMode.RESET_ENCODERS
            _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("current pulley velocity", currentVelocity)
            it.addData("target pulley velocity", targetVelocity)
            it.addData("current ticks pulley velocity", _motorVelocity)
            it.addData("target ticks pulley velocity", _targetTicksVelocity)
            it.addData("pulley motor amps", _pulleyMotorAmps)
            it.addData("pulleyU", _pulleyU)
            it.addData("current turret rotation",  Math.toDegrees(currentRotatePosition))
        }
    }

    override fun opModeStart() {
        _deltaTime.reset()
        _shootTriggerTimer.reset()
        _targetTimer.reset()

        _velocityFilter.start()
        _regulator.start()
        _regulator.resetIntegral()

        _oldMotorPosition = 0.0
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}