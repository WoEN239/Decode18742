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
        val waitDuration = ElapsedTime()
        _patternDetectionAttempts.getAndAdd(1)

        while(waitDuration.milliseconds() < MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS)
        {
            if (_patternWasDetected.get()) return true
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        }

        return false
    }



    private suspend fun firePattern()
    {
        if (DEFAULT_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (DEFAULT_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            _pattern.permanent()
                    )   )
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

                if (storageBalls.maxIdenticalColorCount == ballCount)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            Array(ballCount)
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
        if (FAILSAFE_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (FAILSAFE_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN ->
            {
                if (_patternWasDetected.get()
                    || canTryDetectPattern() && tryGetPattern())
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            FAILSAFE_SHOOTING_MODE,
                            _pattern.permanent()
                    )   )
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

                if (storageBalls.maxIdenticalColorCount == ballCount)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            FAILSAFE_SHOOTING_MODE,
                            Array(ballCount)
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
                    FAILSAFE_PATTERN
                )

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
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            StorageGiveDrumRequest(
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE,
                Array(MAX_BALL_COUNT) {
                    BallRequest.Name.ANY_CLOSEST
                }
        )   )
    }

    private fun sendFinishedFiringEvent(requestResult: RequestResult.Name)
        = ThreadedEventBus.LAZY_INSTANCE.invoke(
            FullFinishedFiringEvent(
                requestResult
        )   )
}