package org.woen.modules.scoringSystem.storage.sorting.hardware

import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.enumerators.Ball
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.telemetry.Configs.DEBUG_LEVELS.SENSORS_DEBUG_LEVELS
import org.woen.telemetry.Configs.DEBUG_LEVELS.SENSORS_DEBUG_SETTING
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_L
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_R
import org.woen.telemetry.LogManager
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.events.SimpleEmptyEvent
import org.woen.utils.events.SimpleEvent
import kotlin.math.max
import kotlin.math.min

class ColorSensorsData(var color: Ball.Name)

class HwSortingSensors() : IHardwareDevice {
    private lateinit var _rightColor: RevColorSensorV3
    private lateinit var _leftColor: RevColorSensorV3

    val logM = LogManager(
        SENSORS_DEBUG_SETTING,
        SENSORS_DEBUG_LEVELS, "SENSORS"
    )

    private var _oldCombinedGreen = false
    private var _oldCombinedPurple = false

    val opticDetectedShotFiringEvent = SimpleEmptyEvent()
    val colorSensorsDetectedIntakeEvent = SimpleEvent<ColorSensorsData>()

    override fun init(hardwareMap: HardwareMap) {
        _rightColor = hardwareMap.get(INTAKE_COLOR_SENSOR_L) as RevColorSensorV3
        _leftColor = hardwareMap.get(INTAKE_COLOR_SENSOR_R) as RevColorSensorV3

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _rightColor.gain = 4.0f
            _leftColor.gain = 4.0f
        }
    }

    override fun update() {
        val leftColor = _leftColor.normalizedColors
        val rightColor = _rightColor.normalizedColors

        val leftR = leftColor.red * Configs.STORAGE_SENSORS.MAXIMUM_READING
        val leftG = leftColor.green * Configs.STORAGE_SENSORS.MAXIMUM_READING
        val leftB = leftColor.blue * Configs.STORAGE_SENSORS.MAXIMUM_READING

        val rightR = rightColor.red * Configs.STORAGE_SENSORS.MAXIMUM_READING
        val rightG = rightColor.green * Configs.STORAGE_SENSORS.MAXIMUM_READING
        val rightB = rightColor.blue * Configs.STORAGE_SENSORS.MAXIMUM_READING

        val leftGreen = (leftG - max(leftR, leftB)) > Configs.STORAGE_SENSORS.GREEN_THRESHOLD
        val rightGreen = (rightG - max(rightR, rightB)) > Configs.STORAGE_SENSORS.GREEN_THRESHOLD

        val combinedGreen = leftGreen || rightGreen

        if(combinedGreen && !_oldCombinedGreen)
            colorSensorsDetectedIntakeEvent.invoke(ColorSensorsData(Ball.Name.GREEN))

        _oldCombinedGreen = combinedGreen

        val maxRightC = max(max(rightR, rightG), rightB)
        val minRightC = min(min(rightR, rightG), rightB)

        val difRight = maxRightC - minRightC

        val rightH =
            when (maxRightC) {
                rightR -> ((rightG - rightB) / difRight)
                rightG -> (2.0 + (rightB - rightR) / difRight)
                else -> (4.0 + (rightR - rightG) / difRight)
            }

        val maxLeftC = max(max(leftR, leftG), leftB)
        val minLeftC = min(min(leftR, leftG), leftB)

        val difLeft = maxLeftC - minLeftC

        val leftH =
            when (maxLeftC) {
                leftR -> ((leftG - leftB) / difLeft)
                leftG -> (2.0 + (leftB - leftR) / difLeft)
                else -> (4.0 + (leftR - leftG) / difLeft)
            }

        val leftPurple = leftH in (Configs.STORAGE_SENSORS.MIN_PURPLE_H..Configs.STORAGE_SENSORS.MAX_PURPLE_H)
        val rightPurple = rightH in (Configs.STORAGE_SENSORS.MIN_PURPLE_H..Configs.STORAGE_SENSORS.MAX_PURPLE_H)

        val combinedPurple = leftPurple || rightPurple

        if(combinedPurple && !_oldCombinedPurple)
            colorSensorsDetectedIntakeEvent.invoke(ColorSensorsData(Ball.Name.PURPLE))

        _oldCombinedPurple = combinedPurple
    }

    override fun opModeStart() {}
    override fun opModeStop() {}

    override fun dispose() {}
}