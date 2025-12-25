package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import org.woen.hotRun.HotRun
import java.util.concurrent.atomic.AtomicBoolean

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult
import org.woen.enumerators.Shooting
import org.woen.modules.driveTrain.DriveTrain
import org.woen.modules.driveTrain.SetDriveModeEvent

import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.storage.sorting.SortingStorage

import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent

import org.woen.modules.scoringSystem.turret.CurrentlyShooting
import org.woen.modules.scoringSystem.turret.TurretCurrentPeaked

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent

import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent

import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent
import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent

import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageGiveSingleRequest
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.StorageGiveStreamDrumRequest

import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.NOTHING
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT

import org.woen.modules.scoringSystem.storage.Alias.GamepadLI
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.TelemetryLI

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.BRUSH.TIME_FOR_BRUSH_REVERSING
import org.woen.telemetry.Configs.SORTING_SETTINGS.SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS



class ReverseAndThenStartBrushesAgain(var reverseTime: Long)



class ScoringModulesConnector
{
    private val _storage = SortingStorage()


    private val _isBusy          = AtomicBoolean(false)
    private val _isAlreadyFiring = AtomicBoolean(false)

    private val _shotWasFired      = AtomicBoolean(false)
    private val _canRestartBrushes = AtomicBoolean(false)

    private val _intakeWasTerminated  = AtomicBoolean(false)
    private val _requestWasTerminated = AtomicBoolean(false)



    constructor()
    {
        subscribeToEvents()
        subscribeToGamepad()

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            resetParametersToDefault()
            //  SortingStorage resets automatically
        }
    }

    private fun subscribeToEvents()
    {
        EventBusLI.subscribe(
            StorageGetReadyForIntakeEvent::class, {
                startIntakeProcess(it.inputBall)
        }   )



        EventBusLI.subscribe(
            StorageGiveSingleRequest::class, {

                startSingleRequest(it.ballRequest)
        }   )
        EventBusLI.subscribe(
            StorageGiveStreamDrumRequest::class, {

                startLazyStreamDrumRequest()
        }   )
        EventBusLI.subscribe(
            StorageGiveDrumRequest::class, {

                startDrumRequest(
                    it.shootingMode,
                    it.requestPattern,
                    it.failsafePattern)
        }   )


        EventBusLI.subscribe(
            TurretCurrentPeaked::class, {

                _shotWasFired.set(true)
        }   )


        EventBusLI.subscribe(
            StorageRequestIsReadyEvent::class, {

                readyUpForShooting()
        }   )



        EventBusLI.subscribe(
            ReverseAndThenStartBrushesAgain::class, {

                startBrushesAfterDelay(it.reverseTime)
        }   )
    }
    private fun subscribeToGamepad()
    {
        GamepadLI.addListener(
            createClickDownListener(
                { it.dpad_up }, {

                    val startingResult = EventBusLI.invoke(
                        StartLazyIntakeEvent(
                            Intake.FAIL_UNKNOWN))

                    TelemetryLI.log(
                        "\nSMC: try start LazyIntake: ${startingResult.startingResult}")
        }   )   )


        GamepadLI.addListener(
            createClickDownListener(
            { it.dpad_right }, {

                    EventBusLI.invoke(
                        StorageGetReadyForIntakeEvent(
                            Ball.Name.PURPLE))

                    TelemetryLI.log("\nSMC: START - PURPLE Intake - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )

        GamepadLI.addListener(
            createClickDownListener(
            { it.dpad_left }, {

                    EventBusLI.invoke(
                        StorageGetReadyForIntakeEvent(
                            Ball.Name.GREEN))

                    TelemetryLI.log("\nSMC: START - GREEN Intake - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )

        GamepadLI.addListener(
            createClickDownListener(
            { it.dpad_down }, {

                    _intakeWasTerminated.set(true)
                    EventBusLI.invoke(TerminateIntakeEvent())

                    TelemetryLI.log("\nSMC: STOP  - INTAKE - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )



        GamepadLI.addListener(
            createClickDownListener(
            { it.right_bumper }, {

                    EventBusLI.invoke(StorageGiveStreamDrumRequest())

                    TelemetryLI.log("\nSMC: START - STREAM Drum request - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )

        GamepadLI.addListener(
            createClickDownListener(
            { it.right_trigger > 0.75 }, {

                    EventBusLI.invoke(StorageGiveSingleRequest(BallRequest.Name.PURPLE))

                    TelemetryLI.log("\nSMC: START - PURPLE Request - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )

        GamepadLI.addListener(
            createClickDownListener(
            { it.left_trigger > 0.75 }, {

                    EventBusLI.invoke(StorageGiveSingleRequest(BallRequest.Name.GREEN))

                    TelemetryLI.log("\nSMC: START - GREEN Request - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )


        GamepadLI.addListener(
            createClickDownListener(
            { it.left_bumper }, {

                    _requestWasTerminated.set(true)
                    EventBusLI.invoke(TerminateRequestEvent())

                    TelemetryLI.log("", "SMC: STOP  - ANY Request - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )
    }
    private fun resetParametersToDefault()
    {
        _isBusy.set(false)

        _shotWasFired.set(false)
        _canRestartBrushes.set(false)

        _intakeWasTerminated .set(false)
        _requestWasTerminated.set(false)
    }



    private suspend fun startIntakeProcess(inputBall: Ball.Name): IntakeResult.Name
    {
        if (isBusy())
        {
            reverseAndThenStartBrushesAfterTimePeriod(
                TIME_FOR_BRUSH_REVERSING)

            return Intake.FAIL_IS_BUSY
        }

        setBusy()

        TelemetryLI.log("SMC: Started - Intake, INPUT BALL: $inputBall")
        val intakeResult = _storage.handleIntake(inputBall)


        if (_storage.alreadyFull())
        {
            _canRestartBrushes.set(false)

            EventBusLI.invoke(
                SwitchBrushStateEvent(
                    Brush.BrushState.REVERSE,
                    TIME_FOR_BRUSH_REVERSING
            )   )
        }

        TelemetryLI.log("SMC: FINISHED - INTAKE, result: $intakeResult")
        setIdle()
        return intakeResult
    }

    private fun reverseAndThenStartBrushesAfterTimePeriod(reverseTime: Long)
    {
        _canRestartBrushes.set(true)

        EventBusLI.invoke(
            SwitchBrushStateEvent(
                Brush.BrushState.REVERSE,
                reverseTime
        )   )

        EventBusLI.invoke(
            ReverseAndThenStartBrushesAgain(
                reverseTime
        )   )
    }
    private suspend fun startBrushesAfterDelay(delay: Long)
    {
        delay(delay)

        if (_canRestartBrushes.get())
            EventBusLI.invoke(
                SwitchBrushStateEvent(
                    Brush.BrushState.FORWARD))
    }



    private suspend fun startDrumRequest(
        shootingMode:    Shooting.Mode,
        requestPattern:  Array<BallRequest.Name>,
        failsafePattern: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        if (_isAlreadyFiring.get()) return Request.FAIL_IS_BUSY
        _isAlreadyFiring.set(true)

        while (isBusy()) delay(DELAY.EVENT_AWAITING_MS)
        setBusy()

        TelemetryLI.log("SMC: Started - SMART drum request")
        val requestResult = _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern,
                failsafePattern,
            SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS,
            SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS,
            SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS,
            SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS)

        TelemetryLI.log("SMC: FINISHED - SMART drum request")


        resumeLogicAfterShooting(requestResult)
        return requestResult
    }
    private suspend fun startLazyStreamDrumRequest(): RequestResult.Name
    {
        if (_isAlreadyFiring.get()) return Request.FAIL_IS_BUSY
        _isAlreadyFiring.set(true)

        while (isBusy()) delay(DELAY.EVENT_AWAITING_MS)
        setBusy()

        EventBusLI.invoke(SetDriveModeEvent(
                DriveTrain.DriveMode.SHOOTING))

        TelemetryLI.log("SMC: Started - LazySTREAM drum request")
        val requestResult = _storage.streamDrumRequest()
        TelemetryLI.log("SMC: FINISHED - LazySTREAM drum request")


        resumeLogicAfterShooting(requestResult)
        return requestResult
    }
    private suspend fun startSingleRequest(ballRequest: BallRequest.Name): RequestResult.Name
    {
        if (_isAlreadyFiring.get()) return Request.FAIL_IS_BUSY
        _isAlreadyFiring.set(true)

        while (isBusy()) delay(DELAY.EVENT_AWAITING_MS)
        setBusy()

        TelemetryLI.log("SMC: Started - Single request")

        val requestResult = _storage.handleRequest(ballRequest)
        TelemetryLI.log("SMC: FINISHED - Single request")


        resumeLogicAfterShooting(requestResult)
        return requestResult
    }


    private suspend fun readyUpForShooting()
    {
        EventBusLI.invoke(SetDriveModeEvent(
            DriveTrain.DriveMode.SHOOTING)).process.wait()

        TelemetryLI.log("[&] SMC: Drivetrain rotated successfully")
        awaitShotFiring()
    }
    private suspend fun awaitShotFiring()
    {
        TelemetryLI.log("SMC - SEND - AWAITING SHOT")
        EventBusLI.invoke(CurrentlyShooting())
        _storage.hwSmartPushNextBall()


        var timePassedWaitingForShot = NOTHING.toLong()
        while (!_shotWasFired.get() && !_requestWasTerminated.get()
            && timePassedWaitingForShot < DELAY.MAX_SHOT_AWAITING_MS)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            timePassedWaitingForShot += DELAY.EVENT_AWAITING_MS
        }

        if (timePassedWaitingForShot >= DELAY.MAX_SHOT_AWAITING_MS)
             TelemetryLI.log("\n\n\nSMC - Shot timeout, assume success\n")
        else TelemetryLI.log("\n\n\nSMC - RECEIVED - SHOT FIRED\n")


        if (_shotWasFired.get()) EventBusLI.invoke(ShotWasFiredEvent())

        _shotWasFired.set(false)
        _requestWasTerminated.set(false)
    }

    private suspend fun sendFinishedFiringEvent(requestResult: RequestResult.Name)
    {
        TelemetryLI.log("SMC: FINISHED all firing, event send")

        EventBusLI.invoke(
            FullFinishedFiringEvent(
                requestResult
        )   )

        _storage.tryStartLazyIntake()
    }
    private suspend fun resumeLogicAfterShooting(requestResult: RequestResult.Name)
    {
        tryRestartBrushes()
        setIdle()
        _isAlreadyFiring.set(false)
        sendFinishedFiringEvent(requestResult)
    }





    fun isBusy(): Boolean = _isBusy.get()
    private fun setBusy() = _isBusy.set(true)
    private fun setIdle() = _isBusy.set(false)

    private fun tryRestartBrushes()
    {
        if (BallCountInStorageEvent(NOTHING).count < MAX_BALL_COUNT)
            EventBusLI.invoke(
                SwitchBrushStateEvent(Brush.BrushState.FORWARD))
    }
}