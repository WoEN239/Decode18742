package org.woen.utils.drivers

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo

class LEDLine(
    name: String,
    hardwareMap: HardwareMap,
    signalPin: SignalPin = SignalPin.PLUS
) {
    enum class SignalPin {
        PLUS,
        MINUS
    }

    private val _port = hardwareMap.get(name) as Servo

    var power
        set(value) {
            _port.position = value * (0.99 - 0.001) + 0.001
        }
        get() = (_port.position - 0.001) / (0.99 - 0.001)

    init {
        if (signalPin == SignalPin.PLUS)
            _port.direction = Servo.Direction.REVERSE

        (_port as PwmControl).pwmRange = PwmControl.PwmRange(0.0, 1000.0, 1000.0)

        power = 0.0
    }
}