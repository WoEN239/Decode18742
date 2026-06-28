package org.woen.configs


import com.acmerobotics.dashboard.config.Config



@Config
internal object DelayMS
{
    @Config
    internal object PUSH
    {
        @JvmField
        var FULL: Long = 160
        val HALF get() = FULL / 2

        @JvmField
        var PART: Long = 80

        @JvmField
        var READJUSTING: Long = 100
    }

    @Config
    internal object SHOOTING
    {
        @JvmField
        var ADDITIONAL_TOLERANCE_FOR_AUTONOMOUS: Long = 100
        @JvmField
        var ADDITIONAL_TOLERANCE_FOR_TELEOP: Long = 20


        @JvmField
        var FAST_3: Long = 150

        @JvmField
        var FAST_2: Long = 100

        @JvmField
        var FAST_1: Long = 50

        @JvmField
        var FAST_LAST_WITH_LAUNCHER: Long = 40



        @JvmField
        var SLOW_3: Long = 440

        @JvmField
        var SLOW_2: Long = 280

        @JvmField
        var SLOW_1: Long = 140


        @JvmField
        var SLOW_CONSIDER_SHOT_FIRED: Long = 23
        @JvmField
        var BETWEEN_SHOTS: Long = 100

        @JvmField
        var SLOW_LAST_WITH_LAUNCHER: Long = 50
    }


    @Config
    internal object REALIGNMENT
    {
        @JvmField
        var SORTING_FORWARD: Long = 0

        @JvmField
        var SORTING_REVERSE: Long = 80


        @JvmField
        var WAITING_IN_SORTING_PASE_7: Long = 0
    }

    @JvmField
    var BRUSH_REVERSE: Long = 500

    @JvmField
    var GAMEPAD_RUMBLE_STORAGE_IS_NOW_FULL = 500
}