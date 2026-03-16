package org.woen.utils.drivers

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.clamp
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo

@Config
internal object LED_CONFIG {
    @JvmField
    var MAX_BORDER = 0.999999

    @JvmField
    var MIN_BORDER = 0.650051
}

class LEDLine(
    name: String,
    hardwareMap: HardwareMap,
    signalPin: SignalPin = SignalPin.MINUS,
    private val _maxBorder: Double = LED_CONFIG.MAX_BORDER,
    private val _minBorder: Double = LED_CONFIG.MIN_BORDER
) {
    enum class SignalPin {
        PLUS,
        MINUS
    }

    private val _port = hardwareMap.get(name) as Servo

    var power
        set(value) {
            _port.position = clamp(value, 0.0, 1.0) * (_maxBorder - _minBorder) + _minBorder
        }
        get() = (_port.position - _minBorder) / (_maxBorder - _minBorder)

    init {
        if (signalPin == SignalPin.MINUS)
            _port.direction = Servo.Direction.REVERSE

        (_port as PwmControl).pwmRange = PwmControl.PwmRange(0.0, 20000.0, 7000.0)

        power = 0.0
    }
}