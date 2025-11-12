package org.woen.modules.scoringSystem



import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference

import woen239.enumerators.ShotType

import woen239.enumerators.Ball
import woen239.enumerators.BallRequest

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import org.woen.modules.scoringSystem.turret.Turret
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.storage.sorting.SortingStorage

import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener
import org.woen.threading.ThreadedEventBus
import org.woen.telemetry.ThreadedTelemetry

import org.woen.modules.scoringSystem.brush.SwitchBrush

import org.woen.modules.scoringSystem.turret.SetCurrentTurretStateEvent
import org.woen.modules.scoringSystem.turret.RequestTurretAtTargetEvent

import org.woen.modules.scoringSystem.storage.StorageCloseTurretGateEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent

import org.woen.modules.scoringSystem.storage.StorageIsReadyToEatIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent

import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallWasEatenByTheStorageEvent

import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.StorageGiveSingleRequest
import org.woen.modules.scoringSystem.storage.StorageGiveSimpleDrumRequest

import org.woen.telemetry.Configs.BRUSH.TIME_FOR_BRUSH_REVERSING
import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_BETWEEN_SHOTS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS
import org.woen.telemetry.Configs.STORAGE.MAX_WAITING_TIME_FOR_INTAKE_MS
import org.woen.telemetry.Configs.TURRET.MAX_POSSIBLE_DELAY_FOR_BALL_SHOOTING_MS



class ReverseAndThenStartBrushesAgain(var reverseTime: Long)



class ScoringModulesConnector
{
    private val _storage = SortingStorage()  //  Schrodinger storage


    private var _isBusy = AtomicReference(false)
    private var _canRestartBrushes = AtomicReference(false)

    private var _ballWasEaten = AtomicReference(false)
    private var _isAwaitingEating = AtomicReference(false)

    private var _shotWasFired = AtomicReference(false)



    constructor()
    {
        //---  Handling received events  ---//

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageGetReadyForIntakeEvent::class, {

                startIntakeProcess(it.inputBall)
        }   )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageIsReadyToEatIntakeEvent::class, {

                currentlyEatingIntakeProcess()
        }   )

//        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
//            BottomOpticPareSeesSomethingEvent::class, {
//
//                safeBallWasEaten()
//        }   )



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

                currentlyShootingRequestsProcess(it.shotNum)
        }   )



        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            ReverseAndThenStartBrushesAgain::class, {
                startBrushesAfterDelay(it.reverseTime)
        }   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.dpad_left }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGetReadyForIntakeEvent(Ball.Name.PURPLE))


            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("START - PURPLE INTAKE - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.dpad_right }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGetReadyForIntakeEvent(Ball.Name.GREEN))


            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("START - GREEN INTAKE - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.dpad_down }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateIntakeEvent())

            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("STOP  - INTAKE - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))



        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.triangle }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveSimpleDrumRequest())

            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("START - SIMPLE DRUM REQUEST - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.square }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveSingleRequest(BallRequest.Name.PURPLE))

            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("START - PURPLE REQUEST - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))

        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.circle }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveSingleRequest(BallRequest.Name.GREEN))

            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("START - GREEN REQUEST - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))


        ThreadedGamepad.LAZY_INSTANCE.addListener(createClickDownListener({ it.cross }, {
            ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateRequestEvent())


            ThreadedTelemetry.LAZY_INSTANCE.log("")
            ThreadedTelemetry.LAZY_INSTANCE.log("STOP  - REQUEST - GAMEPAD")
            ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }))



        setTurretToShootMode()
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


        if (_storage.anyBallCount() >= MAX_BALL_COUNT)
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
        val requestResult = _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern)


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
        val requestResult = _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern,
                failsafePattern)


        setTurretToWaitMode()
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SwitchBrush(Brush.AcktBrush.ACKT)
        )
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

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SwitchBrush(Brush.AcktBrush.ACKT)
        )
        setIdle()
        ThreadedTelemetry.LAZY_INSTANCE.log("IDLE, busy: " + isBusy())
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageCloseTurretGateEvent())

        return requestResult
    }
    suspend fun startSingleRequest(ballRequest: BallRequest.Name): RequestResult.Name
    {
        while (isBusy()) delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()

        ThreadedTelemetry.LAZY_INSTANCE.log("all other processes finished")

        setTurretToShootMode()
        ThreadedTelemetry.LAZY_INSTANCE.log("turret set to shoot mode")
        ThreadedTelemetry.LAZY_INSTANCE.log("starting request search")

        val requestResult = _storage.handleRequest(ballRequest)


        setTurretToWaitMode()
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SwitchBrush(Brush.AcktBrush.ACKT)
        )
        setIdle()
        ThreadedTelemetry.LAZY_INSTANCE.log("IDLE, busy: " + isBusy())
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageCloseTurretGateEvent())
        return requestResult
    }

    suspend fun currentlyShootingRequestsProcess(shotNum: Int)
    {
        var turretHasAccelerated = ThreadedEventBus.LAZY_INSTANCE.invoke(
            RequestTurretAtTargetEvent() ).atTarget

        while (!turretHasAccelerated)
        {
            delay(DELAY_FOR_EVENT_AWAITING_MS)

            turretHasAccelerated = ThreadedEventBus.LAZY_INSTANCE.invoke(
                RequestTurretAtTargetEvent() ).atTarget
        }

        awaitSuccessfulRequestShot(shotNum)
    }


    suspend fun awaitSuccessfulRequestShot(shotNum: Int)
    {
        delay(DELAY_BETWEEN_SHOTS)
        _storage.pushNextWithoutUpdating(shotNum)
        while (!shotWasFired()) _storage.pushNextWithoutUpdating(shotNum)

        _shotWasFired.set(false)
        ThreadedEventBus.LAZY_INSTANCE.invoke(ShotWasFiredEvent())
    }





    fun setBusy() = _isBusy.set(true)
    fun setIdle() = _isBusy.set(false)

    fun isBusy(): Boolean = _isBusy.get()


    fun setAwaitingEating() = _isAwaitingEating.set(true)
    fun setNotAwaitingEating() = _isAwaitingEating.set(false)


//    fun safeBallWasEaten()
//    {
//        if (_isAwaitingEating.get())
//            _ballWasEaten.set(true)
//    }
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