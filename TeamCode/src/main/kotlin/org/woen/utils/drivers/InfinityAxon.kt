package org.woen.utils.drivers

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import org.woen.utils.regulator.Regulator
import org.woen.utils.regulator.RegulatorParameters
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.abs

@Config
internal object INFINITY_AXON_CONFIG {
    @JvmField
    var REGULATOR = RegulatorParameters(kP = 0.2, kD = 0.00005, kPow = 0.0001, limitU = 1.0)

    @JvmField
    var SENSITIVE = toRadians(10.0)

    @JvmField
    var DELTA_BORDER = 0.56

    @JvmField
    var DELTAS_COUNT = 5
}

class InfinityAxon(
    servoName: String,
    encoderName: String,
    hardwareMap: HardwareMap,
    startPosition: Double = 0.0,
    var direction: Servo.Direction = Servo.Direction.FORWARD,
    regulator: RegulatorParameters = INFINITY_AXON_CONFIG.REGULATOR,
    private var _sens: Double = INFINITY_AXON_CONFIG.SENSITIVE,
    private var _deltaBorder: Double = INFINITY_AXON_CONFIG.DELTA_BORDER,
    private var _deltasCount: Int = INFINITY_AXON_CONFIG.DELTAS_COUNT
) {
    private val _servo = hardwareMap.get(servoName) as Servo
    private val _encoder = hardwareMap.get(encoderName) as AnalogInput

    private val _regulator = Regulator(regulator)

    init {
        _servo.direction = Servo.Direction.REVERSE
        (_servo as PwmControl).pwmRange = PwmControl.PwmRange(500.0, 2500.0)

        _servo.position = 0.5
    }

    var targetPosition
        set(value) {
            _targetPosition = if (direction == Servo.Direction.FORWARD) value else -value
        }
        get() = if (direction == Servo.Direction.FORWARD) _targetPosition else -_targetPosition

    private var _position = 0.0

    val position
        get() = if (direction == Servo.Direction.FORWARD) _position else -_position

    val atTarget
        get() = abs(_position - _targetPosition) < _sens

    private var _targetPosition =
        if (direction == Servo.Direction.FORWARD) startPosition else -startPosition
    private var _turns = 0
    private var _oldPosition = 0.0
    private val _deltas = arrayListOf<Double>()

    fun start() {
        _regulator.start()
        _regulator.resetIntegral()

        _oldPosition = _encoder.voltage / 3.3
        _deltas.clear()
    }

    fun update() {
        val rawPos = _encoder.voltage / 3.3

        val delta = _oldPosition - rawPos

        _deltas.add(delta)

        if (_deltas.count() > _deltasCount)
            _deltas.removeAt(0)

        val deltasSum = _deltas.sum()

        if (deltasSum > _deltaBorder) {
            _turns++

            _deltas.clear()
        }

        if (deltasSum < -_deltaBorder) {
            _turns--
            _deltas.clear()
        }

        _oldPosition = rawPos

        _position = (_turns.toDouble() + rawPos) * PI * 2.0

        _servo.position = (_regulator.update(_targetPosition - _position) + 1.0) / 2.0
    }
}