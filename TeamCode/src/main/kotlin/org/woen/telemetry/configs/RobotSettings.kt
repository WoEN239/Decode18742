package org.woen.telemetry.configs

import com.acmerobotics.dashboard.config.Config

import org.woen.enumerators.Shooting
import org.woen.enumerators.StorageSlot



object RobotSettings
{
    @Config
    internal object ROBOT
    {
        @JvmField
        var INITIAL_LOAD_FROM_TURRET_TO_BOTTOM = Shooting.StockPattern.Sequence.Storage.EMPTY


        @JvmField
        var PREFERRED_INTAKE_SLOT_SEARCHING_ORDER = arrayOf(
            StorageSlot.Companion.TURRET,
            StorageSlot.Companion.CENTER,
            StorageSlot.Companion.BOTTOM)

        @JvmField
        var PREFERRED_REQUEST_SLOT_SEARCHING_ORDER = arrayOf(
            StorageSlot.Companion.TURRET,
            StorageSlot.Companion.CENTER,
            StorageSlot.Companion.BOTTOM,
            StorageSlot.Companion.MOBILE)
    }


    @Config
    internal object CONTROLS
    {
        @JvmField
        var DRIVE_TO_SHOOTING_ZONE = false

        @JvmField
        var TRY_TERMINATE_INTAKE_WHEN_SHOOTING = true

        @JvmField
        var IGNORE_DUPLICATE_SHOOTING_COMMAND  = true

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
            var ALWAYS_TRY_IN_ADVANCE = true
            //  IN_ADVANCE = Before shooting

            @JvmField
            var FURTHER_TRY_IN_ACTION = true
            //  IN_ACTION -> Whilest mid Sorting DrumRequest


            @JvmField
            var START_WEIGHT = 0.0

            @JvmField
            var TRUE_MATCH_WEIGHT = 1.0

            @JvmField
            var PSEUDO_MATCH_WEIGHT = 0.75


            @JvmField
            var MIN_SEQUENCE_SCORE = 0.75
        }


        @JvmField
        var USE_CURRENT_PROTECTION_FOR_STORAGE_BELTS = false

        @JvmField
        var SMART_RECALIBRATE_STORAGE_WITH_CURRENT_PROTECTION = false

        @JvmField
        var TRY_RECALIBRATE_WITH_CURRENT_UNTIL_SUCCESS = false


        @JvmField
        var TRY_RECALIBRATE_IF_SOMETHING_FAILS = true
    }


    @Config
    internal object SHOOTING
    {
        @JvmField
        var USE_LAZY_VERSION_OF_STREAM_REQUEST = true

        @JvmField
        var DO_WAIT_BEFORE_NEXT_SHOT = false
    }


    @Config
    internal object AUTONOMOUS
    {
        @JvmField
        var DEFAULT_SHOOTING_MODE = Shooting.Mode.FIRE_PATTERN_CAN_SKIP
        @JvmField
        var DEFAULT_PATTERN = Shooting.StockPattern.Name.USE_DETECTED_PATTERN

        @JvmField
        var FAILSAFE_SHOOTING_MODE = Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
        @JvmField
        var FAILSAFE_PATTERN = Shooting.StockPattern.Name.ANY


        @JvmField
        var MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS: Long = 1000

        @JvmField
        var MAX_ATTEMPTS_FOR_PATTERN_DETECTION = 0
    }


    @Config
    internal object TELEOP
    {
        @JvmField
        var INCLUDE_PREVIOUS_UNFINISHED_TO_REQUEST_ORDER  = false
        @JvmField
        var INCLUDE_PREVIOUS_UNFINISHED_TO_FAILSAFE_ORDER = false
        @JvmField
        var AUTO_UPDATE_UNFINISHED_FOR_NEXT_PATTERN = false
        @JvmField
        var IF_AUTO_UPDATE_UNFINISHED_USE_FAILSAFE_ORDER = false


        @JvmField
        var PATTERN_SHOOTING_MODE = Shooting.Mode.FIRE_PATTERN_CAN_SKIP
    }
}