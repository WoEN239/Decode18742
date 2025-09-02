package org.woen.threading.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.IHardwareDevice
import org.woen.utils.servoAngle.ServoAngle
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

class ThreadedServo(val name: String,
                    var Vmax: Double = ThreadedConfigs.DEFAULT_SERVO_V_MAX.get(),
                    var a: Double = ThreadedConfigs.DEFAULT_SERVO_A.get(),
                    val maxAngle: Double = ThreadedConfigs.DEFAULT_SERVO_ANGLE.get(),
                    val angleOffset: Double = ThreadedConfigs.DEFAULT_SERVO_OFFSET.get(),
                    val startAngle: Double = 0.0): IHardwareDevice {
    private lateinit var _device: ServoAngle

    var currentAngle = startAngle
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

    private val _calcMutex = Mutex()

    var targetAngle = startAngle
        set(value) {
            if(value < 0)
                return

            if (abs(value - field) < angleOffset) {
                return
            }

            runBlocking {
                _calcMutex.withLock {
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
            }

            field = value
        }

    override fun update() {
        runBlocking {
            _calcMutex.withLock {
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
                    }

                    return@runBlocking
                }

                if (_servoTime.seconds() <= t5) {
                    currentAngle = if (_servoTime.seconds() <= t4)
                        y0 + sign * (a * _servoTime.seconds().pow(2) / 2)
                    else
                        y0 + sign * (a * t4.pow(2) / 2 + sqrt(yAbs / a) * a *
                                (_servoTime.seconds() - t4) - a *
                                (_servoTime.seconds() - t4).pow(2) / 2)
                }
            }
        }
    }

    val atTargetAngle: Boolean
        get() =
            runBlocking {
                _calcMutex.withLock {
                    return@withLock _servoTime.seconds() > t5 || (t3 > t2 &&
                            _servoTime.seconds() > t2 + t3)
                }
            }

    override fun init(hardwareMap: HardwareMap) {
        _device = ServoAngle(hardwareMap.get(name) as Servo, maxAngle)

        _device.angle = startAngle
    }

    override fun dispose() {

    }
}