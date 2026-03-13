package org.woen.modules.scoringSystem.simple

import android.annotation.SuppressLint
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.enumerators.Ball
import org.woen.hotRun.HotRun
import org.woen.modules.scoringSystem.storage.sorting.hardware.ColorSensorsData

import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.Hardware
import org.woen.telemetry.ThreadedTelemetry
import org.woen.utils.events.SimpleEmptyEvent
import kotlin.math.max
import kotlin.math.min


class ControlHardwareSimpleStorage : IHardwareDevice {
    private lateinit var _leftColor: RevColorSensorV3
    private lateinit var _rightColor: RevColorSensorV3
    
    val detectEvent = SimpleEmptyEvent()
    
    private var _leftR = 0.0
    private var _leftG = 0.0
    private var _leftB = 0.0
    private var _rightR = 0.0
    private var _rightG = 0.0
    private var _rightB = 0.0

    override fun update() {
        val leftColor = _leftColor.normalizedColors
        val rightColor = _rightColor.normalizedColors

        _leftR = leftColor.red * Hardware.SENSORS.MAXIMUM_READING
        _leftG = leftColor.green * Hardware.SENSORS.MAXIMUM_READING
        _leftB = leftColor.blue * Hardware.SENSORS.MAXIMUM_READING

        _rightR = rightColor.red * Hardware.SENSORS.MAXIMUM_READING
        _rightG = rightColor.green * Hardware.SENSORS.MAXIMUM_READING
        _rightB = rightColor.blue * Hardware.SENSORS.MAXIMUM_READING

        val leftGreen =
            (_leftG - max(_leftR, _leftB)) > Hardware.SENSORS.GREEN_THRESHOLD_LEFT
        val rightGreen =
            (_rightG - max(_rightR, _rightB)) > Hardware.SENSORS.GREEN_THRESHOLD_RIGHT

        val combinedGreen = leftGreen || rightGreen

        val maxRightC = max(max(_rightR, _rightG), _rightB)
        val minRightC = min(min(_rightR, _rightG), _rightB)

        val difRight = maxRightC - minRightC

        val rightH =
            when (maxRightC) {
                _rightR -> ((_rightG - _rightB) / difRight)
                _rightG -> (2.0 + (_rightB - _rightR) / difRight)
                else -> (4.0 + (_rightR - _rightG) / difRight)
            }

        val maxLeftC = max(max(_leftR, _leftG), _leftB)
        val minLeftC = min(min(_leftR, _leftG), _leftB)

        val difLeft = maxLeftC - minLeftC

        val leftH =
            when (maxLeftC) {
                _leftR -> ((_leftG - _leftB) / difLeft)
                _leftG -> (2.0 + (_leftB - _leftR) / difLeft)
                else -> (4.0 + (_leftR - _leftG) / difLeft)
            }

        val leftPurple =
            leftH in (Hardware.SENSORS.MIN_PURPLE_H_LEFT..Hardware.SENSORS.MAX_PURPLE_H_LEFT)
        val rightPurple =
            rightH in (Hardware.SENSORS.MIN_PURPLE_H_RIGHT..Hardware.SENSORS.MAX_PURPLE_H_RIGHT)

        val combinedPurple = leftPurple || rightPurple

        if(combinedGreen || combinedPurple)
            detectEvent.invoke()
    }

    @SuppressLint("DefaultLocale")
    override fun init(hardwareMap: HardwareMap) {
        _leftColor = hardwareMap.get("leftColor") as RevColorSensorV3
        _rightColor = hardwareMap.get("rightColor") as RevColorSensorV3

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _rightColor.gain = 4.0f
            _leftColor.gain = 4.0f
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addLine(
                "left = ${String.format("%.1f", _leftR)} ${
                    String.format(
                        "%.1f",
                        _leftG
                    )
                } ${String.format("%.1f", _leftB)}"
            )
            it.addLine(
                "right = ${String.format("%.1f", _rightR)} ${
                    String.format(
                        "%.1f",
                        _rightG
                    )
                } ${String.format("%.1f", _rightB)}"
            )
        }
    }

    override fun opModeStart() {

    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}