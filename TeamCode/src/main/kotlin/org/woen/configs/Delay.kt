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
            var FULL: Long = 515
            val HALF get() = FULL / 2

            @JvmField
            var PART: Long = 300
        }

        @Config
        internal object SHOOTING
        {
            @JvmField
            var FIRE_3: Long = 444

            @JvmField
            var FIRE_2: Long = 300

            @JvmField
            var FIRE_1: Long = 190

            @JvmField
            var FIRE_LAST_WITH_LAUNCHER: Long = 100


            @JvmField
            var BEFORE_AUTOSHOT: Long = 40

            @JvmField
            var CONSIDER_SHOT_FIRED: Long = 140
        }


        @Config
        internal object REALIGNMENT
        {
            @JvmField
            var SORTING_FORWARD: Long = 170

            @JvmField
            var SORTING_REVERSE: Long = 0


            @JvmField
            var WAITING_IN_SORTING_PASE_7: Long = 0
        }

        @JvmField
        var BRUSH_FORWARD: Long = 800
        @JvmField
        var BRUSH_REVERSE: Long = 500

        @JvmField
        var GAMEPAD_RUMBLE_STORAGE_IS_NOW_FULL = 500
    }
}