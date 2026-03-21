package org.woen.utils.debug


import org.woen.modules.Telemetry



class LogManager
{
    class Config(
        var debugSetting:    DebugSetting = DebugSetting.SHOW_SELECTED_LEVELS,
        var warningSetting:  DebugSetting = DebugSetting.SHOW_EXCEPT_SELECTED_LEVELS,
        var debugLevels:   ArrayList<Int> = arrayListOf(
            Debug.HW, Debug.HW_HIGH, Debug.GAMEPAD, Debug.EVENTS,
            Debug.TRYING, Debug.START, Debug.END, Debug.GENERIC,
            Debug.STATUS, Debug.LOGIC, Debug.PROCESS_NAME, Debug.ERROR),
        var warningLevels: ArrayList<Int> = arrayListOf(
            Debug.HW_LOW, Debug.HW, Debug.HW_HIGH),
        var moduleName: String = "")


    enum class DebugSetting
    {
        HIDE,
        SHOW_EVERYTHING,

        SHOW_SELECTED_LEVELS,
        SHOW_EXCEPT_SELECTED_LEVELS,

        SHOW_ABOVE_SELECTED_INCLUSIVE,
        SHOW_ABOVE_SELECTED_EXCLUSIVE,

        SHOW_BELOW_SELECTED_INCLUSIVE,
        SHOW_BELOW_SELECTED_EXCLUSIVE
    }



    private var _debugLevels:   Array<Int> = arrayOf()
    private var _warningLevels: Array<Int> = arrayOf()

    private var _debugShowSetting   = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE
    private var _warningShowSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE

    private var _moduleName = ""
    private var _telemetryM: Telemetry



    constructor(telemetry: Telemetry, config: Config)
    {
        _telemetryM = telemetry
        relink(config)
    }
    fun relink(config: Config, telemetry: Telemetry? = null)
    {
        updateDebugSetting  (config.debugSetting)
        updateWarningSetting(config.warningSetting)

        setShowedDebugLevels  (config.debugLevels.toTypedArray())
        setShowedWarningLevels(config.warningLevels.toTypedArray())

        setModuleName(config.moduleName)

        if (telemetry != null) _telemetryM = telemetry
    }



    fun updateDebugSetting  (debugSetting:   DebugSetting)
    {
        _debugShowSetting   = debugSetting
    }
    fun updateWarningSetting(warningSetting: DebugSetting)
    {
        _warningShowSetting = warningSetting
    }

    fun setShowedDebugLevels  (levels: Array<Int>)
    {
        _debugLevels = levels.copyOf()
    }
    fun setShowedWarningLevels(levels: Array<Int>)
    {
        _warningLevels = levels.copyOf()
    }

    fun setModuleName(moduleName: String)
    {
        _moduleName = moduleName
    }



    fun log   (s: String, vararg debug: Int)
    {
        val firstMatch = debug.firstOrNull { allowedToShow(it, _debugLevels) }

        if (firstMatch != null)
        {
            _telemetryM.log(
                if (Debug.SHOW_DEBUG_LEVEL)
                    withDebugLevel(firstMatch) + s else s)
        }
        else if (Debug.SHOW_DEBUG_SUPPRESS_WARNINGS)
            _telemetryM.logWithTag(
                "Debug levels [${debug.filter {
                    allowedToShow(it, _warningLevels)
                }.joinToString(", ")}]" +
                    " are turned off", "Warning")
    }
    fun logMd (s: String, vararg debug: Int)
        = log(toMdString(s), *debug)
    fun logTag(s: String, tag: String, vararg debug: Int)
    {
        val firstMatch = debug.firstOrNull { allowedToShow(it, _debugLevels) }

        if (firstMatch != null)
        {
            _telemetryM.logWithTag(
                if (Debug.SHOW_DEBUG_LEVEL)
                    withDebugLevel(firstMatch) + s else s, tag)
        }
        else if (Debug.SHOW_DEBUG_SUPPRESS_WARNINGS)
            _telemetryM.logWithTag(
                "Debug levels [${debug.filter {
                    allowedToShow(it, _warningLevels)
                }.joinToString(", ")}]" +
                        " are turned off", "Warning")
    }
    fun logMdTag(s: String, tag: String, vararg debug: Int)
        = logTag(toMdString(s), tag, *debug)



    private fun withDebugLevel(debug: Int)
        = '[' + debug.toString().padStart(
        Debug.FILL_DEBUG_LEVEL_TO_DIGIT_COUNT,
        '0') + "] "
    private fun toMdString(s: String)
        = if    (_moduleName.isEmpty()) s
          else "$_moduleName: $s"
    private fun allowedToShow(debugLevel: Int, show: Array<Int>): Boolean
    {
        return !Debug.DISABLE_ALL_LOGS && when (_debugShowSetting)
        {
            DebugSetting.HIDE            -> false
            DebugSetting.SHOW_EVERYTHING -> true

            DebugSetting.SHOW_SELECTED_LEVELS        ->   customSelected(debugLevel, show)
            DebugSetting.SHOW_EXCEPT_SELECTED_LEVELS ->  !customSelected(debugLevel, show)

            DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE -> aboveInclusive(debugLevel, show)
            DebugSetting.SHOW_ABOVE_SELECTED_EXCLUSIVE -> aboveExclusive(debugLevel, show)

            DebugSetting.SHOW_BELOW_SELECTED_INCLUSIVE -> belowInclusive(debugLevel, show)
            DebugSetting.SHOW_BELOW_SELECTED_EXCLUSIVE -> belowExclusive(debugLevel, show)
        }
    }



    private fun customSelected(debugLevel: Int, show: Array<Int>): Boolean
        = show.contains(debugLevel)

    private fun aboveInclusive(debugLevel: Int, show: Array<Int>): Boolean
        = if (show.isEmpty()) false
          else debugLevel >= show[0]
    private fun aboveExclusive(debugLevel: Int, show: Array<Int>): Boolean
        = if (show.isEmpty()) false
          else debugLevel >  show[0]

    private fun belowInclusive(debugLevel: Int, show: Array<Int>): Boolean
        = if (show.isEmpty()) false
          else debugLevel <= show[0]
    private fun belowExclusive(debugLevel: Int, show: Array<Int>): Boolean
        = if (show.isEmpty()) false
          else debugLevel <  show[0]
}