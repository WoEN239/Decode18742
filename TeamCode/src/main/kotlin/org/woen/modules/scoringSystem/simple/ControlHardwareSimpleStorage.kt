package org.woen.modules.scoringSystem.simple

import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.Hardware
import org.woen.telemetry.ThreadedTelemetry



class ControlHardwareSimpleStorage : IHardwareDevice {
    private lateinit var _leftColorSensor: RevColorSensorV3
    private lateinit var _rightColorSensor: RevColorSensorV3

    private lateinit var _gateServo: Servo
    private lateinit var _pushServo: Servo

    var isBall = false
        private set

    override fun update() {
//        val rightColorRaw = _rightColorSensor.normalizedColors
//        val leftColorRaw = _leftColorSensor.normalizedColors
//
//        val rightRed = rightColorRaw.red * Configs.STORAGE_SENSORS.VAR_MAXIMUM_READING
//        val rightGreen = rightColorRaw.green * Configs.STORAGE_SENSORS.VAR_MAXIMUM_READING
//        val rightBlue = rightColorRaw.blue * Configs.STORAGE_SENSORS.VAR_MAXIMUM_READING
//
//        val leftRed = leftColorRaw.red * Configs.STORAGE_SENSORS.VAR_MAXIMUM_READING
//        val leftGreen = leftColorRaw.green * Configs.STORAGE_SENSORS.VAR_MAXIMUM_READING
//        val leftBlue = leftColorRaw.blue * Configs.STORAGE_SENSORS.VAR_MAXIMUM_READING
//
//        isBall = (rightRed > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD ||
//                    rightGreen > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD ||
//                    rightBlue > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD ||
//                    leftRed > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD ||
//                    leftGreen > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD ||
//                    leftBlue > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD)
    }

    override fun init(hardwareMap: HardwareMap) {
//        _leftColorSensor = hardwareMap.get("leftColorSensor") as RevColorSensorV3
//        _rightColorSensor = hardwareMap.get("rightColorSensor") as RevColorSensorV3

        _gateServo = hardwareMap.get(Hardware.DEVICE_NAMES.GATE_SERVO) as Servo
        _pushServo = hardwareMap.get(Hardware.DEVICE_NAMES.PUSH_SERVO) as Servo

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("isBall", isBall)
        }
    }

    override fun opModeStart() {
        isBall = false

        _gateServo.position = Hardware.VALUES.SERVO.GATE_CLOSE
        _pushServo.position = Hardware.VALUES.SERVO.PUSH_CLOSE
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}