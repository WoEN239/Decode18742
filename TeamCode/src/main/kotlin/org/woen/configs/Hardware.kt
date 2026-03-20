package org.woen.configs


import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotorSimple
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
        )   )   )   ).toDouble()

        @JvmField
        var GREEN_THRESHOLD_RIGHT = 80.0

        @JvmField
        var GREEN_THRESHOLD_LEFT = 80.0

        @JvmField
        var MIN_PURPLE_H_RIGHT = 2.8

        @JvmField
        var MAX_PURPLE_H_RIGHT = 4.0

        @JvmField
        var MIN_PURPLE_H_LEFT = 2.8

        @JvmField
        var MAX_PURPLE_H_LEFT = 4.0

        @JvmField
        var DOUBLE_DETECT_TIMER = 0.5

        @JvmField
        var DOUBLE_DETECT_COUNT_MAX = 2
    }

    @Config
    internal object DEVICE_NAMES
    {
        @JvmField
        var COLOR_SENSOR_L = "leftColor"

        @JvmField
        var COLOR_SENSOR_R = "rightColor"


        @JvmField
        var TURRET_OPTIC_L = "leftOptic"

        @JvmField
        var TURRET_OPTIC_R = "rightOptic"


        @JvmField
        var STORAGE_BELT_MOTOR = "beltMotor"
        @JvmField
        var BRUSH_MOTOR = "brushMotor"


        @JvmField
        var GATE_SERVO   = "gateServo"

        @JvmField
        var PUSH_SERVO   = "pushServo"

        @JvmField
        var LAUNCH_SERVO = "launchServo"

        @JvmField
        var TURRET_GATE_SERVO = "turretGateServo"
    }

    @Config
    internal object SERVO
    {
        @JvmField
        var GATE_OPEN = 0.42
        @JvmField
        var GATE_CLOSE = 0.0


        @JvmField
        var PUSH_OPEN = 0.025
        @JvmField
        var PUSH_CLOSE = 0.275


        @JvmField
        var LAUNCH_OPEN = 0.58
        @JvmField
        var LAUNCH_CLOSE = 0.96


        @JvmField
        var TURRET_GATE_OPEN = 0.72
        @JvmField
        var TURRET_GATE_CLOSE = 0.45
    }

    @Config
    internal object MOTOR
    {
        @JvmField
        var BELTS_DIRECTION = DcMotorSimple.Direction.FORWARD

        @JvmField
        var BELTS_FORWARD = 11.0
        @JvmField
        var BELTS_REVERSE  = 10.0


        @JvmField
        var BRUSH_FORWARD = 12.0
        @JvmField
        var BRUSH_REVERSE = 9.0
    }
}