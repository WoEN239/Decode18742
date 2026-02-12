package org.woen.telemetry


import org.woen.telemetry.configs.Debug.DISABLE_ALL_LOGS
import org.woen.telemetry.configs.Debug.SHOW_DEBUG_SUPPRESS_WARNINGS



class LogManager
{
    private var _showLevels = arrayListOf(0)
    private var _debugShowSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE
    private var _moduleName = ""


    constructor(showSetting: DebugSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE,
        showLevels: ArrayList<Int> = arrayListOf(0),
        moduleName: String = "")
    {
        updateDebugSetting(showSetting)
        setShowedDebugLevels(showLevels)
        setModuleName(moduleName)
    }


    enum class DebugSetting
    {
        HIDE,
        SHOW_EVERYTHING,

        SHOW_SELECTED_LEVELS,

        SHOW_ABOVE_SELECTED_INCLUSIVE,
        SHOW_ABOVE_SELECTED_EXCLUSIVE,

        SHOW_BELOW_SELECTED_INCLUSIVE,
        SHOW_BELOW_SELECTED_EXCLUSIVE
    }



    fun updateDebugSetting(showSetting: DebugSetting)
    {
        _debugShowSetting = showSetting
    }
    fun setShowedDebugLevels(showLevels: ArrayList<Int>)
    {
        _showLevels = ArrayList(showLevels)
    }
    fun setModuleName(moduleName: String)
    {
        _moduleName = moduleName
    }



    fun log   (s: String, vararg debug: Int)
    {
        if (debug.any { allowedToShow(it) })
            ThreadedTelemetry.LAZY_INSTANCE.log(s)
        else if (SHOW_DEBUG_SUPPRESS_WARNINGS)
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
                "Debug level $debug is turned off", "Warning")
    }
    fun logMd (s: String, vararg debug: Int)
    {
        if (debug.any { allowedToShow(it) })
            ThreadedTelemetry.LAZY_INSTANCE.log(toMdString(s))
        else if (SHOW_DEBUG_SUPPRESS_WARNINGS)
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
                toMdString("Debug level $debug is turned off"), "Warning")
    }
    fun logTag(s: String, tag: String, vararg debug: Int)
    {
        if (debug.any { allowedToShow(it) })
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(s, tag)
        else if (SHOW_DEBUG_SUPPRESS_WARNINGS)
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
                "Debug level $debug is turned off", "Warning")
    }



    private fun toMdString(s: String)
        = if (_moduleName.isEmpty()) s
          else "$_moduleName: $s"
    private fun allowedToShow(debugLevel: Int): Boolean
    {
        return !DISABLE_ALL_LOGS && when (_debugShowSetting)
        {
            DebugSetting.HIDE                 -> false
            DebugSetting.SHOW_EVERYTHING      -> true
            DebugSetting.SHOW_SELECTED_LEVELS -> customSelected(debugLevel)

            DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE -> aboveInclusive(debugLevel)
            DebugSetting.SHOW_ABOVE_SELECTED_EXCLUSIVE -> aboveExclusive(debugLevel)

            DebugSetting.SHOW_BELOW_SELECTED_INCLUSIVE -> belowInclusive(debugLevel)
            DebugSetting.SHOW_BELOW_SELECTED_EXCLUSIVE -> belowExclusive(debugLevel)
        }
    }



    private fun customSelected(debugLevel: Int): Boolean
            = _showLevels.contains(debugLevel)


    private fun aboveInclusive(debugLevel: Int): Boolean
        = if (_showLevels.isEmpty()) false
          else debugLevel >= _showLevels[0]
    private fun aboveExclusive(debugLevel: Int): Boolean
        = if (_showLevels.isEmpty()) false
          else debugLevel >  _showLevels[0]


    private fun belowInclusive(debugLevel: Int): Boolean
        = if (_showLevels.isEmpty()) false
          else debugLevel <= _showLevels[0]
    private fun belowExclusive(debugLevel: Int): Boolean
        = if (_showLevels.isEmpty()) false
          else debugLevel <  _showLevels[0]
}