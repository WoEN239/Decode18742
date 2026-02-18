package org.woen.telemetry.configs

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
            )   )   )
        ).toDouble()

        @JvmField
        var GREEN_THRESHOLD_RIGHT = 60.0

        @JvmField
        var GREEN_THRESHOLD_LEFT = 60.0

        @JvmField
        var MIN_PURPLE_H_RIGHT = 3.0

        @JvmField
        var MAX_PURPLE_H_RIGHT = 4.0

        @JvmField
        var MIN_PURPLE_H_LEFT = 3.0

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



    @Config
    internal object VALUES
    {
        internal object SERVO
        {
            @JvmField
            var GATE_OPEN = 0.78
            @JvmField
            var GATE_CLOSE = 0.355


            @JvmField
            var PUSH_OPEN = 0.31
            @JvmField
            var PUSH_CLOSE = 0.545


            @JvmField
            var LAUNCH_OPEN = 0.58
            @JvmField
            var LAUNCH_CLOSE = 0.96


            @JvmField
            var TURRET_GATE_OPEN = 0.7
            @JvmField
            var TURRET_GATE_CLOSE = 0.36
        }

        internal object BELTS
        {
            @JvmField
            var STORAGE_CURRENT_WHEN_FULL = 8.1

            @JvmField
            var MOTORS_DIRECTION = DcMotorSimple.Direction.REVERSE


            internal object POWER
            {
                @JvmField
                var SHOOT = 11.0

                @JvmField
                var LAZY  = 10.0


                @JvmField
                var FAST  = 11.0

                @JvmField
                var SLOW  = 10.0
            }
        }
    }
}