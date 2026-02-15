package org.woen.telemetry.configs

import com.acmerobotics.dashboard.config.Config



object Delay
{
    @Config
    internal object MS
    {
        internal object RACE_CONDITION
        {
            @JvmField
            var INTAKE: Long = 20
            @JvmField
            var LAZY_INTAKE: Long = 20
            @JvmField
            var UPDATE_AFTER_LAZY_INTAKE: Long = 20


            @JvmField
            var SINGLE_REQUEST: Long = 10
            @JvmField
            var DRUM_REQUEST:   Long = 10


            @JvmField
            var PREDICT_SORT: Long = 15
            @JvmField
            var STORAGE_CALIBRATION: Long = 15
        }

        internal object PUSH
        {
            @JvmField
            var FULL: Long = 444

            val HALF get() = FULL / 2

            @JvmField
            var PART: Long = 333
        }

        internal object SHOOTING
        {
            @JvmField
            var FIRE_3: Long = 500

            @JvmField
            var FIRE_2: Long = 400

            @JvmField
            var FIRE_1: Long = 200
        }


        internal object REALIGNMENT
        {
            @JvmField
            var SORTING_FORWARD: Long = 444
            @JvmField
            var SORTING_REVERSE: Long = 75

            @JvmField
            var SHOOTING_FORWARD: Long = 222
        }


        internal object AWAIT
        {
            @JvmField
            var EVENTS: Long = 5

            @JvmField
            var ODOMETRY_TURNING: Long = 33333 / 3


            @JvmField
            var SMC_SHOT: Long = 60
            @JvmField
            var SSL_SHOT: Long = 245       //  8.5V configuration
        }


        @JvmField
        var BETWEEN_SHOTS: Long = 100
        @JvmField
        var BETWEEN_INTAKES: Long = 500


        @JvmField
        var HW_REQUEST_FREQUENCY: Long = 5

        @JvmField
        var IGNORE_BELTS_CURRENT_AFTER_START: Long = 200
    }

    internal object SEC
    {

    }
}