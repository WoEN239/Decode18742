package org.woen.telemetry.configs

import com.acmerobotics.dashboard.config.Config
import kotlin.math.ceil
import kotlin.math.max


object Hardware
{
    @Config
    internal object SENSORS
    {
        @JvmField
        var MAXIMUM_READING = (65535.coerceAtMost(
            1024 * (256 - max(
                        0,
                        256 - ceil((24 / 2.4f).toDouble()).toInt()
            )   )   )
        ).toDouble()

        @JvmField
        var GREEN_THRESHOLD_RIGHT = 50.0

        @JvmField
        var GREEN_THRESHOLD_LEFT = 70.0

        @JvmField
        var MIN_PURPLE_H_RIGHT = 2.9

        @JvmField
        var MAX_PURPLE_H_RIGHT = 4.0

        @JvmField
        var MIN_PURPLE_H_LEFT = 2.8

        @JvmField
        var MAX_PURPLE_H_LEFT = 4.0

        @JvmField
        var DOUBLE_DETECT_TIMER = 0.4

        @JvmField
        var DOUBLE_DETECT_COUNT_MAX = 2
    }



    @Config
    internal object DEVICE_NAMES
    {
        @JvmField
        var INTAKE_COLOR_SENSOR_L = "leftColorSensor"

        @JvmField
        var INTAKE_COLOR_SENSOR_R = "rightColorSensor"


        @JvmField
        var TURRET_OPTIC_1 = "optic1"

        @JvmField
        var TURRET_OPTIC_2 = "optic2"


        @JvmField
        var TURRET_GATE_SERVO = "turretGateServo"


        @JvmField
        var STORAGE_BELT_MOTOR = "beltMotor"


        @JvmField
        var GATE_SERVO = "gateServo"

        @JvmField
        var PUSH_SERVO = "pushServo"

        @JvmField
        var LAUNCH_SERVO = "launchServo"
    }
}