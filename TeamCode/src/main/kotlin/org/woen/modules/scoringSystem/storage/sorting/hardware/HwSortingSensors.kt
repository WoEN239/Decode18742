package org.woen.modules.scoringSystem.storage.sorting.hardware

import kotlin.math.max
import kotlin.math.min
import android.annotation.SuppressLint

import org.woen.hotRun.HotRun
import org.woen.enumerators.Ball

import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.utils.events.SimpleEvent
import org.woen.utils.events.SimpleEmptyEvent
import org.woen.utils.updateCounter.UpdateCounter

import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_L
import org.woen.telemetry.configs.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_R



class ColorSensorsData(var color: Ball.Name)



class HwSortingSensors() : IHardwareDevice {
    private lateinit var _rightColor: RevColorSensorV3
    private lateinit var _leftColor: RevColorSensorV3

    private var _oldCombinedGreen = false
    private var _oldCombinedPurple = false

    private val _greenTimer = ElapsedTime()
    private val _purpleTimer = ElapsedTime()

    val opticDetectedShotFiringEvent = SimpleEmptyEvent()
    val colorSensorsDetectedIntakeEvent = SimpleEvent<ColorSensorsData>()
    private val _updatesCounter = UpdateCounter()

    private var _leftR = 0.0
    private var _leftG = 0.0
    private var _leftB = 0.0
    private var _rightR = 0.0
    private var _rightG = 0.0
    private var _rightB = 0.0
    private var _rightH = 0.0
    private var _leftH = 0.0

    private var _doubleCounter = 0


    @SuppressLint("DefaultLocale")
    override fun init(hardwareMap: HardwareMap) {
        _rightColor = hardwareMap.get(INTAKE_COLOR_SENSOR_R) as RevColorSensorV3
        _leftColor = hardwareMap.get(INTAKE_COLOR_SENSOR_L) as RevColorSensorV3

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _rightColor.gain = 4.0f
            _leftColor.gain = 4.0f
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("color sensor updates", String.format("%.1f", _updatesCounter.currentUPS))
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
            it.addLine("hueRight ${String.format("%.1f", _rightH)}")
            it.addLine("hueLeft ${String.format("%.1f", _leftH)}")
        }
    }

    override fun update() {
        try {
            val leftColor = _leftColor.normalizedColors
            val rightColor = _rightColor.normalizedColors

            _leftR = leftColor.red * Configs.STORAGE_SENSORS.MAXIMUM_READING
            _leftG = leftColor.green * Configs.STORAGE_SENSORS.MAXIMUM_READING
            _leftB = leftColor.blue * Configs.STORAGE_SENSORS.MAXIMUM_READING

            _rightR = rightColor.red * Configs.STORAGE_SENSORS.MAXIMUM_READING
            _rightG = rightColor.green * Configs.STORAGE_SENSORS.MAXIMUM_READING
            _rightB = rightColor.blue * Configs.STORAGE_SENSORS.MAXIMUM_READING

            val leftGreen =
                (_leftG - max(_leftR, _leftB)) > Configs.STORAGE_SENSORS.GREEN_THRESHOLD_LEFT
            val rightGreen =
                (_rightG - max(_rightR, _rightB)) > Configs.STORAGE_SENSORS.GREEN_THRESHOLD_RIGHT

            val combinedGreen = leftGreen || rightGreen

            if (combinedGreen && !_oldCombinedGreen) {
                colorSensorsDetectedIntakeEvent.invoke(ColorSensorsData(Ball.Name.GREEN))
                _greenTimer.reset()
                _doubleCounter = 0
            } else if (combinedGreen && _greenTimer.seconds() > Configs.STORAGE_SENSORS.DOUBLE_DETECT_TIMER &&
                _doubleCounter < Configs.STORAGE_SENSORS.DOUBLE_DETECT_COUNT_MAX
            ) {
                colorSensorsDetectedIntakeEvent.invoke(ColorSensorsData(Ball.Name.GREEN))
                _greenTimer.reset()
                _doubleCounter++
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
                _leftH in (Configs.STORAGE_SENSORS.MIN_PURPLE_H_LEFT..Configs.STORAGE_SENSORS.MAX_PURPLE_H_LEFT)
            val rightPurple =
                _rightH in (Configs.STORAGE_SENSORS.MIN_PURPLE_H_RIGHT..Configs.STORAGE_SENSORS.MAX_PURPLE_H_RIGHT)

            val combinedPurple = leftPurple || rightPurple

            if (combinedPurple && !_oldCombinedPurple) {
                colorSensorsDetectedIntakeEvent.invoke(ColorSensorsData(Ball.Name.PURPLE))
                _purpleTimer.reset()
                _doubleCounter = 0
            } else if (combinedPurple && _purpleTimer.seconds() > Configs.STORAGE_SENSORS.DOUBLE_DETECT_TIMER &&
                _doubleCounter < Configs.STORAGE_SENSORS.DOUBLE_DETECT_COUNT_MAX
            ) {
                colorSensorsDetectedIntakeEvent.invoke(ColorSensorsData(Ball.Name.PURPLE))
                _purpleTimer.reset()
                _doubleCounter++
            }

            _oldCombinedPurple = combinedPurple
        }
        catch (_: Exception){

        }
    }

    override fun opModeStart() {}
    override fun opModeStop() {}

    override fun dispose() {}
}