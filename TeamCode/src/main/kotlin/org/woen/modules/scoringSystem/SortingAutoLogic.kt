package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import com.qualcomm.robotcore.util.ElapsedTime

import woen239.enumerators.Ball
import woen239.enumerators.BallRequest
import woen239.enumerators.RequestResult
import woen239.enumerators.Shooting

import org.woen.hotRun.HotRun
import org.woen.threading.ThreadedEventBus

import org.woen.modules.scoringSystem.storage.sorting.DynamicPattern

import org.woen.modules.camera.OnPatternDetectedEvent
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent

import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS

import org.woen.telemetry.Configs.SORTING_AUTO_OPMODE.DEFAULT_PATTERN
import org.woen.telemetry.Configs.SORTING_AUTO_OPMODE.DEFAULT_SHOOTING_MODE

import org.woen.telemetry.Configs.SORTING_AUTO_OPMODE.FAILSAFE_PATTERN
import org.woen.telemetry.Configs.SORTING_AUTO_OPMODE.FAILSAFE_SHOOTING_MODE

import org.woen.telemetry.Configs.SORTING_AUTO_OPMODE.MAX_ATTEMPTS_FOR_PATTERN_DETECTION
import org.woen.telemetry.Configs.SORTING_AUTO_OPMODE.MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS
import org.woen.telemetry.ThreadedTelemetry


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
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            OnPatternDetectedEvent::class, {
                _pattern.setPermanent(it.pattern.subsequence)
                _patternWasDetected.set(true)
        }   )
    }
    private fun subscribeToDefaultFiringEvent()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
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
        ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Waiting for detected pattern")

        val waitDuration = ElapsedTime()
        _patternDetectionAttempts.getAndAdd(1)

        while(waitDuration.milliseconds() < MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS)
        {
            if (_patternWasDetected.get())
            {
                ThreadedTelemetry.LAZY_INSTANCE.log("SAL: Pattern detected successfully")
                var i = 0
                var patternString = ""
                while (i < _pattern.permanent().size)
                {
                    patternString += _pattern.permanent()[i].toString() + ", "
                    i++
                }
                ThreadedTelemetry.LAZY_INSTANCE.log(patternString)

                return true
            }
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("SAL: Failed to get pattern")
        return false
    }



    private suspend fun firePattern()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Choosing default")

        if (DEFAULT_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (DEFAULT_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Default trying: fire detected pattern")
                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                {
                    ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Default decided: Fire detected pattern" +
                            "\nSAL pattern: ")

                    var i = 0
                    var patternString = ""
                    while (i < _pattern.permanent().size)
                    {
                        patternString += _pattern.permanent()[i].toString() + ", "
                        i++
                    }
                    ThreadedTelemetry.LAZY_INSTANCE.log(patternString)

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
                val ballCount = Shooting.StockPattern.RequestedBallCount(
                    DEFAULT_PATTERN
                )
                val storageBalls = ThreadedEventBus.LAZY_INSTANCE.invoke(
                    StorageHandleIdenticalColorsEvent(
                        ballCount, Ball.Name.NONE
                )   )

                ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Default decided: Fire identical colors: $ballCount")
                ThreadedTelemetry.LAZY_INSTANCE.log("SAL - got - identical count: " +
                        "${storageBalls.maxIdenticalColorCount}, " +
                        "identical color: ${storageBalls.identicalColor}")

                if (storageBalls.maxIdenticalColorCount >= ballCount)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            Array(storageBalls.maxIdenticalColorCount)
                            {
                                Ball.ToBallRequestName(storageBalls.identicalColor)
                            }
                    )   )
                else fireFailsafe()
            }

            else ->
            {
                val convertedPattern = Shooting.StockPattern.
                    TryConvertToPatternSequence(
                        DEFAULT_PATTERN
                    )

                ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Default decided: Fire custom pattern" +
                        "\nSAL pattern: ")

                var i = 0
                var patternString = ""
                while (i < convertedPattern.size)
                {
                    patternString += convertedPattern[i].toString() + ", "
                    i++
                }
                ThreadedTelemetry.LAZY_INSTANCE.log(patternString)

                if (convertedPattern != null)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            convertedPattern
                    )   )
                else fireFailsafe()
            }
        }
    }
    private suspend fun fireFailsafe()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Choosing failsafe")

        if (FAILSAFE_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (FAILSAFE_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Failsafe trying: fire detected pattern")


                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                {
                    ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Failsafe decided: Fire detected pattern" +
                            "\nSAL pattern: ")

                    var i = 0
                    var patternString = ""
                    while (i < _pattern.permanent().size)
                    {
                        patternString += _pattern.permanent()[i].toString() + ", "
                        i++
                    }
                    ThreadedTelemetry.LAZY_INSTANCE.log(patternString)

                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            FAILSAFE_SHOOTING_MODE,
                            _pattern.permanent()
                    )   )
                }
                else sendFinishedFiringEvent(RequestResult.Name.FAIL_COULD_NOT_DETECT_PATTERN)
            }

            Shooting.StockPattern.Name.ANY_TWO_IDENTICAL_COLORS,
            Shooting.StockPattern.Name.ANY_THREE_IDENTICAL_COLORS ->
            {
                val ballCount = Shooting.StockPattern.RequestedBallCount(
                    DEFAULT_PATTERN
                )
                val storageBalls = ThreadedEventBus.LAZY_INSTANCE.invoke(
                    StorageHandleIdenticalColorsEvent(
                        0, Ball.Name.NONE
                )   )

                ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Failsafe decided: Fire identical colors: $ballCount")
                ThreadedTelemetry.LAZY_INSTANCE.log("SAL - got - identical count: " +
                        "${storageBalls.maxIdenticalColorCount}, " +
                        "identical color: ${storageBalls.identicalColor}")

                if (storageBalls.maxIdenticalColorCount >= ballCount)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            FAILSAFE_SHOOTING_MODE,
                            Array(storageBalls.maxIdenticalColorCount)
                            {
                                Ball.ToBallRequestName(storageBalls.identicalColor)
                            }
                    )   )
                else sendFinishedFiringEvent(RequestResult.Name.FAIL_NOT_ENOUGH_COLORS)
            }

            else ->
            {
                val convertedPattern = Shooting.StockPattern.
                TryConvertToPatternSequence(
                    FAILSAFE_PATTERN)

                ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Failsafe decided: Fire custom pattern" +
                        "\nSAL pattern: ")

                var i = 0
                var patternString = ""
                while (i < convertedPattern.size)
                {
                    patternString += convertedPattern[i].toString() + ", "
                    i++
                }
                ThreadedTelemetry.LAZY_INSTANCE.log(patternString)

                if (convertedPattern != null)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            convertedPattern
                    )   )
                else sendFinishedFiringEvent(RequestResult.Name.FAIL_UNKNOWN)
            }
        }
    }
    private fun fireEverything()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("SAL - ? Decided: Fire everything")

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            StorageGiveDrumRequest(
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE,
                Array(MAX_BALL_COUNT) {
                    BallRequest.Name.ANY_CLOSEST
                }
        )   )
    }

    private fun sendFinishedFiringEvent(requestResult: RequestResult.Name)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("SAL - Shooting stopped, fail reason: $requestResult")

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            FullFinishedFiringEvent(
                requestResult
            )
        )
    }
}