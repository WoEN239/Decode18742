package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference

import woen239.enumerators.Ball
import woen239.enumerators.BallRequest

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult
import woen239.enumerators.Shooting

import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.storage.sorting.SortingStorage

import org.woen.threading.ThreadedEventBus
import org.woen.telemetry.ThreadedTelemetry

import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.modules.scoringSystem.brush.SwitchBrush

import org.woen.modules.scoringSystem.turret.CurrentlyShooting
import org.woen.modules.scoringSystem.turret.TurretVoltageDropped
import org.woen.modules.scoringSystem.turret.RequestTurretAtTargetEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent

import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent

import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent

import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageGiveSingleRequest
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.StorageGiveStreamDrumRequest

import org.woen.telemetry.Configs.BRUSH.TIME_FOR_BRUSH_REVERSING
import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS



class ReverseAndThenStartBrushesAgain(var reverseTime: Long)



class ScoringModulesConnector
{
    private val _storage = SortingStorage()


    private var _isBusy = AtomicReference(false)
    private var _canRestartBrushes = AtomicReference(false)
    private var _isAwaitingEating = AtomicReference(false)
    private var _intakeWasTerminated = AtomicReference(false)

    private var _shotWasFired = AtomicReference(false)
    private var _requestWasTerminated = AtomicReference(false)





    constructor()
    {
        subscribeToEvents()
        subscribeToGamepad()
    }

    fun subscribeToEvents()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageGetReadyForIntakeEvent::class, {

                startIntakeProcess(it.inputBall)
        }   )



        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageGiveSingleRequest::class, {

                startSingleRequest(it.ballRequest)
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageGiveStreamDrumRequest::class, {

                startStreamDrumRequest()
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageGiveDrumRequest::class, {

                startDrumRequest(
                    it.shootingMode,
                    it.requestPattern,
                    it.failsafePattern
        )   }   )


        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            TurretVoltageDropped::class, {

                _shotWasFired.set(true)
            }
        )


        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageRequestIsReadyEvent::class, {

                currentlyShootingRequestsProcess()
        }   )



        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            ReverseAndThenStartBrushesAgain::class, {
                startBrushesAfterDelay(it.reverseTime)
        }   )
    }
    fun subscribeToGamepad()
    {
        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
            { it.dpad_right }, {

                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGetReadyForIntakeEvent(Ball.Name.PURPLE))

                    ThreadedTelemetry.LAZY_INSTANCE.log("\nSTART - PURPLE Intake - GAMEPAD")
                    ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
            { it.dpad_left }, {

                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StorageGetReadyForIntakeEvent(Ball.Name.GREEN))

                    ThreadedTelemetry.LAZY_INSTANCE.log("\nSTART - GREEN Intake - GAMEPAD")
                    ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
            { it.dpad_down }, {

                    _intakeWasTerminated.set(true)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateIntakeEvent())

                    ThreadedTelemetry.LAZY_INSTANCE.log("\nSTOP  - INTAKE - GAMEPAD")
                    ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }   )   )



        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
            { it.right_bumper }, {

                    ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveStreamDrumRequest())

                    ThreadedTelemetry.LAZY_INSTANCE.log("\nSTART - STREAM Drum request - GAMEPAD")
                    ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
            { it.right_trigger > 0.75 }, {

                    ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveSingleRequest(BallRequest.Name.PURPLE))

                    ThreadedTelemetry.LAZY_INSTANCE.log("\nSTART - PURPLE Request - GAMEPAD")
                    ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
            { it.left_trigger > 0.75 }, {

                    ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveSingleRequest(BallRequest.Name.GREEN))

                    ThreadedTelemetry.LAZY_INSTANCE.log("\nSTART - GREEN Request - GAMEPAD")
                    ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }   )   )


        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
            { it.left_bumper }, {

                    _requestWasTerminated.set(true)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateRequestEvent())

                    ThreadedTelemetry.LAZY_INSTANCE.log("\nSTOP  - ANY Request - GAMEPAD")
                    ThreadedTelemetry.LAZY_INSTANCE.log("CONNECTOR STATUS isBusy: " + isBusy())
        }   )   )
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
            reverseAndThenStartBrushesAfterTimePeriod(
                TIME_FOR_BRUSH_REVERSING)

            return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        }

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

        ThreadedTelemetry.LAZY_INSTANCE.log("FINISHED - INTAKE, result: $intakeResult")

        setIdle()
        return intakeResult
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
        shootingMode: Shooting.Mode,
        requestPattern: Array<BallRequest.Name>,
        failsafePattern: Array<BallRequest.Name>
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

        val requestResult = _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern,
                failsafePattern)

        ThreadedTelemetry.LAZY_INSTANCE.log("SMART DrumRequest - FINISHED")

        tryRestartBrushes()
        setIdle()
        return requestResult
    }
    suspend fun startStreamDrumRequest(): RequestResult.Name
    {
        while (isBusy()) delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()

        val requestResult = _storage.shootEntireDrumRequest()
        ThreadedTelemetry.LAZY_INSTANCE.log("SIMPLE DrumRequest - FINISHED")

        tryRestartBrushes()
        setIdle()
        return requestResult
    }
    suspend fun startSingleRequest(ballRequest: BallRequest.Name): RequestResult.Name
    {
        while (isBusy()) delay(DELAY_FOR_EVENT_AWAITING_MS)
        setBusy()

        val requestResult = _storage.handleRequest(ballRequest)
        ThreadedTelemetry.LAZY_INSTANCE.log("SINGLE request - FINISHED")

        tryRestartBrushes()
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
        ThreadedEventBus.LAZY_INSTANCE.invoke(CurrentlyShooting())

        _storage.hwForceResumeBelts()
        while (!_shotWasFired.get() && !_requestWasTerminated.get())
            delay(DELAY_FOR_EVENT_AWAITING_MS)

        _storage.hwForcePauseBelts()

        if (_shotWasFired.get())
            ThreadedEventBus.LAZY_INSTANCE.invoke(ShotWasFiredEvent())

        _shotWasFired.set(false)
        _requestWasTerminated.set(false)
    }





    fun setBusy() = _isBusy.set(true)
    fun setIdle() = _isBusy.set(false)

    fun isBusy(): Boolean = _isBusy.get()


    fun tryRestartBrushes()
    {
        if (BallCountInStorageEvent(0).count < MAX_BALL_COUNT)
            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SwitchBrush(Brush.AcktBrush.ACKT))
    }


    fun setAwaitingEating()    = _isAwaitingEating.set(true)
    fun setNotAwaitingEating() = _isAwaitingEating.set(false)
}