package org.woen.threading.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.utils.servoAngle.ServoAngle
import org.woen.utils.smartMutex.SmartMutex
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

class ThreadedServo(
    private val _name: String,
    var Vmax: Double = Configs.SERVO_ANGLE.DEFAULT_SERVO_V_MAX,
    var a: Double = Configs.SERVO_ANGLE.DEFAULT_SERVO_A,
    val maxAngle: Double = Configs.SERVO_ANGLE.DEFAULT_SERVO_ANGLE,
    private val _angleOffset: Double = Configs.SERVO_ANGLE.DEFAULT_SERVO_OFFSET,
    private val _startAngle: Double = 0.0
) : IHardwareDevice {
    private lateinit var _device: ServoAngle

    var currentAngle = _startAngle
        private set

    private val _servoTime = ElapsedTime()

    private var t2 = 0.0
    private var t3 = 0.0
    private var t4 = 0.0
    private var t5 = 0.0
    private var yAbs = 0.0
    private var sign = 0.0
    private var t2Pow = 0.0
    private var y0 = 0.0

    private val _calcMutex = SmartMutex()

    var targetAngle = _startAngle
        set(value) {
            if (value < 0)
                return

            if (abs(value - field) < _angleOffset) {
                return
            }

            _calcMutex.smartLock {
                y0 = currentAngle

                _servoTime.reset()

                yAbs = abs(currentAngle - value)
                sign = sign(value - currentAngle)

                t2 = Vmax / a
                t3 = yAbs / Vmax - Vmax / a + t2

                if (t3 > t2)
                    t2Pow = a * t2.pow(2) / 2
                else {
                    t4 = sqrt(yAbs / a)

                    t5 = t4 * 2
                }
            }

            field = value
        }

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
            return

        _calcMutex.smartLock {
            if (t3 > t2) {
                if (_servoTime.seconds() <= t2 + t3) {
                    if (_servoTime.seconds() <= t2)
                        currentAngle = y0 + sign * (a * _servoTime.seconds().pow(2) / 2)
                    else if (_servoTime.seconds() <= t3)
                        currentAngle =
                            y0 + sign * (t2Pow + Vmax * (_servoTime.seconds() - t2))
                    else
                        currentAngle =
                            y0 + sign * (t2Pow + Vmax * (t3 - t2) + Vmax *
                                    (_servoTime.seconds() - t3) - a *
                                    (_servoTime.seconds() - t3).pow(2) / 2)

                    _device.angle = currentAngle
                }

                return@smartLock
            }

            if (_servoTime.seconds() <= t5) {
                currentAngle = if (_servoTime.seconds() <= t4)
                    y0 + sign * (a * _servoTime.seconds().pow(2) / 2)
                else
                    y0 + sign * (a * t4.pow(2) / 2 + sqrt(yAbs / a) * a *
                            (_servoTime.seconds() - t4) - a *
                            (_servoTime.seconds() - t4).pow(2) / 2)

                _device.angle = currentAngle
            }
        }
    }

    val atTargetAngle: Boolean
        get() =
            _calcMutex.smartLock {
                return@smartLock _servoTime.seconds() > t5 || (t3 > t2 &&
                        _servoTime.seconds() > t2 + t3)
            }

    override fun init(hardwareMap: HardwareMap) {
        _device = ServoAngle(hardwareMap.get(_name) as Servo, maxAngle)

        _device.angle = _startAngle
    }

    override fun dispose() {

    }
}