package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest
import org.woen.enumerators.RequestResult
import org.woen.enumerators.Shooting

import org.woen.hotRun.HotRun
import org.woen.telemetry.LogManager

import org.woen.modules.scoringSystem.storage.sorting.DynamicPattern

import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT

import org.woen.modules.camera.OnPatternDetectedEvent
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent

import org.woen.telemetry.configs.Debug
import org.woen.telemetry.configs.Configs.DELAY
import org.woen.telemetry.configs.RobotSettings.AUTONOMOUS



class DefaultFireEvent()



class SortingAutoLogic
{
    private val _patternWasDetected = AtomicBoolean(false)
    private val _patternDetectionAttempts = AtomicInteger(0)
    private val _pattern = DynamicPattern()
    val logM = LogManager(
         Debug.SAL_DEBUG_SETTING,
        Debug.SAL_WARNING_SETTING,
         Debug.SAL_DEBUG_LEVELS,
        Debug.SAL_WARNING_LEVELS,
        "SAL")



    constructor()
    {
        subscribeToPatternDetectionEvents()
        subscribeToDefaultFiringEvent()

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            logM.logMd("Resetting parameters on initialisation", Debug.EVENTS)
            resetParametersToDefault()
        }
    }


    private fun subscribeToPatternDetectionEvents()
    {
        EventBusLI.subscribe(OnPatternDetectedEvent::class, {

                logM.logMd("Pattern successfully detected!", Debug.EVENTS)

                _pattern.setPermanent(it.pattern.subsequence)
                _patternWasDetected.set(true)
        }   )
    }
    private fun subscribeToDefaultFiringEvent()
    {
        EventBusLI.subscribe(DefaultFireEvent::class, {

                logM.logMd("Received fire command, starting DefaultFire!", Debug.EVENTS)
                firePattern()
        }   )
    }
    private fun resetParametersToDefault()
    {
        _patternWasDetected.set(false)
        _patternDetectionAttempts.set(0)

        _pattern.fullReset()

        logM.reset(
             Debug.SAL_DEBUG_SETTING,
            Debug.SAL_WARNING_SETTING,
             Debug.SAL_DEBUG_LEVELS,
            Debug.SAL_WARNING_LEVELS,
            "SAL")
    }



    private fun canTryDetectPattern(): Boolean
        = _patternDetectionAttempts.get() <
            AUTONOMOUS.MAX_ATTEMPTS_FOR_PATTERN_DETECTION
    private suspend fun tryGetPattern(): Boolean
    {
        logM.logMd("Waiting for detected pattern", Debug.LOGIC)

        val waitDuration = ElapsedTime()
        _patternDetectionAttempts.getAndAdd(1)

        while (waitDuration.milliseconds() <
            AUTONOMOUS.MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS)
        {
            if (_patternWasDetected.get())
            {
                logM.logMd("Pattern detected successfully", Debug.TRYING)
                logPatternInfo(_pattern.permanent())

                return true
            }
            delay(DELAY.EVENT_AWAITING_MS)
        }

        logM.logMd("Failed to get pattern", Debug.TRYING)
        return false
    }



    private suspend fun firePattern()
    {
        logM.logMd("Choosing default", Debug.PROCESS_NAME)

        if (AUTONOMOUS.DEFAULT_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (AUTONOMOUS.DEFAULT_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                logM.logMd("Default trying: fire detected pattern", Debug.TRYING)

                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                {
                    logM.logMd("Default decided: Fire detected pattern", Debug.PROCESS_NAME)
                    logPatternInfo(_pattern.permanent())

                    EventBusLI.invoke(StorageGiveDrumRequest(
                        AUTONOMOUS.DEFAULT_SHOOTING_MODE,
                            _pattern.permanent()
                    )   )
                }
                else fireFailsafe()
            }

            Shooting.StockPattern.Name.ANY_TWO_IDENTICAL_COLORS,
            Shooting.StockPattern.Name.ANY_THREE_IDENTICAL_COLORS ->
            {
                logM.logMd("Default trying: fire identical colors", Debug.TRYING)

                val ballCount = Shooting.StockPattern.requestedBallCount(
                    AUTONOMOUS.DEFAULT_PATTERN)
                val storageBalls = EventBusLI.invoke(
                    StorageHandleIdenticalColorsEvent())

                logM.logMd("Got identical count: " +
                        "${storageBalls.maxIdenticalColorCount}, " +
                        "identical color: ${storageBalls.identicalColor}",
                        Debug.GENERIC)

                if (storageBalls.maxIdenticalColorCount >= ballCount)
                {
                    logM.logMd("Default decided: Fire identical colors: " +
                            "${storageBalls.maxIdenticalColorCount}", Debug.PROCESS_NAME)

                    val convertedPattern =  Array(storageBalls.maxIdenticalColorCount)
                        { Ball.toBallRequestName(storageBalls.identicalColor) }

                    logPatternInfo(convertedPattern)

                    EventBusLI.invoke(StorageGiveDrumRequest(
                        AUTONOMOUS.DEFAULT_SHOOTING_MODE,
                            convertedPattern))
                }
                else fireFailsafe()
            }

            else ->
            {
                logM.logMd("Default decided: Fire custom pattern", Debug.PROCESS_NAME)

                val convertedPattern = Shooting.StockPattern.
                    tryConvertToPatternSequence(AUTONOMOUS.DEFAULT_PATTERN)

                logPatternInfo(convertedPattern)

                EventBusLI.invoke(StorageGiveDrumRequest(
                    AUTONOMOUS.DEFAULT_SHOOTING_MODE,
                        convertedPattern
                )   )
            }
        }
    }
    private suspend fun fireFailsafe()
    {
        logM.logMd("Choosing failsafe", Debug.PROCESS_NAME)

        if (AUTONOMOUS.FAILSAFE_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (AUTONOMOUS.FAILSAFE_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                logM.logMd("Failsafe trying: fire detected pattern", Debug.TRYING)

                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                {
                    logM.logMd("Failsafe decided: Fire detected pattern", Debug.PROCESS_NAME)
                    logPatternInfo(_pattern.permanent())

                    EventBusLI.invoke(StorageGiveDrumRequest(
                            AUTONOMOUS.FAILSAFE_SHOOTING_MODE,
                            _pattern.permanent()
                    )   )
                }
                else sendFinishedFiringEvent(Request.COULD_NOT_DETECT_PATTERN)
            }

            Shooting.StockPattern.Name.ANY_TWO_IDENTICAL_COLORS,
            Shooting.StockPattern.Name.ANY_THREE_IDENTICAL_COLORS ->
            {
                logM.logMd("Failsafe trying: fire identical colors", Debug.TRYING)

                val ballCount = Shooting.StockPattern.requestedBallCount(
                    AUTONOMOUS.DEFAULT_PATTERN)
                val storageBalls = EventBusLI.invoke(
                    StorageHandleIdenticalColorsEvent())

                logM.logMd("Got identical count: " +
                        "${storageBalls.maxIdenticalColorCount}, " +
                        "identical color: ${storageBalls.identicalColor}",
                        Debug.GENERIC)

                if (storageBalls.maxIdenticalColorCount >= ballCount)
                {
                    logM.logMd("Failsafe decided: Fire identical colors: " +
                            "${storageBalls.maxIdenticalColorCount}", Debug.GENERIC)

                    val convertedPattern =  Array(storageBalls.maxIdenticalColorCount)
                    { Ball.toBallRequestName(storageBalls.identicalColor) }

                    logPatternInfo(convertedPattern)

                    EventBusLI.invoke(StorageGiveDrumRequest(
                        AUTONOMOUS.FAILSAFE_SHOOTING_MODE,
                            convertedPattern))
                }
                else sendFinishedFiringEvent(Request.NOT_ENOUGH_COLORS)
            }

            else ->
            {
                logM.logMd("Failsafe decided: Fire custom pattern", Debug.PROCESS_NAME)

                val convertedPattern = Shooting.StockPattern.
                    tryConvertToPatternSequence(AUTONOMOUS.FAILSAFE_PATTERN)

                logPatternInfo(convertedPattern)

                EventBusLI.invoke(StorageGiveDrumRequest(
                    AUTONOMOUS.DEFAULT_SHOOTING_MODE,
                        convertedPattern
                )   )
            }
        }
    }
    private fun fireEverything()
    {
        logM.logMd("Jump! Decided: Fire everything", Debug.PROCESS_NAME)

        EventBusLI.invoke(StorageGiveDrumRequest(
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE,
                Array(MAX_BALL_COUNT)
                {
                    BallRequest.Name.ANY_CLOSEST
                }
        )   )
    }



    private fun sendFinishedFiringEvent(requestResult: RequestResult.Name)
    {
        logM.logMd("Shooting stopped, fail reason: $requestResult", Debug.END)

        EventBusLI.invoke(FullFinishedFiringEvent(requestResult))
    }

    private fun logPatternInfo(pattern: Array<BallRequest.Name>)
    {
        var patternString = "Pattern = "
        for (nextColorInPattern in pattern)
            patternString += "$nextColorInPattern, "

        logM.logMd(patternString, Debug.GENERIC)
    }
}