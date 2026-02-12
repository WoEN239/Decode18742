package org.woen.telemetry.configs

import com.acmerobotics.dashboard.config.Config
import org.woen.telemetry.LogManager



@Config
object Debug
{
    var DISABLE_ALL_LOGS = false
    var SHOW_DEBUG_SUPPRESS_WARNINGS = true



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
    var CELLS_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    var CELLS_DEBUG_LEVELS = arrayListOf(
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
        TERMINATION
    )


    //  Sorting Storage Logic
    var SSL_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    var SSL_DEBUG_LEVELS = arrayListOf(
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
        TERMINATION
    )


    //  Sorting storage module
    var SSM_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    var SSM_DEBUG_LEVELS = arrayListOf(
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
        TERMINATION
    )


    //  Sorting Auto Logic
    var SAL_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    var SAL_DEBUG_LEVELS = arrayListOf(
        ATTEMPTING_LOGIC,
        PROCESS_STARTING,
        PROCESS_ENDING,

        GENERIC_INFO,
        LOGIC_STEPS,

        PROCESS_NAME,
        TERMINATION
    )


    //  Scoring modules connector
    var SMC_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
    var SMC_DEBUG_LEVELS = arrayListOf(
        GAMEPAD_FEEDBACK,
        EVENTS_FEEDBACK,
        RACE_CONDITION,

        ATTEMPTING_LOGIC,
        PROCESS_STARTING,
        PROCESS_ENDING,

        GENERIC_INFO,
        LOGIC_STEPS,

        PROCESS_NAME,
        TERMINATION
    )


    //  Hardware sorting manager
    var HSM_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
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
        TERMINATION
    )
}