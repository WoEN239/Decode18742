package org.woen.telemetry.configs

import com.acmerobotics.dashboard.config.Config
import org.woen.telemetry.EventConfig
import org.woen.telemetry.LogManager
import org.woen.telemetry.ThreadedTelemetry



@Config
object Debug
{
    @JvmField
    var SHOW_DEBUG_LEVEL = true
    @JvmField
    var FILL_DEBUG_LEVEL_TO_DIGIT_COUNT = 2


    @JvmField
    var DISABLE_ALL_LOGS = false
    @JvmField
    var SHOW_DEBUG_SUPPRESS_WARNINGS = true


    @JvmField
    var TELEMETRY_ENABLE = true
    @EventConfig
    var TELEMETRY_UPDATE_HZ = ThreadedTelemetry.EventValueProvider(5)



    const val HW_LOW = 1
    const val HARDWARE_LOW = 1

    const val HW = 2
    const val HARDWARE = 2

    const val HW_HIGH  = 3
    const val HARDWARE_HIGH = 3


    const val GAMEPAD = 4
    const val GAMEPAD_FEEDBACK = 4

    const val EVENTS = 5
    const val EVENTS_FEEDBACK = 5

    const val RACE_COND = 6
    const val RACE_CONDITION = 6

    const val TRYING = 7
    const val ATTEMPTING_LOGIC = 7


    const val START = 8
    const val PROCESS_STARTING = 8
    const val END = 9
    const val PROCESS_ENDING = 9

    const val GENERIC = 10
    const val GENERIC_INFO = 10

    const val LOGIC = 11
    const val LOGIC_STEPS = 11

    const val PROCESS_NAME = 12
    const val TERMINATION  = 13





    //  Sorting cells
    @JvmField
    var CELLS_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    @JvmField
    var CELLS_DEBUG_LEVELS  = arrayListOf(
        HARDWARE,
        HARDWARE_HIGH,

        GAMEPAD_FEEDBACK,
        EVENTS_FEEDBACK,

        ATTEMPTING_LOGIC,
        PROCESS_STARTING,
        PROCESS_ENDING,

        GENERIC_INFO,
        LOGIC_STEPS,

        PROCESS_NAME,
        TERMINATION)
    @JvmField
    var CELLS_WARNING_SETTING = LogManager.DebugSetting.SHOW_EXCEPT_SELECTED_LEVELS
    @JvmField
    var CELLS_WARNING_LEVELS  = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH)



    //  Sorting Storage Logic
    @JvmField
    var SSL_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    @JvmField
    var SSL_DEBUG_LEVELS  = arrayListOf(
        HARDWARE,
        HARDWARE_HIGH,

        GAMEPAD_FEEDBACK,
        EVENTS_FEEDBACK,
        RACE_CONDITION,

        ATTEMPTING_LOGIC,
        PROCESS_STARTING,
        PROCESS_ENDING,

        GENERIC_INFO,
        LOGIC_STEPS,

        PROCESS_NAME,
        TERMINATION)
    @JvmField
    var SSL_WARNING_SETTING = LogManager.DebugSetting.SHOW_EXCEPT_SELECTED_LEVELS
    @JvmField
    var SSL_WARNING_LEVELS  = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH)



    //  Sorting storage module
    @JvmField
    var SSM_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    @JvmField
    var SSM_DEBUG_LEVELS  = arrayListOf(
        HARDWARE,
        HARDWARE_HIGH,

        GAMEPAD_FEEDBACK,
        EVENTS_FEEDBACK,
        RACE_CONDITION,

        ATTEMPTING_LOGIC,
        PROCESS_STARTING,
        PROCESS_ENDING,

        GENERIC_INFO,
        LOGIC_STEPS,

        PROCESS_NAME,
        TERMINATION)
    @JvmField
    var SSM_WARNING_SETTING = LogManager.DebugSetting.SHOW_EXCEPT_SELECTED_LEVELS
    @JvmField
    var SSM_WARNING_LEVELS  = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH)



    //  Sorting Auto Logic
    @JvmField
    var SAL_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    @JvmField
    var SAL_DEBUG_LEVELS  = arrayListOf(
        ATTEMPTING_LOGIC,
        PROCESS_STARTING,
        PROCESS_ENDING,

        GENERIC_INFO,
        LOGIC_STEPS,

        PROCESS_NAME,
        TERMINATION)
    @JvmField
    var SAL_WARNING_SETTING = LogManager.DebugSetting.SHOW_EXCEPT_SELECTED_LEVELS
    @JvmField
    var SAL_WARNING_LEVELS  = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH)



    //  Scoring modules connector
    @JvmField
    var SMC_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    @JvmField
    var SMC_DEBUG_LEVELS  = arrayListOf(
        GAMEPAD_FEEDBACK,
        EVENTS_FEEDBACK,
        RACE_CONDITION,

        ATTEMPTING_LOGIC,
        PROCESS_STARTING,
        PROCESS_ENDING,

        GENERIC_INFO,
        LOGIC_STEPS,

        PROCESS_NAME,
        TERMINATION)
    @JvmField
    var SMC_WARNING_SETTING = LogManager.DebugSetting.SHOW_EXCEPT_SELECTED_LEVELS
    @JvmField
    var SMC_WARNING_LEVELS  = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH)



    //  Hardware sorting manager
    @JvmField
    var HSM_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    @JvmField
    var HSM_DEBUG_LEVELS = arrayListOf(
        HARDWARE,
        HARDWARE_HIGH,

        GAMEPAD_FEEDBACK,
        EVENTS_FEEDBACK,

        ATTEMPTING_LOGIC,
        PROCESS_STARTING,
        PROCESS_ENDING,

        GENERIC_INFO,
        LOGIC_STEPS,

        PROCESS_NAME,
        TERMINATION)
    @JvmField
    var HSM_WARNING_SETTING = LogManager.DebugSetting.SHOW_EXCEPT_SELECTED_LEVELS
    @JvmField
    var HSM_WARNING_LEVELS  = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH)
}