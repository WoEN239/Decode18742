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
            var FAST_3: Long = 333

            @JvmField
            var FAST_2: Long = 250

            @JvmField
            var FAST_1: Long = 125

            @JvmField
            var FAST_LAST_WITH_LAUNCHER: Long = 100



            @JvmField
            var SLOW_3: Long = 510

            @JvmField
            var SLOW_2: Long = 400

            @JvmField
            var SLOW_1: Long = 200

            @JvmField
            var SLOW_LAST_WITH_LAUNCHER: Long = 150


            @JvmField
            var BEFORE_AUTOSHOT: Long = 80


            @JvmField
            var FAST_CONSIDER_SHOT_FIRED: Long = 110
            @JvmField
            var SLOW_CONSIDER_SHOT_FIRED: Long = 165
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