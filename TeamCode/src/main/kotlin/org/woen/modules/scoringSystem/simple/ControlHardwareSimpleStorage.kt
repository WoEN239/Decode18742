package org.woen.modules.scoringSystem.simple

import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice

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

        _gateServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.GATE_SERVO) as Servo
        _pushServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.PUSH_SERVO) as Servo

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("isBall", isBall)
        }
    }

    override fun opModeStart() {
        isBall = false

        _gateServo.position = Configs.STORAGE.GATE_SERVO_CLOSE_VALUE
        _pushServo.position = Configs.STORAGE.PUSH_SERVO_CLOSE_VALUE
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}