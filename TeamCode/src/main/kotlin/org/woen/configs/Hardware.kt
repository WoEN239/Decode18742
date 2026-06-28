@file:Suppress("ClassName")
package org.woen.configs


import kotlin.math.max
import kotlin.math.ceil
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotorSimple



internal object Hardware
{
    @Config
    internal object SENSORS
    {
        val MAXIMUM_READING = (65535.coerceAtMost(
            1024 * (256 - max(
                0,
                256 - ceil((24 / 2.4f).toDouble()).toInt()
        )   )   )   ).toDouble()

        @JvmField
        var GREEN_THRESHOLD_BOTTOM = 80.0
        @JvmField
        var GREEN_THRESHOLD_CENTER = 80.0
        @JvmField
        var GREEN_THRESHOLD_TURRET = 80.0


        @JvmField
        var MIN_PURPLE_H_BOTTOM = 2.8
        @JvmField
        var MIN_PURPLE_H_CENTER = 2.8
        @JvmField
        var MIN_PURPLE_H_TURRET = 2.8


        @JvmField
        var MAX_PURPLE_H_BOTTOM = 4.0
        @JvmField
        var MAX_PURPLE_H_CENTER = 4.0
        @JvmField
        var MAX_PURPLE_H_TURRET = 4.0


        @JvmField
        var UNKNOWN_MIN_R_BOTTOM = 100
        @JvmField
        var UNKNOWN_MIN_G_BOTTOM = 100
        @JvmField
        var UNKNOWN_MIN_B_BOTTOM = 100

        @JvmField
        var UNKNOWN_MIN_R_CENTER = 100
        @JvmField
        var UNKNOWN_MIN_G_CENTER = 100
        @JvmField
        var UNKNOWN_MIN_B_CENTER = 100

        @JvmField
        var UNKNOWN_MIN_R_TURRET = 100
        @JvmField
        var UNKNOWN_MIN_G_TURRET = 100
        @JvmField
        var UNKNOWN_MIN_B_TURRET = 100
    }

    @Config
    internal object DEVICE_NAMES
    {
        @JvmField
        var COLOR_SENSOR_BOTTOM = "bottomColor"
        @JvmField
        var COLOR_SENSOR_CENTER = "centerColor"
        @JvmField
        var COLOR_SENSOR_TURRET = "turretColor"


        @JvmField
        var STORAGE_BELT_MOTOR = "beltsMotor"
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
        var GATE_OPEN = 0.9
        @JvmField
        var GATE_CLOSE = 0.35


        @JvmField
        var PUSH_OPEN = 0.27
        @JvmField
        var PUSH_CLOSE = 0.095


        @JvmField
        var LAUNCH_OPEN = 0.6
        @JvmField
        var LAUNCH_CLOSE = 0.955


        @JvmField
        var TURRET_GATE_OPEN = 0.735
        @JvmField
        var TURRET_GATE_CLOSE = 0.45
    }

    @Config
    internal object MOTOR
    {
        @JvmField
        var ACCELERATION_PHASES_LIMIT: Double = 3.0

        @JvmField
        var ACC_PHASE_K: Double = 3.0


        @JvmField
        var BELTS_DIRECTION = DcMotorSimple.Direction.FORWARD
        @JvmField
        var BRUSH_DIRECTION = DcMotorSimple.Direction.FORWARD

        @JvmField
        var BELTS_FORWARD = 11.0//8.9
        @JvmField
        var BELTS_REVERSE  = 10.0


        @JvmField
        var BELTS_FOR_FAST_SHOOTING = 12.0
        @JvmField
        var BELTS_FOR_SLOW_SHOOTING = 10.5


        @JvmField
        var BRUSH_FORWARD = 12.0
        @JvmField
        var BRUSH_REVERSE = 9.0
    }
}