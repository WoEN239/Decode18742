package org.woen.scoringSystem.storage.hardware


import kotlin.math.max
import kotlin.math.min

import com.qualcomm.robotcore.util.ElapsedTime
import com.qualcomm.hardware.rev.RevColorSensorV3

import org.woen.configs.Hardware
import org.woen.enumerators.Ball
import org.woen.scoringSystem.ConnectorModuleStatus



class HwSensors
{
    private var _cms: ConnectorModuleStatus
    private var _rightColor: RevColorSensorV3
    private var _leftColor: RevColorSensorV3

    private var _oldCombinedGreen = false
    private var _oldCombinedPurple = false

    private val _greenTimer = ElapsedTime()
    private val _purpleTimer = ElapsedTime()


    private var _leftR = 0.0
    private var _leftG = 0.0
    private var _leftB = 0.0
    private var _rightR = 0.0
    private var _rightG = 0.0
    private var _rightB = 0.0
    private var _rightH = 0.0
    private var _leftH = 0.0

    private var _doubleCounter = 0


    constructor(cms: ConnectorModuleStatus) {
        _cms = cms

        _rightColor = _cms.collector.hardwareMap.get(
            Hardware.DEVICE_NAMES.COLOR_SENSOR_R)
                as RevColorSensorV3
        _leftColor = _cms.collector.hardwareMap.get(
            Hardware.DEVICE_NAMES.COLOR_SENSOR_L)
                as RevColorSensorV3

        _cms.collector.startEvent += {
            _rightColor.gain = 4.0f
            _leftColor.gain = 4.0f
        }

//        _cms.collector.telemetry.onTelemetrySend += {
//            it.addData("color sensor updates", String.format("%.1f", _updatesCounter.currentUPS))
//            it.addLine(
//                "left = ${String.format("%.1f", _leftR)} ${
//                    String.format(
//                        "%.1f",
//                        _leftG
//                    )
//                } ${String.format("%.1f", _leftB)}"
//            )
//            it.addLine(
//                "right = ${String.format("%.1f", _rightR)} ${
//                    String.format(
//                        "%.1f",
//                        _rightG
//                    )
//                } ${String.format("%.1f", _rightB)}"
//            )
//            it.addLine("hueRight ${String.format("%.1f", _rightH)}")
//            it.addLine("hueLeft ${String.format("%.1f", _leftH)}")
//        }
    }

    fun update(): Ball.Name {
        try {
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

            if (combinedGreen && !_oldCombinedGreen) {
                _greenTimer.reset()
                _doubleCounter = 0
                return Ball.Name.GREEN
            } else if (combinedGreen && _greenTimer.seconds() > Hardware.SENSORS.DOUBLE_DETECT_TIMER &&
                _doubleCounter < Hardware.SENSORS.DOUBLE_DETECT_COUNT_MAX
            ) {
                _doubleCounter++
                _greenTimer.reset()
                return Ball.Name.GREEN
            }

            _oldCombinedGreen = combinedGreen

            val maxRightC = max(max(_rightR, _rightG), _rightB)
            val minRightC = min(min(_rightR, _rightG), _rightB)

            val difRight = maxRightC - minRightC

            _rightH =
                when (maxRightC) {
                    _rightR -> ((_rightG - _rightB) / difRight)
                    _rightG -> (2.0 + (_rightB - _rightR) / difRight)
                    else -> (4.0 + (_rightR - _rightG) / difRight)
                }

            val maxLeftC = max(max(_leftR, _leftG), _leftB)
            val minLeftC = min(min(_leftR, _leftG), _leftB)

            val difLeft = maxLeftC - minLeftC

            _leftH =
                when (maxLeftC) {
                    _leftR -> ((_leftG - _leftB) / difLeft)
                    _leftG -> (2.0 + (_leftB - _leftR) / difLeft)
                    else -> (4.0 + (_leftR - _leftG) / difLeft)
                }

            val leftPurple =
                _leftH in (Hardware.SENSORS.MIN_PURPLE_H_LEFT..Hardware.SENSORS.MAX_PURPLE_H_LEFT)
            val rightPurple =
                _rightH in (Hardware.SENSORS.MIN_PURPLE_H_RIGHT..Hardware.SENSORS.MAX_PURPLE_H_RIGHT)

            val combinedPurple = leftPurple || rightPurple

            if (combinedPurple && !_oldCombinedPurple) {
                _purpleTimer.reset()
                _doubleCounter = 0
                return Ball.Name.PURPLE
            } else if (combinedPurple && _purpleTimer.seconds() > Hardware.SENSORS.DOUBLE_DETECT_TIMER &&
                _doubleCounter < Hardware.SENSORS.DOUBLE_DETECT_COUNT_MAX
            ) {
                _purpleTimer.reset()
                _doubleCounter++
                return Ball.Name.PURPLE
            }

            _oldCombinedPurple = combinedPurple
        } catch (_: Exception) { }
        return Ball.Name.NONE
    }
}