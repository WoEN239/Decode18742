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
            var FULL: Long = 400
            val HALF get() = FULL / 2

            @JvmField
            var PART: Long = 300
        }

        @Config
        internal object SHOOTING
        {
            @JvmField
            var FIRE_3: Long = 400

            @JvmField
            var FIRE_2: Long = 275

            @JvmField
            var FIRE_1: Long = 150

            @JvmField
            var FIRE_LAST_WITH_LAUNCHER: Long = 150


            @JvmField
            var BEFORE_AUTOSHOT: Long = 50

            @JvmField
            var CONSIDER_SHOT_FIRED: Long = 133
        }


        @Config
        internal object REALIGNMENT
        {
            @JvmField
            var SORTING_FORWARD: Long = 222

            @JvmField
            var SORTING_REVERSE: Long = 85


            @JvmField
            var WAITING_IN_SORTING_PASE_7: Long = 80
        }

        @JvmField
        var BRUSH_REVERSE: Long = 1500

        @JvmField
        var GAMEPAD_RUMBLE_STORAGE_IS_NOW_FULL = 500
    }

    @Config
    internal object SEC
    {
        @JvmField
        var GAMEPAD_RUMBLE_STORAGE_IS_NOW_FULL = 0.5
    }
}