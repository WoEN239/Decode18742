package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import com.qualcomm.robotcore.util.ElapsedTime

import woen239.enumerators.Ball
import woen239.enumerators.BallRequest
import woen239.enumerators.RequestResult
import woen239.enumerators.Shooting

import org.woen.modules.scoringSystem.storage.sorting.DynamicPattern

import org.woen.threading.ThreadedEventBus

import org.woen.modules.camera.OnPatternDetectedEvent
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.StorageHasThreeBallsWithIdenticalColorsEvent

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
    private val _finishedFiring = AtomicBoolean(true)
    private val _patternDetectionAttempts = AtomicInteger(0)
    private val _pattern = DynamicPattern()



    constructor()
    {
        subscribeToPatternDetectionEvents()

        subscribeToDefaultFiringEvent()
    }

    private fun subscribeToPatternDetectionEvents()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            OnPatternDetectedEvent::class, {
                _pattern.setPermanent(it.pattern.subsequence)
                _patternWasDetected.set(true)
            }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            FullFinishedFiringEvent::class, {
                _finishedFiring.set(true)
            }   )
    }
    private fun subscribeToDefaultFiringEvent()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            DefaultFireEvent::class, {
                firePattern()
        }   )
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



    private fun firePattern()
    {
        if (DEFAULT_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (DEFAULT_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN -> {
                if (_patternWasDetected.get())
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            _pattern.permanent()
                    )   )
                else fireFailsafe()
            }

            Shooting.StockPattern.Name.ANY_THREE_IDENTICAL_COLORS -> {
                var storageBalls = ThreadedEventBus.LAZY_INSTANCE.invoke(
                    StorageHasThreeBallsWithIdenticalColorsEvent(
                        0, Ball.Name.NONE
                )   )

                if (storageBalls.maxIdenticalColorCount == MAX_BALL_COUNT)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            DEFAULT_SHOOTING_MODE,
                            Array(3)
                            {
                                Ball.ToBallRequestName(storageBalls.identicalColor)
                            }
                    )   )
                else fireFailsafe()
            }

            else -> {
                var convertedPattern = Shooting.StockPattern.
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
    private fun fireFailsafe()
    {
        if (FAILSAFE_SHOOTING_MODE == Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE)
            fireEverything()

        when (FAILSAFE_PATTERN)
        {
            Shooting.StockPattern.Name.USE_DETECTED_PATTERN -> {
                if (_patternWasDetected.get())
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            FAILSAFE_SHOOTING_MODE,
                            _pattern.permanent()
                    )   )
                else sendFinishedFiringEvent(RequestResult.Name.FAIL_COULD_NOT_DETECT_PATTERN)
            }

            Shooting.StockPattern.Name.ANY_THREE_IDENTICAL_COLORS -> {
                var storageBalls = ThreadedEventBus.LAZY_INSTANCE.invoke(
                    StorageHasThreeBallsWithIdenticalColorsEvent(
                        0, Ball.Name.NONE
                )   )

                if (storageBalls.maxIdenticalColorCount == MAX_BALL_COUNT)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGiveDrumRequest(
                            FAILSAFE_SHOOTING_MODE,
                            Array(3)
                            {
                                Ball.ToBallRequestName(storageBalls.identicalColor)
                            }
                    )   )
                else sendFinishedFiringEvent(RequestResult.Name.FAIL_NOT_ENOUGH_COLORS)
            }

            else -> {
                var convertedPattern = Shooting.StockPattern.
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
                Array(3) {
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