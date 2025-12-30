package org.woen.telemetry


import org.woen.telemetry.Configs.DEBUG_LEVELS.SHOW_DEBUG_SUPPRESS_WARNINGS



class LogManager
{
    private var _showLevels = arrayListOf(0u)
    private var _debugShowSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE
    private var _moduleName = ""


    constructor(showSetting: DebugSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE,
        showLevels: ArrayList<UInt> = arrayListOf(0u),
        moduleName: String = "")
    {
        updateDebugSetting(showSetting)
        setShowedDebugLevels(showLevels)
        setModuleName(moduleName)
    }


    enum class DebugSetting
    {
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
    fun setShowedDebugLevels(showLevels: ArrayList<UInt>)
    {
        _showLevels = ArrayList(showLevels)
    }
    fun setModuleName(moduleName: String)
    {
        _moduleName = moduleName
    }



    fun log   (s: String, debugLevel: UInt)
    {
        if (allowedToShow(debugLevel))
            ThreadedTelemetry.LAZY_INSTANCE.log(s)
        else if (SHOW_DEBUG_SUPPRESS_WARNINGS)
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
                "Debug level $debugLevel is turned off", "Warning")
    }
    fun logMd (s: String, debugLevel: UInt)
    {
        if (allowedToShow(debugLevel))
            ThreadedTelemetry.LAZY_INSTANCE.log(toMdString(s))
        else if (SHOW_DEBUG_SUPPRESS_WARNINGS)
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
                toMdString("Debug level $debugLevel is turned off"), "Warning")
    }
    fun logTag(s: String, tag: String, debugLevel: UInt)
    {
        if (allowedToShow(debugLevel))
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(s, tag)
        else if (SHOW_DEBUG_SUPPRESS_WARNINGS)
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
                "Debug level $debugLevel is turned off", "Warning")
    }



    private fun toMdString(s: String)
        = if (_moduleName.isEmpty()) s
          else "$_moduleName: $s"
    private fun allowedToShow(debugLevel: UInt): Boolean
    {
        return when (_debugShowSetting)
        {
            DebugSetting.SHOW_EVERYTHING      -> true
            DebugSetting.SHOW_SELECTED_LEVELS -> customSelected(debugLevel)

            DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE -> aboveInclusive(debugLevel)
            DebugSetting.SHOW_ABOVE_SELECTED_EXCLUSIVE -> aboveExclusive(debugLevel)

            DebugSetting.SHOW_BELOW_SELECTED_INCLUSIVE -> belowInclusive(debugLevel)
            DebugSetting.SHOW_BELOW_SELECTED_EXCLUSIVE -> belowExclusive(debugLevel)
        }
    }



    private fun customSelected(debugLevel: UInt): Boolean
            = _showLevels.contains(debugLevel)


    private fun aboveInclusive(debugLevel: UInt): Boolean
        = if (_showLevels.isEmpty()) false
          else debugLevel >= _showLevels[0]
    private fun aboveExclusive(debugLevel: UInt): Boolean
        = if (_showLevels.isEmpty()) false
          else debugLevel >  _showLevels[0]


    private fun belowInclusive(debugLevel: UInt): Boolean
        = if (_showLevels.isEmpty()) false
          else debugLevel <= _showLevels[0]
    private fun belowExclusive(debugLevel: UInt): Boolean
        = if (_showLevels.isEmpty()) false
          else debugLevel <  _showLevels[0]
}