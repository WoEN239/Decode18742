package org.woen.telemetry


import java.util.concurrent.atomic.AtomicIntegerArray

import org.woen.utils.atomic.isEmpty
import org.woen.utils.atomic.contains

import org.woen.telemetry.configs.Debug



class LogManager
{
    private var _debugLevels   = AtomicIntegerArray(0)
    private var _warningLevels = AtomicIntegerArray(0)

    private var _debugShowSetting   = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE
    private var _warningShowSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE

    private var _moduleName = ""



    constructor(debugSetting:   DebugSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE,
                warningSetting: DebugSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE,
                debugLevels:   ArrayList<Int> = arrayListOf(0),
                warningLevels: ArrayList<Int> = arrayListOf(0),
                moduleName: String = "")
    { reset(debugSetting, warningSetting, debugLevels, warningLevels, moduleName) }
    fun reset(debugSetting:   DebugSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE,
              warningSetting: DebugSetting = DebugSetting.SHOW_ABOVE_SELECTED_INCLUSIVE,
              debugLevels:   ArrayList<Int> = arrayListOf(0),
              warningLevels: ArrayList<Int> = arrayListOf(0),
              moduleName: String = "")
    {
        updateDebugSetting  (debugSetting)
        updateWarningSetting(warningSetting)

        setShowedDebugLevels  (debugLevels  .toIntArray())
        setShowedWarningLevels(warningLevels.toIntArray())

        setModuleName(moduleName)
    }



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



    fun updateDebugSetting  (debugSetting:   DebugSetting)
    {
        _debugShowSetting   = debugSetting
    }
    fun updateWarningSetting(warningSetting: DebugSetting)
    {
        _warningShowSetting = warningSetting
    }

    fun setShowedDebugLevels  (levels: IntArray)
    {
        _debugLevels   = AtomicIntegerArray(levels)
    }
    fun setShowedWarningLevels(levels: IntArray)
    {
        _warningLevels = AtomicIntegerArray(levels)
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
            ThreadedTelemetry.LAZY_INSTANCE.log(
                if (Debug.SHOW_DEBUG_LEVEL)
                    withDebugLevel(firstMatch) + s else s)
        }
        else if (Debug.SHOW_DEBUG_SUPPRESS_WARNINGS)
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
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
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
                if (Debug.SHOW_DEBUG_LEVEL)
                    withDebugLevel(firstMatch) + s else s, tag)
        }
        else if (Debug.SHOW_DEBUG_SUPPRESS_WARNINGS)
            ThreadedTelemetry.LAZY_INSTANCE.logWithTag(
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
        '0') + "]"
    private fun toMdString(s: String)
        = if (_moduleName.isEmpty()) s
          else "$_moduleName: $s"
    private fun allowedToShow(debugLevel: Int, show: AtomicIntegerArray): Boolean
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



    private fun customSelected(debugLevel: Int, show: AtomicIntegerArray): Boolean
        = show.contains(debugLevel)


    private fun aboveInclusive(debugLevel: Int, show: AtomicIntegerArray): Boolean
        = if (show.isEmpty()) false
          else debugLevel >= show[0]
    private fun aboveExclusive(debugLevel: Int, show: AtomicIntegerArray): Boolean
        = if (show.isEmpty()) false
          else debugLevel >  show[0]


    private fun belowInclusive(debugLevel: Int, show: AtomicIntegerArray): Boolean
        = if (show.isEmpty()) false
          else debugLevel <= show[0]
    private fun belowExclusive(debugLevel: Int, show: AtomicIntegerArray): Boolean
        = if (show.isEmpty()) false
          else debugLevel <  show[0]
}