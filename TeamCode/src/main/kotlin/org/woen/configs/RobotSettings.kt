package org.woen.configs


import com.acmerobotics.dashboard.config.Config

import org.woen.enumerators.Shooting
import org.woen.enumerators.StorageSlot
import org.woen.enumerators.StockPattern



object RobotSettings
{
    @Config
    internal object ROBOT
    {
        @JvmField
        var AUTONOMOUS_INITIAL_LOAD_FROM_TURRET_TO_BOTTOM = StockPattern.Storage.GPP
        @JvmField
        var TELEOP_INITIAL_LOAD_FROM_TURRET_TO_BOTTOM = StockPattern.Storage.EMPTY


        @JvmField
        var INTAKE_INPUT_ORDER = arrayOf(
            StorageSlot.TURRET,
            StorageSlot.CENTER,
            StorageSlot.BOTTOM)

        @JvmField
        var REQUEST_SEARCH_ORDER = arrayOf(
            StorageSlot.TURRET,
            StorageSlot.CENTER,
            StorageSlot.BOTTOM,
            StorageSlot.MOBILE)
    }


    @Config
    internal object CONTROLS
    {
        @JvmField
        var USE_LAZY_VERSION_OF_STREAM_DRUM = true

        @JvmField
        var USE_AUTO_SHOOTING_WHEN_IN_ZONE = false
        @JvmField
        var DISABLE_AUTO_SHOOTING_IN_END_GAME = true


        @JvmField
        var TRY_AUTO_SORT_WHEN_FULL_IN_ENDGAME = false

        @JvmField
        var USE_LAUNCHER_FOR_LAST_BALL = true
        @JvmField
        var USE_LAUNCHER_AFTER_GAMEPAD_HOLD_SHOOT = true


        @JvmField
        var HOLD_FOR_MANUAL_SHOOTING = true

        @JvmField
        var DO_SORTING_TEST_100_ON_TOUCHPAD_1_PRESSED = true
        @JvmField
        var USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION = false
    }


    @Config
    internal object SORTING
    {
        @Config
        internal object PREDICT
        {
            @JvmField
            var START_WEIGHT = 0.0

            @JvmField
            var TRUE_MATCH_WEIGHT = 1.0

            @JvmField
            var PSEUDO_MATCH_WEIGHT = 0.9
        }
    }

    @Config
    internal object AUTONOMOUS
    {
        @JvmField
        var AUTOCORRECT_REQUEST_PATTERN = true
        @JvmField
        var AUTOCORRECT_FAILSAFE_PATTERN = false

        @JvmField
        var PATTERN_SHOOTING_MODE = Shooting.Mode.FIRE_PATTERN_CAN_SKIP

        @JvmField
        var FAILSAFE_SHOOTING_MODE = Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
        @JvmField
        var FAILSAFE_PATTERN = StockPattern.Request.STREAM


        @JvmField
        var IGNORE_COLOR_SENSORS = true
    }


    @Config
    internal object TELEOP
    {
        @JvmField
        var AUTOCORRECT_REQUEST_PATTERN  = true
        @JvmField
        var AUTOCORRECT_FAILSAFE_PATTERN = false


        @JvmField
        var IGNORE_COLOR_SENSORS = false

        @JvmField
        var PATTERN_SHOOTING_MODE = Shooting.Mode.FIRE_PATTERN_CAN_SKIP
    }
}