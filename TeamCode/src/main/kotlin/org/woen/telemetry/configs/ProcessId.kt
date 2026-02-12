package org.woen.telemetry.configs

import com.acmerobotics.dashboard.config.Config
import org.woen.utils.process.RunStatus



@Config
object ProcessId
{
    @JvmField
    var PRIORITY_SETTING_FOR_SSM = RunStatus.Priority.PRIORITIZE_HIGH_PROCESS_ID
    @JvmField
    var PRIORITY_SETTING_FOR_SMC = RunStatus.Priority.PRIORITIZE_HIGH_PROCESS_ID


    @JvmField
    var IDLE = 0
    @JvmField
    var UNDEFINED_PROCESS_ID = 0


    @JvmField
    var INTAKE = 1
    @JvmField
    var LAZY_INTAKE = 2

    @JvmField
    var RUNNING_INTAKE_INSTANCE = 3


    @JvmField
    var DRUM_REQUEST = 4
    @JvmField
    var SINGLE_REQUEST = 5


    @JvmField
    var PREDICT_SORT = 6
    @JvmField
    var STORAGE_CALIBRATION = 7

    @JvmField
    var UPDATE_AFTER_LAZY_INTAKE = 8

    @JvmField
    var SORTING_TESTING = 9
}