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

import org.woen.modules.scoringSystem.storage.sorting.DynamicPattern

import org.woen.modules.camera.OnPatternDetectedEvent
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent

import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.TelemetryLI

import org.woen.telemetry.Configs.DELAY

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



    constructor()
    {
        subscribeToPatternDetectionEvents()
        subscribeToDefaultFiringEvent()

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            resetParametersToDefault()
        }
    }


    private fun subscribeToPatternDetectionEvents()
    {
        EventBusLI.subscribe(
            OnPatternDetectedEvent::class, {
                _pattern.setPermanent(it.pattern.subsequence)
                _patternWasDetected.set(true)
        }   )
    }
    private fun subscribeToDefaultFiringEvent()
    {
        EventBusLI.subscribe(
            DefaultFireEvent::class, {
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
        TelemetryLI.log("SAL - Waiting for detected pattern")

        val waitDuration = ElapsedTime()
        _patternDetectionAttempts.getAndAdd(1)

        while(waitDuration.milliseconds() < MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS)
        {
            if (_patternWasDetected.get())
            {
                TelemetryLI.log("SAL: Pattern detected successfully")

                var patternString = "SAL pattern: "
                for (nextColorInPattern in _pattern.permanent())
                    patternString += "$nextColorInPattern, "
                TelemetryLI.log(patternString)

                return true
            }
            delay(DELAY.EVENT_AWAITING_MS)
        }

        TelemetryLI.log("SAL: Failed to get pattern")
        return false
    }



    private suspend fun firePattern()
    {
        TelemetryLI.log("SAL - Choosing default")

        if (DEFAULT_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (DEFAULT_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                TelemetryLI.log("SAL - Default trying: fire detected pattern")
                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                {
                    TelemetryLI.log("SAL - Default decided: Fire detected pattern")

                    var patternString = "SAL pattern: "
                    for (nextColorInPattern in _pattern.permanent())
                        patternString += "$nextColorInPattern, "
                    TelemetryLI.log(patternString)

                    EventBusLI.invoke(
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
                val ballCount = Shooting.StockPattern.requestedBallCount(
                    DEFAULT_PATTERN)
                val storageBalls = EventBusLI.invoke(
                    StorageHandleIdenticalColorsEvent(
                        ballCount, Ball.Name.NONE
                )   )

                TelemetryLI.log("SAL - Default decided: Fire identical colors: $ballCount")
                TelemetryLI.log("SAL - got - identical count: " +
                        "${storageBalls.maxIdenticalColorCount}, " +
                        "identical color: ${storageBalls.identicalColor}")

                if (storageBalls.maxIdenticalColorCount >= ballCount)
                    EventBusLI.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            Array(storageBalls.maxIdenticalColorCount)
                            {
                                Ball.toBallRequestName(storageBalls.identicalColor)
                            }
                    )   )
                else fireFailsafe()
            }

            else ->
            {
                val convertedPattern = Shooting.StockPattern.
                    tryConvertToPatternSequence(DEFAULT_PATTERN)!!

                TelemetryLI.log("SAL - Default decided: Fire custom pattern")

                var patternString = "SAL pattern: "
                for (nextColorInPattern in _pattern.permanent())
                    patternString += "$nextColorInPattern, "
                TelemetryLI.log(patternString)

                EventBusLI.invoke(
                    StorageGiveDrumRequest(
                        DEFAULT_SHOOTING_MODE,
                        convertedPattern
                )   )
            }
        }
    }
    private suspend fun fireFailsafe()
    {
        TelemetryLI.log("SAL - Choosing failsafe")

        if (FAILSAFE_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (FAILSAFE_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                TelemetryLI.log("SAL - Failsafe trying: fire detected pattern")

                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                {
                    TelemetryLI.log("SAL - Failsafe decided: Fire detected pattern")

                    var patternString = "SAL pattern: "
                    for (nextColorInPattern in _pattern.permanent())
                        patternString += "$nextColorInPattern, "

                    TelemetryLI.log(patternString)

                    EventBusLI.invoke(
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
                val ballCount = Shooting.StockPattern.requestedBallCount(
                    DEFAULT_PATTERN)
                val storageBalls = EventBusLI.invoke(
                    StorageHandleIdenticalColorsEvent(
                        0, Ball.Name.NONE
                )   )

                TelemetryLI.log("SAL - Failsafe decided: Fire identical colors: $ballCount")
                TelemetryLI.log("SAL - got - identical count: " +
                        "${storageBalls.maxIdenticalColorCount}, " +
                        "identical color: ${storageBalls.identicalColor}")

                if (storageBalls.maxIdenticalColorCount >= ballCount)
                    EventBusLI.invoke(
                        StorageGiveDrumRequest(
                            FAILSAFE_SHOOTING_MODE,
                            Array(storageBalls.maxIdenticalColorCount)
                            {
                                Ball.toBallRequestName(storageBalls.identicalColor)
                            }
                    )   )
                else sendFinishedFiringEvent(Request.NOT_ENOUGH_COLORS)
            }

            else ->
            {
                val convertedPattern = Shooting.StockPattern.
                    tryConvertToPatternSequence(FAILSAFE_PATTERN)!!

                TelemetryLI.log("SAL - Failsafe decided: Fire custom pattern")

                var patternString = "SAL pattern: "
                for (nextColorInPattern in _pattern.permanent())
                    patternString += "$nextColorInPattern, "

                TelemetryLI.log(patternString)

                EventBusLI.invoke(
                    StorageGiveDrumRequest(
                        DEFAULT_SHOOTING_MODE,
                        convertedPattern
                )   )
            }
        }
    }
    private fun fireEverything()
    {
        TelemetryLI.log("SAL - ? Decided: Fire everything")

        EventBusLI.invoke(
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
        TelemetryLI.log("SAL - Shooting stopped, fail reason: $requestResult")

        EventBusLI.invoke(
            FullFinishedFiringEvent(
                requestResult
        )   )
    }
}