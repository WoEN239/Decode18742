package org.woen.configs

import com.acmerobotics.dashboard.config.Config



object Delay
{
    @Config
    internal object MS
    {
        @Config
        internal object PUSH
        {
            @JvmField
            var FULL: Long = 444
            val HALF get() = FULL / 2

            @JvmField
            var PART: Long = 333
        }

        @Config
        internal object SHOOTING
        {
            @JvmField
            var FIRE_3: Long = 500

            @JvmField
            var FIRE_2: Long = 400

            @JvmField
            var FIRE_1: Long = 200


            @JvmField
            var BEFORE_AUTOSHOT: Long = 50
        }


        @Config
        internal object REALIGNMENT
        {
            @JvmField
            var SORTING_FORWARD: Long = 444

            @JvmField
            var SORTING_REVERSE: Long = 85
        }


        @JvmField
        var BRUSH_REVERSE: Long = 1500

        @JvmField
        var HW_REQUEST_FREQUENCY: Long = 5
    }

    @Config
    internal object SEC
    {
        @JvmField
        var GAMEPAD_RUMBLE_STORAGE_IS_NOW_FULL = 0.6
    }
}