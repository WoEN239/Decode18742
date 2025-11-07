package org.woen.modules.scoringSystem


//import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent

import kotlinx.coroutines.delay
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.brush.SwitchBrush
import org.woen.modules.scoringSystem.storage.BallWasEatenByTheStorageEvent
import org.woen.modules.scoringSystem.storage.BottomOpticPareSeesSomethingEvent
import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.StorageCloseGateForShot
import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntake
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.StorageGiveSimpleDrumRequest
import org.woen.modules.scoringSystem.storage.StorageGiveSingleRequest
import org.woen.modules.scoringSystem.storage.StorageIsReadyToEatIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageOpenGateForShot
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent
import org.woen.modules.scoringSystem.storage.SwitchStorage
import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent
import org.woen.modules.scoringSystem.turret.RequestTurretAtTargetEvent
import org.woen.modules.scoringSystem.turret.SetCurrentTurretStateEvent
import org.woen.modules.scoringSystem.turret.Turret
import org.woen.telemetry.Configs.BRUSH.TIME_FOR_BRUSH_REVERSING
import org.woen.telemetry.Configs.STORAGE.DELAY_BETWEEN_SHOTS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS
import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.MAX_WAITING_TIME_FOR_INTAKE_MS
import org.woen.telemetry.Configs.TURRET.MAX_POSSIBLE_DELAY_FOR_BALL_SHOOTING_MS
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener
import woen239.enumerators.Ball
import woen239.enumerators.BallRequest
import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult
import woen239.enumerators.ShotType
import java.util.concurrent.atomic.AtomicReference


class ReverseAndThenStartBrushesAgain(var reverseTime: Long)



class ScoringModulesConnector
{
    private val _storage = SwitchStorage()  //  Schrodinger storage

    private val _isStream  = _storage.isStream
    private val _isSorting = _storage.isSorting

    private var _isBusy = AtomicReference(false)
    private var _canRestartBrushes = AtomicReference(false)

    private var _ballWasEaten = AtomicReference(false)
    private var _isAwaitingEating = AtomicReference(false)

    private var _shotWasFired = AtomicReference(false)



    constructor()
    {
        setTurretToShootMode()


        //---  Handling received events  ---//

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageGetReadyForIntake::class, {

                startIntakeProcess(it.inputBall)
        }   )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageIsReadyToEatIntakeEvent::class, {

                currentlyEatingIntakeProcess()
        }   )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            BottomOpticPareSeesSomethingEvent::class, {

                safeBallWasEaten()
        }   )



        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageGiveSingleRequest::class, {

                startSingleRequest(it.ballRequest)
        }   )
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageGiveSimpleDrumRequest::class, {

                startSimpleDrumRequest()
        }   )
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageGiveDrumRequest::class, {

                startDrumRequest(
                    it.shotType,
                    it.requestPattern,
                    it.failsafePattern
        )   }   )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageRequestIsReadyEvent::class, {

                currentlyShootingRequestsProcess()
        }   )



        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            ReverseAndThenStartBrushesAgain::class, {
                startBrushesAfterDelay(it.reverseTime)
        }   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.dpad_up }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGetReadyForIntake(Ball.Name.GREEN))


            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("START - INTAKE - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.dpad_down }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateIntakeEvent())

            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("STOP  - INTAKE - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.dpad_left }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveSimpleDrumRequest())

            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("START - REQUEST - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.dpad_right }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateRequestEvent())


            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("STOP  - REQUEST - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))
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


        if (isBusy())
        {
            reverseAndThenStartBrushesAfterTimePeriod(TIME_FOR_BRUSH_REVERSING)

            return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        }

        delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()
        setAwaitingEating()

        ThreadedTelemetry.LAZY_INSTANCE.log("INPUT BALL: $inputBall")
        val intakeResult = _storage.handleIntake(inputBall)


        if (_storage.ballCount() >= MAX_BALL_COUNT)
        {
            _canRestartBrushes.set(false)

            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SwitchBrush(
                    Brush.AcktBrush.REVERS,
                    TIME_FOR_BRUSH_REVERSING
            )   )
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("FINISHED - INTAKE")
        ThreadedTelemetry.LAZY_INSTANCE.log("RESULT: $intakeResult")


        setIdle()
        ThreadedTelemetry.LAZY_INSTANCE.log("IDLE, busy: " + isBusy())
        return intakeResult
    }
    suspend fun currentlyEatingIntakeProcess()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SwitchBrush(Brush.AcktBrush.ACKT) )

        awaitSuccessfulBallIntake()
    }

    suspend fun awaitSuccessfulBallIntake()
    {
        var maxWaitForIntakeMS: Long = 0

        while (!_ballWasEaten.get() && maxWaitForIntakeMS < MAX_WAITING_TIME_FOR_INTAKE_MS)
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


    fun reverseAndThenStartBrushesAfterTimePeriod(reverseTime: Long)
    {
        _canRestartBrushes.set(true)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SwitchBrush(
                Brush.AcktBrush.REVERS,
                reverseTime
        )   )

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            ReverseAndThenStartBrushesAgain(
                reverseTime
        )   )
    }
    suspend fun startBrushesAfterDelay(delay: Long)
    {
        delay(delay)

        if (_canRestartBrushes.get())
            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SwitchBrush(Brush.AcktBrush.ACKT)
            )
    }





    suspend fun startDrumRequest(
        shootingMode: ShotType,
        requestPattern: Array<BallRequest.Name>
    ): RequestResult.Name
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

        while (isBusy()) delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()


        setTurretToShootMode()
        val requestResult =
            if (_isSorting) _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern)
            else _storage.shootEntireDrumRequest()


        setTurretToWaitMode()
        setIdle()
        return requestResult
    }
    suspend fun startDrumRequest(
        shootingMode: ShotType,
        requestPattern: Array<BallRequest.Name>,
        failsafePattern: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        while (isBusy()) delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()


        setTurretToShootMode()
        val requestResult =
            if (_isSorting) _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern,
                failsafePattern)
            else _storage.shootEntireDrumRequest()


        setTurretToWaitMode()
        setIdle()
        return requestResult
    }
    suspend fun startSimpleDrumRequest(): RequestResult.Name
    {
        while (isBusy()) delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()

        ThreadedTelemetry.LAZY_INSTANCE.log("all other processes finished")
        setTurretToShootMode()

        ThreadedTelemetry.LAZY_INSTANCE.log("turret set to shoot mode")
        ThreadedTelemetry.LAZY_INSTANCE.log("starting request search")
        val requestResult = _storage.shootEntireDrumRequest()

        setTurretToWaitMode()

        ThreadedTelemetry.LAZY_INSTANCE.log("SIMPLE DRUM - FINISHED")

        setIdle()
        ThreadedTelemetry.LAZY_INSTANCE.log("IDLE, busy: " + isBusy())
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageCloseGateForShot())

        return requestResult
    }
    suspend fun startSingleRequest(ballRequest: BallRequest.Name): RequestResult.Name
    {
        if (_isStream) return RequestResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE

        while (isBusy()) delay(DELAY_FOR_EVENT_AWAITING_MS)
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
            RequestTurretAtTargetEvent() ).atTarget

        while (!turretHasAccelerated)
        {
            delay(DELAY_FOR_EVENT_AWAITING_MS)

            turretHasAccelerated = ThreadedEventBus.LAZY_INSTANCE.invoke(
                RequestTurretAtTargetEvent() ).atTarget
        }

        awaitSuccessfulRequestShot()
    }


    suspend fun awaitSuccessfulRequestShot()
    {
        delay(DELAY_BETWEEN_SHOTS)
        while (!shotWasFired()) _storage.pushNextWithoutUpdating()

        _shotWasFired.set(false)
        ThreadedEventBus.LAZY_INSTANCE.invoke(ShotWasFiredEvent())
    }





    fun setBusy() = _isBusy.set(true)
    fun setIdle() = _isBusy.set(false)

    fun isBusy(): Boolean = _isBusy.get()


    fun setAwaitingEating() = _isAwaitingEating.set(true)
    fun setNotAwaitingEating() = _isAwaitingEating.set(false)


    fun safeBallWasEaten()
    {
        if (_isAwaitingEating.get())
            _ballWasEaten.set(true)
    }
    suspend fun shotWasFired(): Boolean
    {
        if (_shotWasFired.get()) return true

        delay(MAX_POSSIBLE_DELAY_FOR_BALL_SHOOTING_MS)
        return true  //!  Temporary dumb always-succeed approach
    }


    fun setTurretToWaitMode()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SetCurrentTurretStateEvent(
                Turret.TurretState.WAITING
            )   )
    }
    fun setTurretToShootMode()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SetCurrentTurretStateEvent(
                Turret.TurretState.SHOOT
            )   )
    }
}