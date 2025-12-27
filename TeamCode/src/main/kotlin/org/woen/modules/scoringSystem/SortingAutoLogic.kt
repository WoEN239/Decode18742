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
import org.woen.threading.ThreadedEventBus

import org.woen.modules.scoringSystem.storage.sorting.DynamicPattern

import org.woen.modules.camera.OnPatternDetectedEvent
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent

import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT
import org.woen.telemetry.Configs.DEBUG_LEVELS.EVENTS_FEEDBACK
import org.woen.telemetry.Configs.DEBUG_LEVELS.ATTEMPTING_LOGIC
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_ENDING
import org.woen.telemetry.Configs.DEBUG_LEVELS.GENERIC_INFO
import org.woen.telemetry.Configs.DEBUG_LEVELS.LOGIC_STEPS
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_NAME

import org.woen.telemetry.Configs.DELAY

import org.woen.telemetry.Configs.DEBUG_LEVELS.SAL_DEBUG_LEVELS
import org.woen.telemetry.Configs.DEBUG_LEVELS.SAL_DEBUG_SETTING

import org.woen.telemetry.Configs.SORTING_SETTINGS.DEFAULT_PATTERN
import org.woen.telemetry.Configs.SORTING_SETTINGS.DEFAULT_SHOOTING_MODE

import org.woen.telemetry.Configs.SORTING_SETTINGS.FAILSAFE_PATTERN
import org.woen.telemetry.Configs.SORTING_SETTINGS.FAILSAFE_SHOOTING_MODE

import org.woen.telemetry.Configs.SORTING_SETTINGS.MAX_ATTEMPTS_FOR_PATTERN_DETECTION
import org.woen.telemetry.Configs.SORTING_SETTINGS.MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS



class DefaultFireEvent()



class SortingAutoLogic
{
    private val _patternWasDetected = AtomicBoolean(false)
    private val _patternDetectionAttempts = AtomicInteger(0)
    private val _pattern = DynamicPattern()
    val logM = LogManager(SAL_DEBUG_SETTING,
        SAL_DEBUG_LEVELS, "SAL")



    constructor()
    {
        subscribeToPatternDetectionEvents()
        subscribeToDefaultFiringEvent()

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            logM.logMd("Resetting parameters on initialisation", EVENTS_FEEDBACK)
            resetParametersToDefault()
        }
    }


    private fun subscribeToPatternDetectionEvents()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            OnPatternDetectedEvent::class, {

                logM.logMd("Pattern successfully detected!", EVENTS_FEEDBACK)

                _pattern.setPermanent(it.pattern.subsequence)
                _patternWasDetected.set(true)
        }   )
    }
    private fun subscribeToDefaultFiringEvent()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            DefaultFireEvent::class, {

                logM.logMd("Received fire command, starting DefaultFire!", EVENTS_FEEDBACK)
                firePattern()
        }   )
    }
    private fun resetParametersToDefault()
    {
        _patternWasDetected.set(false)
        _patternDetectionAttempts.set(0)

        _pattern.fullReset()
    }



    private fun canTryDetectPattern(): Boolean
        = _patternDetectionAttempts.get() < MAX_ATTEMPTS_FOR_PATTERN_DETECTION
    private suspend fun tryGetPattern(): Boolean
    {
        logM.logMd("Waiting for detected pattern", LOGIC_STEPS)

        val waitDuration = ElapsedTime()
        _patternDetectionAttempts.getAndAdd(1)

        while(waitDuration.milliseconds() < MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS)
        {
            if (_patternWasDetected.get())
            {
                logM.logMd("Pattern detected successfully", ATTEMPTING_LOGIC)
                logPatternInfo(_pattern.permanent())

                return true
            }
            delay(DELAY.EVENT_AWAITING_MS)
        }

        logM.logMd("Failed to get pattern", ATTEMPTING_LOGIC)
        return false
    }



    private suspend fun firePattern()
    {
        logM.logMd("Choosing default", PROCESS_NAME)

        if (DEFAULT_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (DEFAULT_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                logM.logMd("Default trying: fire detected pattern", ATTEMPTING_LOGIC)

                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                {
                    logM.logMd("Default decided: Fire detected pattern", PROCESS_NAME)
                    logPatternInfo(_pattern.permanent())

                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            _pattern.permanent()
                    )   )
                }
                else fireFailsafe()
            }

            Shooting.StockPattern.Name.ANY_TWO_IDENTICAL_COLORS,
            Shooting.StockPattern.Name.ANY_THREE_IDENTICAL_COLORS ->
            {
                logM.logMd("Default trying: fire identical colors", ATTEMPTING_LOGIC)

                val ballCount = Shooting.StockPattern.requestedBallCount(DEFAULT_PATTERN)
                val storageBalls = ThreadedEventBus.LAZY_INSTANCE.invoke(
                    StorageHandleIdenticalColorsEvent())

                logM.logMd("Got identical count: " +
                        "${storageBalls.maxIdenticalColorCount}, " +
                        "identical color: ${storageBalls.identicalColor}", GENERIC_INFO)

                if (storageBalls.maxIdenticalColorCount >= ballCount)
                {
                    logM.logMd("Default decided: Fire identical colors: " +
                            "${storageBalls.maxIdenticalColorCount}", PROCESS_NAME)

                    val convertedPattern =  Array(storageBalls.maxIdenticalColorCount)
                        { Ball.toBallRequestName(storageBalls.identicalColor) }

                    logPatternInfo(convertedPattern)

                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            convertedPattern
                        ))
                }
                else fireFailsafe()
            }

            else ->
            {
                logM.logMd("Default decided: Fire custom pattern", PROCESS_NAME)

                val convertedPattern = Shooting.StockPattern.
                    tryConvertToPatternSequence(DEFAULT_PATTERN)!!

                logPatternInfo(convertedPattern)

                ThreadedEventBus.LAZY_INSTANCE.invoke(
                    StorageGiveDrumRequest(
                        DEFAULT_SHOOTING_MODE,
                        convertedPattern
                )   )
            }
        }
    }
    private suspend fun fireFailsafe()
    {
        logM.logMd("Choosing failsafe", PROCESS_NAME)

        if (FAILSAFE_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (FAILSAFE_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                logM.logMd("Failsafe trying: fire detected pattern", ATTEMPTING_LOGIC)

                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                {
                    logM.logMd("Failsafe decided: Fire detected pattern", PROCESS_NAME)
                    logPatternInfo(_pattern.permanent())

                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            FAILSAFE_SHOOTING_MODE,
                            _pattern.permanent()
                    )   )
                }
                else sendFinishedFiringEvent(Request.COULD_NOT_DETECT_PATTERN)
            }

            Shooting.StockPattern.Name.ANY_TWO_IDENTICAL_COLORS,
            Shooting.StockPattern.Name.ANY_THREE_IDENTICAL_COLORS ->
            {
                logM.logMd("Failsafe trying: fire identical colors", ATTEMPTING_LOGIC)

                val ballCount = Shooting.StockPattern.requestedBallCount(DEFAULT_PATTERN)
                val storageBalls = ThreadedEventBus.LAZY_INSTANCE.invoke(
                    StorageHandleIdenticalColorsEvent())

                logM.logMd("Got identical count: " +
                        "${storageBalls.maxIdenticalColorCount}, " +
                        "identical color: ${storageBalls.identicalColor}", GENERIC_INFO)

                if (storageBalls.maxIdenticalColorCount >= ballCount)
                {
                    logM.logMd("Failsafe decided: Fire identical colors: " +
                            "${storageBalls.maxIdenticalColorCount}", GENERIC_INFO)

                    val convertedPattern =  Array(storageBalls.maxIdenticalColorCount)
                    { Ball.toBallRequestName(storageBalls.identicalColor) }

                    logPatternInfo(convertedPattern)

                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            FAILSAFE_SHOOTING_MODE,
                            convertedPattern
                    )   )
                }
                else sendFinishedFiringEvent(Request.NOT_ENOUGH_COLORS)
            }

            else ->
            {
                logM.logMd("Failsafe decided: Fire custom pattern", PROCESS_NAME)

                val convertedPattern = Shooting.StockPattern.
                    tryConvertToPatternSequence(FAILSAFE_PATTERN)!!

                logPatternInfo(convertedPattern)

                ThreadedEventBus.LAZY_INSTANCE.invoke(
                    StorageGiveDrumRequest(
                        DEFAULT_SHOOTING_MODE,
                        convertedPattern
                )   )
            }
        }
    }
    private fun fireEverything()
    {
        logM.logMd("Jump! Decided: Fire everything", PROCESS_NAME)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            StorageGiveDrumRequest(
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE,
                Array(MAX_BALL_COUNT)
                {
                    BallRequest.Name.ANY_CLOSEST
                }
        )   )
    }



    private fun sendFinishedFiringEvent(requestResult: RequestResult.Name)
    {
        logM.logMd("Shooting stopped, fail reason: $requestResult", PROCESS_ENDING)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            FullFinishedFiringEvent(
                requestResult
        )   )
    }

    private fun logPatternInfo(pattern: Array<BallRequest.Name>)
    {
        var patternString = "Pattern = "
        for (nextColorInPattern in pattern)
            patternString += "$nextColorInPattern, "

        logM.logMd(patternString, GENERIC_INFO)
    }
}