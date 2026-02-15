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

    const val STATUS = 11

    const val LOGIC = 12
    const val LOGIC_STEPS = 12


    const val PROCESS_NAME = 13
    const val TERMINATION  = 14



    @JvmField
    var SAL = LogManager.Config(  //  Sorting Auto Logic
        moduleName = "SAL",
        warningLevels = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH),
          debugLevels = arrayListOf(
            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            STATUS,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION)
    )

    @JvmField
    var SMC = LogManager.Config(  //  Scoring modules connector
        moduleName = "SMC",
        warningLevels = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH),
          debugLevels = arrayListOf(
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
    )



    @JvmField
    var SSL = LogManager.Config(  //  Sorting Storage Logic
        moduleName = "SSL",
        warningLevels = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH),
          debugLevels = arrayListOf(
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
    )

    @JvmField
    var SSM = LogManager.Config(  //  Sorting storage module
        moduleName = "SSM",
        warningLevels = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH),
          debugLevels = arrayListOf(
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
    )



    @JvmField
    var CELLS = LogManager.Config(  //  Storage cells
        moduleName = "CELLS",
        warningLevels = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH),
          debugLevels = arrayListOf(
            HARDWARE,
            HARDWARE_HIGH,

            GAMEPAD_FEEDBACK,
            EVENTS_FEEDBACK,

            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            STATUS,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION)
    )

    @JvmField
    var HSM = LogManager.Config(  //  Hardware sorting manager
        moduleName = "HSM",
        warningLevels = arrayListOf(HARDWARE, HARDWARE_LOW, HARDWARE_HIGH),
          debugLevels = arrayListOf(
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
    )
}