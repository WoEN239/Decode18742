package org.woen.configs


import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager



object DebugSettings
{
    var SMC = LogManager.Config(moduleName = "SMC")  //  Scoring Module Connector

    var SSM = LogManager.Config(moduleName = "SSM")  //  Sorting storage module


    var CELLS = LogManager.Config(moduleName = "CELLS")  //  Storage cells

    var HSM = LogManager.Config(moduleName = "HSM",  //  Hardware sorting manager
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
            Debug.PROCESS_NAME,
            Debug.ERROR)
    )
}