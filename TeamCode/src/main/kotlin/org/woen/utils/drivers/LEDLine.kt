package org.woen.utils.drivers

import com.acmerobotics.roadrunner.clamp
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo


class LEDLine(
    hardwareMap: HardwareMap, name: String, signalPin: SignalPin = SignalPin.MINUS,
    private val _maxBorder: Double = 0.999999, private val _minBorder: Double = 0.650051
) {
    enum class SignalPin {
        PLUS,
        MINUS
    }

    private val _port: Servo

    var power
        set(value) {
            _port.position = clamp(value, 0.0, 1.0) * (_maxBorder - _minBorder) + _minBorder
        }
        get() = (_port.position - _minBorder) / (_maxBorder - _minBorder)

    init {
        _port = hardwareMap.get(name) as Servo

        init()

        power = 0.0
    }

    fun init(){
        _port.direction = Servo.Direction.REVERSE

        (_port as PwmControl).pwmRange = PwmControl.PwmRange(0.0, 20000.0, 7000.0)
    }
}