package org.woen.utils.debug

import com.acmerobotics.dashboard.config.Config



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

    const val STATUS = 11

    const val LOGIC = 12
    const val LOGIC_STEPS = 12


    const val PROCESS_NAME = 13
    const val TERMINATION  = 14

    const val ERROR = 15
    const val ERROR_INFO = 15
}