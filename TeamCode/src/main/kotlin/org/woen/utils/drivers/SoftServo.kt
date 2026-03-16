package org.woen.utils.drivers

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

@Config
internal object ODOMETRY_CONFIG {
    @JvmField
    var SERVO_E = 15.0

    @JvmField
    var SERVO_W_MAX = 15.0
}

class SoftServo(
    val servo: Servo,
    private val _startPosition: Double = 0.0,
    var E: Double = ODOMETRY_CONFIG.SERVO_E,
    var WMax: Double = ODOMETRY_CONFIG.SERVO_W_MAX
) {
    private val _servoTime = ElapsedTime()

    private var t2 = 0.0
    private var t3 = 0.0
    private var t4 = 0.0
    private var t5 = 0.0
    private var yAbs = 0.0
    private var sign = 0.0
    private var t2Pow = 0.0
    private var y0 = 0.0

    var targetPosition: Double = -1.0
        set(value) {
            if (value < 0)
                return

            if (abs(value - field) < 0.002) {
                return
            }

            y0 = currentPosition

            _servoTime.reset()

            yAbs = abs(currentPosition - value)
            sign = sign(value - currentPosition)

            t2 = WMax / E
            t3 = yAbs / WMax - WMax / E + t2

            if (t3 > t2)
                t2Pow = E * t2.pow(2) / 2
            else {
                t4 = sqrt(yAbs / E)

                t5 = t4 * 2
            }

            field = value
        }

    var currentPosition
        get() = servo.position
        private set(value) {
            servo.position = value
        }

    fun start() {
        currentPosition = _startPosition
        targetPosition = _startPosition
    }

    fun update() {
        if (t3 > t2) {
            if (_servoTime.seconds() <= t2 + t3) {
                currentPosition = if (_servoTime.seconds() <= t2)
                    y0 + sign * (E * _servoTime.seconds().pow(2) / 2)
                else if (_servoTime.seconds() <= t3)
                    y0 + sign * (t2Pow + WMax * (_servoTime.seconds() - t2))
                else
                    y0 + sign * (t2Pow + WMax * (t3 - t2) + WMax * (_servoTime.seconds() - t3) - E * (_servoTime.seconds() - t3).pow(
                        2
                    ) / 2)
            }

            return
        }

        if (_servoTime.seconds() <= t5) {
            currentPosition = if (_servoTime.seconds() <= t4)
                y0 + sign * (E * _servoTime.seconds().pow(2) / 2)
            else
                y0 + sign * (E * t4.pow(2) / 2 + sqrt(yAbs / E) * E * (_servoTime.seconds() - t4) - E * (_servoTime.seconds() - t4).pow(
                    2
                ) / 2)
        }
    }

    val atTarget
        get() = _servoTime.seconds() > t5 || (t3 > t2 && _servoTime.seconds() > t2 + t3)
}