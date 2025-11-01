package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference

import woen239.enumerators.Ball
import woen239.enumerators.BallRequest
import woen239.enumerators.ShotType
import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import org.woen.modules.IModule
import org.woen.modules.scoringSystem.brush.BrushSoft
import org.woen.modules.scoringSystem.brush.SwitchBrush
import org.woen.modules.scoringSystem.turret.Turret
import org.woen.modules.scoringSystem.storage.SwitchStorage

import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS
import org.woen.telemetry.Configs.STORAGE.MAX_WAITING_TIME_FOR_INTAKE_MS
import org.woen.telemetry.Configs.TURRET.MAX_POSSIBLE_DELAY_FOR_BALL_SHOOTING_MS
import org.woen.telemetry.Configs.BRUSH.TIME_FOR_BRUSH_REVERSING

import org.woen.threading.ThreadedEventBus
import org.woen.modules.scoringSystem.turret.RequestTurretAtTargetEvent
import org.woen.modules.scoringSystem.turret.SetCurrentTurretStateEvent

import org.woen.modules.scoringSystem.storage.BottomOpticPareSeesSomethingEvent
import org.woen.modules.scoringSystem.storage.ColorSensorsSeeIntakeIncoming

//import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageIsReadyToEatIntakeEvent
import org.woen.modules.scoringSystem.storage.BallWasEatenByTheStorageEvent

import org.woen.modules.scoringSystem.storage.StorageGiveSingleRequest
import org.woen.modules.scoringSystem.storage.StorageGiveSimpleDrumRequest
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest

import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent
import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent



class ReverseAndThenStartBrushesAgain(var reverseTime: Long)



class ScoringModulesConnector: IModule
{
    private val _storage = SwitchStorage()  //  Schrodinger storage

    private val _isStream  = _storage.isStream()
    private val _isSorting = _storage.isSorting()

    private var _isBusy = AtomicReference(false)
    private var _canRestartBrushes = AtomicReference(false)

    private var _ballWasEaten = AtomicReference(false)
    private var _isAwaitingEating = AtomicReference(false)

    private var _shotWasFired = AtomicReference(false)





    fun setBusy()
    {
        _isBusy.set(true)
    }
    fun setIdle()
    {
        _isBusy.set(false)
    }

    fun setAwaitingEating()
    {
        _isAwaitingEating.set(true)
    }
    fun setNotAwaitingEating()
    {
        _isAwaitingEating.set(false)
    }

    fun ballWasEaten()
    {
        if (_isAwaitingEating.get()) _ballWasEaten.set(true)
    }
    suspend fun shotWasFired(): Boolean
    {
        if (_shotWasFired.get()) return true

        delay(MAX_POSSIBLE_DELAY_FOR_BALL_SHOOTING_MS)
        return true  //!  Temporary dumb always-succeed approach
    }



    suspend fun startIntakeProcess(inputBall: Ball.Name): IntakeResult.Name
    {
        /*----------  How intake in connector module works:
         *  0) = Wait while every request is finished (if is busy)
         *
         *  1) If something fails, exit immediately
         *  2) > If all is good - sends event StorageIsReadyToEatIntakeEvent
         *  3) > Awaits BallWasEatenEvent from BOTTOM hardware color sensor
         *  4) > return intake result when fully finished
         */


        if (isBusy)
        {
            reverseAndThenStartBrushesAfterTimePeriod(TIME_FOR_BRUSH_REVERSING)

            return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        }

        setBusy()
        setAwaitingEating()

        val intakeResult = _storage.handleIntake(inputBall)


        if (_storage.ballCount() >= MAX_BALL_COUNT)
        {
            _canRestartBrushes.set(false)

            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SwitchBrush(
                    BrushSoft.AcktBrush.REVERS,
                    TIME_FOR_BRUSH_REVERSING
            ) )
        }

        setIdle()
        return intakeResult
    }

    fun reverseAndThenStartBrushesAfterTimePeriod(reverseTime: Long)
    {
        _canRestartBrushes.set(true)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SwitchBrush(
                BrushSoft.AcktBrush.REVERS,
                reverseTime))

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            ReverseAndThenStartBrushesAgain(
                reverseTime
            )
        )
    }
    suspend fun startBrushesAfterDelay(delay: Long)
    {
        delay(delay)

        if (_canRestartBrushes.get())
            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SwitchBrush(BrushSoft.AcktBrush.ACKT))
    }
    suspend fun currentlyEatingIntakeProcess()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SwitchBrush(BrushSoft.AcktBrush.ACKT))

        awaitSuccessfulBallIntake()
    }
    suspend fun awaitSuccessfulBallIntake()
    {
        var maxWaitForIntakeMS: Long = 0

        while (!_ballWasEaten.get() &&
            maxWaitForIntakeMS < MAX_WAITING_TIME_FOR_INTAKE_MS)
        {
            delay(DELAY_FOR_EVENT_AWAITING_MS)
            maxWaitForIntakeMS += DELAY_FOR_EVENT_AWAITING_MS
        }

        setNotAwaitingEating()

        //  Bring back when fully finished
//        ThreadedEventBus.LAZY_INSTANCE.invoke(
//            if (maxWaitForIntakeMS > MAX_WAITING_TIME_FOR_INTAKE_MS)
//                         TerminateIntakeEvent()
//                    else BallWasEatenByTheStorageEvent()
//        )

        ThreadedEventBus.LAZY_INSTANCE.invoke((BallWasEatenByTheStorageEvent()))
    }





    suspend fun startDrumRequest(shotType: ShotType,
        requestPattern: Array<BallRequest.Name>,
        failsafePattern: Array<BallRequest.Name>
        ) : RequestResult.Name
    {
        /*----------  How every request in connector module works:
         *  0) = Wait while intake is finished (if is busy)
         *
         *  1) If something fails, finished immediately
         *  2) > If all is good - sends event StorageRequestIsReadyEvent
         *  3) > Waits for the turret being fully accelerated
         *  4) > Push the ball in the turret
         *  5) > Ball flies, and should invoke ShotWasFiredEvent
         *  6) > Awaits ShotWasFiredEvent
         *  7) > > Loop if drum request continues
         *  8) Exit when entire drum was fired
         */

        while (isBusy) delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()


        setTurretToShootMode()
        val requestResult =
            if (_isSorting) _storage.shootEntireDrumRequest(
                requestPattern,
                failsafePattern, shotType)
            else _storage.shootEntireDrumRequest()


        setTurretToWaitMode()
        setIdle()
        return requestResult
    }
    suspend fun startSimpleDrumRequest(): RequestResult.Name
    {
        while (isBusy) delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()


        setTurretToShootMode()
        val requestResult = _storage.shootEntireDrumRequest()


        setTurretToWaitMode()
        setIdle()
        return requestResult
    }
    suspend fun startSingleRequest(ballRequest: BallRequest.Name): RequestResult.Name
    {
        if (_isStream) return RequestResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE

        while (isBusy) delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()


        setTurretToShootMode()
        val requestResult = _storage.handleRequest(ballRequest)


        setTurretToWaitMode()
        setIdle()
        return requestResult
    }

    suspend fun currentlyShootingRequestsProcess()
    {
        var turretHasAccelerated = ThreadedEventBus.LAZY_INSTANCE.invoke(
            RequestTurretAtTargetEvent()).atTarget

        while (!turretHasAccelerated)
        {
            delay (DELAY_FOR_EVENT_AWAITING_MS)

            turretHasAccelerated = ThreadedEventBus.LAZY_INSTANCE.invoke(
                RequestTurretAtTargetEvent()).atTarget
        }

        awaitSuccessfulRequestShot()
    }
    suspend fun awaitSuccessfulRequestShot()
    {
        while (!shotWasFired()) _storage.pushNext()

        _shotWasFired.set(false)

        ThreadedEventBus.LAZY_INSTANCE.invoke(ShotWasFiredEvent())
    }


    fun setTurretToWaitMode()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SetCurrentTurretStateEvent(
                Turret.TurretState.WAITING))
    }
    fun setTurretToShootMode()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SetCurrentTurretStateEvent(
                Turret.TurretState.SHOOT))
    }





    override val isBusy: Boolean get() = _isBusy.get()

    override suspend fun process() { }
    override fun dispose() { }



    init
    {
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            ColorSensorsSeeIntakeIncoming::class, {

                startIntakeProcess(it.inputBall)
            }
        )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageIsReadyToEatIntakeEvent::class, {

                currentlyEatingIntakeProcess()
        } )
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            BottomOpticPareSeesSomethingEvent::class, {

                ballWasEaten()
        } )



        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageGiveSingleRequest::class, {

                startSingleRequest(it.ballRequest)
            }
        )
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageGiveSimpleDrumRequest::class, {

                startSimpleDrumRequest()
            }
        )
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageGiveDrumRequest::class, {

                startDrumRequest(it.shotType,
                    it.requestPattern,
                    it.failsafePattern)
            }
        )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageRequestIsReadyEvent::class, {

                currentlyShootingRequestsProcess()
        } )



        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            ReverseAndThenStartBrushesAgain::class, {
                startBrushesAfterDelay(it.reverseTime)
            }
        )
    }
}