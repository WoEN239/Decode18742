package org.woen.modules.scoringSystem.simple

import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.Hardware
import org.woen.telemetry.ThreadedTelemetry
import org.woen.utils.events.SimpleEmptyEvent


class ControlHardwareSimpleStorage : IHardwareDevice {
    private lateinit var _leftColor: RevColorSensorV3
    private lateinit var _rightColor: RevColorSensorV3
    
    val detectEvent = SimpleEmptyEvent()

    override fun update() {
        val leftColor = _leftColor.normalizedColors
        val rightColor = _rightColor.normalizedColors

        val leftR = leftColor.red * Hardware.SENSORS.MAXIMUM_READING
        val leftG = leftColor.green * Hardware.SENSORS.MAXIMUM_READING
        val leftB = leftColor.blue * Hardware.SENSORS.MAXIMUM_READING

        val rightR = rightColor.red * Hardware.SENSORS.MAXIMUM_READING
        val rightG = rightColor.green * Hardware.SENSORS.MAXIMUM_READING
        val rightB = rightColor.blue * Hardware.SENSORS.MAXIMUM_READING

        if(leftR > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD || leftG > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD || leftB > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD ||
            rightR > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD || rightG > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD || rightB > Configs.SIMPLE_STORAGE.COLOR_THRESHOLD)
            detectEvent.invoke()
    }

    override fun init(hardwareMap: HardwareMap) {
        _leftColor = hardwareMap.get("leftColor") as RevColorSensorV3
        _rightColor = hardwareMap.get("rightColor") as RevColorSensorV3
    }

    override fun opModeStart() {

    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}