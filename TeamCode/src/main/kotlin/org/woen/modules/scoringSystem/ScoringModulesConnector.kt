package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import org.woen.hotRun.HotRun
import java.util.concurrent.atomic.AtomicBoolean

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult
import org.woen.enumerators.Shooting
import org.woen.modules.driveTrain.DriveTrain.DriveMode
import org.woen.modules.driveTrain.SetDriveModeEvent
import org.woen.modules.light.Light
import org.woen.modules.light.SetLightColorEvent

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
import org.woen.modules.scoringSystem.storage.Alias.TelemetryLI

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.BRUSH.TIME_FOR_BRUSH_REVERSING
import org.woen.telemetry.Configs.SORTING_SETTINGS.SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad


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
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageGetReadyForIntakeEvent::class, {
                startIntakeProcess(it.inputToBottomSlot)
        }   )



        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageGiveSingleRequest::class, {

                startSingleRequest(it.ballRequest)
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageGiveStreamDrumRequest::class, {

                startLazyStreamDrumRequest()
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageGiveDrumRequest::class, {

                startDrumRequest(
                    it.shootingMode,
                    it.requestPattern,
                    it.failsafePattern)
        }   )


        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            TurretCurrentPeaked::class, {

                _shotWasFired.set(true)
        }   )


        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageRequestIsReadyEvent::class, {

                readyUpForShooting()
        }   )



        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            ReverseAndThenStartBrushesAgain::class, {

                startBrushesAfterDelay(it.reverseTime)
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(FullFinishedFiringEvent::class, {

            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SetLightColorEvent(Light.LightColor.BLUE))

            ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveModeEvent(DriveMode.DRIVE))
        })
    }
    private fun subscribeToGamepad()
    {
        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
                { it.right_trigger > 0.5 }, {

                    TelemetryLI.log("SMC: Gamepad try start lazy intake")
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        SetLightColorEvent(Light.LightColor.ORANGE))

                    val startingResult = ThreadedEventBus.LAZY_INSTANCE.invoke(
                        StartLazyIntakeEvent())

                    if (startingResult.startingResult) startBrushes()
                    else
                    {
                        ThreadedEventBus.LAZY_INSTANCE.invoke(
                            SetLightColorEvent(Light.LightColor.BLUE))

                        reverseBrushes(TIME_FOR_BRUSH_REVERSING)
                    }

                    TelemetryLI.log("\nSMC: try start LazyIntake: ${startingResult.startingResult}")
        }   )   )


//        ThreadedGamepad.LAZY_INSTANCE.addListener(
//            createClickDownListener(
//            { it.square }, {
//
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(
//                        SetLightColorEvent(Light.LightColor.ORANGE))
//
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(
//                        StorageGetReadyForIntakeEvent(
//                            Ball.Name.PURPLE))
//
//                    TelemetryLI.log("\nSMC: START - PURPLE Intake - GAMEPAD")
//                    TelemetryLI.log("SMC isBusy: " + isBusy())
//        }   )   )
//
//        ThreadedGamepad.LAZY_INSTANCE.addListener(
//            createClickDownListener(
//            { it.circle }, {
//
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(
//                        SetLightColorEvent(Light.LightColor.ORANGE))
//
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(
//                        StorageGetReadyForIntakeEvent(
//                            Ball.Name.GREEN))
//
//                    TelemetryLI.log("\nSMC: START - GREEN Intake - GAMEPAD")
//                    TelemetryLI.log("SMC isBusy: " + isBusy())
//        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
            { it.left_trigger > 0.5 }, {

                    _intakeWasTerminated.set(true)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateIntakeEvent())
                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        SetLightColorEvent(Light.LightColor.BLUE))

                    reverseAndThenStartBrushesAfterTimePeriod(TIME_FOR_BRUSH_REVERSING)

                    TelemetryLI.log("\nSMC: STOP  - INTAKE - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )



        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
                { it.left_bumper }, {

                    _requestWasTerminated.set(true)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(TerminateRequestEvent())

                    TelemetryLI.log("", "SMC: STOP  - ANY Request - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
            { it.right_bumper }, {

                    ThreadedEventBus.LAZY_INSTANCE.invoke(
                        SetLightColorEvent(Light.LightColor.GREEN))

                    ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveStreamDrumRequest())

                    TelemetryLI.log("\nSMC: START - STREAM Drum request - GAMEPAD")
                    TelemetryLI.log("SMC isBusy: " + isBusy())
        }   )   )


//        ThreadedGamepad.LAZY_INSTANCE.addListener(
//            createClickDownListener(
//            { it.left_trigger > 0.75 }, {
//
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(
//                        SetLightColorEvent(Light.LightColor.GREEN))
//
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveSingleRequest(BallRequest.Name.PURPLE))
//
//                    TelemetryLI.log("\nSMC: START - PURPLE Request - GAMEPAD")
//                    TelemetryLI.log("SMC isBusy: " + isBusy())
//        }   )   )
//
//        ThreadedGamepad.LAZY_INSTANCE.addListener(
//            createClickDownListener(
//            { it.right_trigger > 0.75 }, {
//
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(
//                        SetLightColorEvent(Light.LightColor.GREEN))
//
//                    ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGiveSingleRequest(BallRequest.Name.GREEN))
//
//                    TelemetryLI.log("\nSMC: START - GREEN Request - GAMEPAD")
//                    TelemetryLI.log("SMC isBusy: " + isBusy())
//        }   )   )
    }
    private fun resetParametersToDefault()
    {
        _isBusy.set(false)

        _shotWasFired.set(false)
        _canRestartBrushes.set(false)

        _intakeWasTerminated .set(false)
        _requestWasTerminated.set(false)
    }



    private suspend fun startIntakeProcess(inputToBottomSlot: Ball.Name): IntakeResult.Name
    {
        if (isBusy())
        {
            reverseAndThenStartBrushesAfterTimePeriod(
                TIME_FOR_BRUSH_REVERSING)

            return Intake.FAIL_IS_BUSY
        }

        setBusy()

        TelemetryLI.log("SMC: Started - Intake, INPUT BALL: $inputToBottomSlot")
        val intakeResult = _storage.handleIntake(inputToBottomSlot)


        if (_storage.alreadyFull())
        {
            _canRestartBrushes.set(false)
            reverseBrushes(TIME_FOR_BRUSH_REVERSING)
        }

        TelemetryLI.log("SMC: FINISHED - INTAKE, result: $intakeResult")
        setIdle()
        return intakeResult
    }



    private fun startBrushes()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SwitchBrushStateEvent(
                Brush.BrushState.FORWARD))
    }
    private fun reverseBrushes(reverseTime: Long)
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SwitchBrushStateEvent(
                Brush.BrushState.REVERSE,
                reverseTime
        )   )
    }
    private fun reverseAndThenStartBrushesAfterTimePeriod(reverseTime: Long)
    {
        _canRestartBrushes.set(true)
        reverseBrushes(reverseTime)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            ReverseAndThenStartBrushesAgain(
                reverseTime
        )   )
    }
    private suspend fun startBrushesAfterDelay(delay: Long)
    {
        delay(delay)
        if (_canRestartBrushes.get()) startBrushes()
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

        ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveModeEvent(
                DriveMode.SHOOTING))

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
        ThreadedEventBus.LAZY_INSTANCE.invoke(SetDriveModeEvent(
            DriveMode.SHOOTING)).process.wait()

        TelemetryLI.log("[&] SMC: Drivetrain rotated successfully")
        awaitShotFiring()
    }
    private suspend fun awaitShotFiring()
    {
        TelemetryLI.log("SMC - SEND - AWAITING SHOT")
        ThreadedEventBus.LAZY_INSTANCE.invoke(CurrentlyShooting())
        _storage.hwSmartPushNextBall()


        var timePassedWaitingForShot = NOTHING.toLong()
        while (!_shotWasFired.get() && !_requestWasTerminated.get()
            && timePassedWaitingForShot < DELAY.SMC_MAX_SHOT_AWAITING_MS)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            timePassedWaitingForShot += DELAY.EVENT_AWAITING_MS
        }

        if (timePassedWaitingForShot >= DELAY.SMC_MAX_SHOT_AWAITING_MS)
             TelemetryLI.log("\n\n\nSMC - Shot timeout, assume success\n")
        else TelemetryLI.log("\n\n\nSMC - RECEIVED - SHOT FIRED\n")


        if (_shotWasFired.get()) ThreadedEventBus.LAZY_INSTANCE.invoke(ShotWasFiredEvent())

        _shotWasFired.set(false)
        _requestWasTerminated.set(false)
    }

    private suspend fun sendFinishedFiringEvent(requestResult: RequestResult.Name)
    {
        TelemetryLI.log("SMC: FINISHED all firing, event send")

        ThreadedEventBus.LAZY_INSTANCE.invoke(
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
            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SwitchBrushStateEvent(Brush.BrushState.FORWARD))
    }
}