package org.woen.configs


import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager
import com.acmerobotics.dashboard.config.Config



@Config
object DebugSettings
{
    @JvmField
    var SMC = LogManager.Config(  //  Scoring modules connector
        moduleName = "SMC",
        warningLevels = arrayListOf(Debug.HARDWARE, Debug.HARDWARE_LOW, Debug.HARDWARE_HIGH),
        debugLevels = arrayListOf(
            Debug.GAMEPAD_FEEDBACK,
            Debug.EVENTS_FEEDBACK,

            Debug.ATTEMPTING_LOGIC,
            Debug.PROCESS_STARTING,
            Debug.PROCESS_ENDING,

            Debug.GENERIC_INFO,
            Debug.LOGIC_STEPS,
            Debug.PROCESS_NAME)
    )


    @JvmField
    var SSM = LogManager.Config(  //  Sorting storage module
        moduleName = "SSM",
        warningLevels = arrayListOf(Debug.HARDWARE, Debug.HARDWARE_LOW, Debug.HARDWARE_HIGH),
        debugLevels = arrayListOf(
            Debug.HARDWARE,
            Debug.HARDWARE_HIGH,

            Debug.GAMEPAD_FEEDBACK,
            Debug.EVENTS_FEEDBACK,

            Debug.ATTEMPTING_LOGIC,
            Debug.PROCESS_STARTING,
            Debug.PROCESS_ENDING,

            Debug.GENERIC_INFO,
            Debug.LOGIC_STEPS,
            Debug.PROCESS_NAME)
    )

    @JvmField
    var CELLS = LogManager.Config(  //  Storage cells
        moduleName = "CELLS",
        warningLevels = arrayListOf(Debug.HARDWARE, Debug.HARDWARE_LOW, Debug.HARDWARE_HIGH),
        debugLevels = arrayListOf(
            Debug.HARDWARE,
            Debug.HARDWARE_HIGH,

            Debug.GAMEPAD_FEEDBACK,
            Debug.EVENTS_FEEDBACK,

            Debug.ATTEMPTING_LOGIC,
            Debug.PROCESS_STARTING,
            Debug.PROCESS_ENDING,

            Debug.GENERIC_INFO,
            Debug.STATUS,
            Debug.LOGIC_STEPS,
            Debug.PROCESS_NAME)
    )

    @JvmField
    var HSM = LogManager.Config(  //  Hardware sorting manager
        moduleName = "HSM",
        warningLevels = arrayListOf(Debug.HARDWARE, Debug.HARDWARE_LOW, Debug.HARDWARE_HIGH),
        debugLevels = arrayListOf(
            Debug.HARDWARE,
            Debug.HARDWARE_HIGH,

            Debug.GAMEPAD_FEEDBACK,
            Debug.EVENTS_FEEDBACK,

            Debug.ATTEMPTING_LOGIC,
            Debug.PROCESS_STARTING,
            Debug.PROCESS_ENDING,

            Debug.GENERIC_INFO,
            Debug.LOGIC_STEPS,
            Debug.PROCESS_NAME)
    )
}