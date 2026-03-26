package org.woen.utils.drivers

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.clamp
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo

@Config
internal object LED_CONFIG {
    @JvmField
    var MAX_BORDER = 1.0

    @JvmField
    var MIN_BORDER = 0.0
}

class LEDLine(
    name: String,
    hardwareMap: HardwareMap,
    signalPin: SignalPin = SignalPin.PLUS,
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
            _port.position = value
        }
        get() = 0.0

    init {
        if (signalPin == SignalPin.PLUS)
            _port.direction = Servo.Direction.REVERSE

        (_port as PwmControl).pwmRange = PwmControl.PwmRange(0.0,100.0,100.0)

        power = 0.0
    }
}