package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult
import org.woen.enumerators.Shooting

import org.woen.modules.light.Light
import org.woen.modules.light.SetLightColorEvent

import org.woen.modules.driveTrain.DriveTrain.DriveMode
import org.woen.modules.driveTrain.SetDriveModeEvent

import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.storage.sorting.SortingStorage

import org.woen.telemetry.LogManager
import org.woen.telemetry.Configs.DELAY
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent

import org.woen.modules.scoringSystem.turret.CurrentlyShooting
//import org.woen.modules.scoringSystem.turret.TurretCurrentPeaked
import org.woen.modules.scoringSystem.turret.SetTurretShootTypeEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent

//import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent

import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageGiveSingleRequest
import org.woen.modules.scoringSystem.storage.StorageGiveDrumRequest
import org.woen.modules.scoringSystem.storage.StorageGiveStreamDrumRequest

import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.GamepadLI
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.SmartCoroutineLI
import org.woen.modules.scoringSystem.storage.Alias.NOTHING
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT
import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent

import org.woen.telemetry.Configs.DEBUG_LEVELS.SMC_DEBUG_LEVELS
import org.woen.telemetry.Configs.DEBUG_LEVELS.SMC_DEBUG_SETTING

import org.woen.telemetry.Configs.BRUSH.TIME_FOR_BRUSH_REVERSING
//import org.woen.telemetry.Configs.DEBUG_LEVELS.EVENTS_FEEDBACK
import org.woen.telemetry.Configs.DEBUG_LEVELS.GAMEPAD_FEEDBACK
import org.woen.telemetry.Configs.DEBUG_LEVELS.GENERIC_INFO
import org.woen.telemetry.Configs.DEBUG_LEVELS.LOGIC_STEPS
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_ENDING
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_STARTING

import org.woen.telemetry.Configs.SORTING_SETTINGS.SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS
import org.woen.telemetry.Configs.SORTING_SETTINGS.TELEOP_PATTERN_SHOOTING_MODE


class ReverseAndThenStartBrushesAgain(var reverseTime: Long)



class ScoringModulesConnector
{
    private val _storage = SortingStorage()
    val logM = LogManager(SMC_DEBUG_SETTING, SMC_DEBUG_LEVELS, "SMC")


    private val _isBusy            = AtomicBoolean(false)
    private val _isAlreadyFiring   = AtomicBoolean(false)
    private val _currentlyShooting = AtomicBoolean(false)
    private val _runningIntakeInstances = AtomicInteger(0)

//    private val _shotWasFired      = AtomicBoolean(false)
    private val _canRestartBrushes = AtomicBoolean(false)
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
        EventBusLI.subscribe(StorageGetReadyForIntakeEvent::class, {
                SmartCoroutineLI.launch {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.ORANGE))

                    startIntakeProcess(it.inputToBottomSlot)
                }
        }   )



        EventBusLI.subscribe(StorageGiveSingleRequest::class, {

                startSingleRequest(it.ballRequest)
        }   )
        EventBusLI.subscribe(StorageGiveStreamDrumRequest::class, {

                startStreamDrumRequest()
        }   )
        EventBusLI.subscribe(StorageGiveDrumRequest::class, {

                startDrumRequest(
                    it.shootingMode,
                    it.requestPattern,
                    it.failsafePattern)
        }   )


//        EventBusLI.subscribe(TurretCurrentPeaked::class, {
//
//                logM.logMd("RECEIVED - Turret current peaked", EVENTS_FEEDBACK)
//                _shotWasFired.set(true)
//        }   )


        EventBusLI.subscribe(StorageRequestIsReadyEvent::class, {

                readyUpForShooting()
        }   )



        EventBusLI.subscribe(ReverseAndThenStartBrushesAgain::class, {

                startBrushesAfterDelay(it.reverseTime)
        }   )

        EventBusLI.subscribe(FullFinishedFiringEvent::class, {

                EventBusLI.invoke(SetLightColorEvent(Light.LightColor.BLUE))
                EventBusLI.invoke(SetDriveModeEvent(DriveMode.DRIVE))
        }   )
    }
    private fun subscribeToGamepad()
    {
        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.right_trigger > 0.5 }, {

                    logM.logMd("Gamepad try start lazy intake", GAMEPAD_FEEDBACK)
                    val startingResult = EventBusLI.invoke(StartLazyIntakeEvent())

                    if (startingResult.startingResult) startBrushes()
                    else
                    {
                        EventBusLI.invoke(SetLightColorEvent(
                            Light.LightColor.BLUE))

                        reverseBrushes(TIME_FOR_BRUSH_REVERSING)
                    }

                    logM.logMd("\ntry start LazyIntake: ${startingResult.startingResult}", PROCESS_STARTING)
        }   )   )


//        GamepadLI.addGamepad1Listener(createClickDownListener(
//            { it.square }, {
//
//                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.ORANGE))
//                    EventBusLI.invoke(StorageGetReadyForIntakeEvent(
//                            Ball.Name.PURPLE))
//
//                    logM.logMd("\nSTART - PURPLE Intake - GAMEPAD", GAMEPAD_FEEDBACK)
//                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}",
//                        GENERIC_INFO)
//        }   )   )
//
//        GamepadLI.addGamepad1Listener(createClickDownListener(
//            { it.circle }, {
//
//                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.ORANGE))
//                    EventBusLI.invoke(StorageGetReadyForIntakeEvent(
//                            Ball.Name.GREEN))
//
//                    logM.logMd("\nSTART - GREEN Intake - GAMEPAD", GAMEPAD_FEEDBACK)
//                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}",
//                        GENERIC_INFO)
//        }   )   )

        GamepadLI.addGamepad1Listener(createClickDownListener(
            { it.left_trigger > 0.5 }, {

                    EventBusLI.invoke(TerminateIntakeEvent())
                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.BLUE))

                    reverseAndThenStartBrushesAfterTimePeriod(TIME_FOR_BRUSH_REVERSING)

                    logM.logMd("\nSTOP  - INTAKE - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}",
                        GENERIC_INFO)
        }   )   )



        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.left_bumper }, {

                    _requestWasTerminated.set(true)

                    EventBusLI.invoke(TerminateRequestEvent())
                    EventBusLI.invoke(SetDriveModeEvent(DriveMode.DRIVE))

                    logM.logMd("STOP  - ANY Request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}",
                        GENERIC_INFO)
        }   )   )

        GamepadLI.addGamepad1Listener(createClickDownListener(
            { it.right_bumper }, {

                    _requestWasTerminated.set(false)

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveStreamDrumRequest())

                    logM.logMd("\nSTART - STREAM Drum request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}",
                        GENERIC_INFO)
        }   )   )

        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.dpad_left }, {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveDrumRequest(
                        TELEOP_PATTERN_SHOOTING_MODE,
                            Shooting.StockPattern.Sequence.Request.GPP))

                    logM.logMd("\nSTART - GPP Drum Request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}",
                        GENERIC_INFO)
        }   )   )
        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.dpad_up }, {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveDrumRequest(
                        TELEOP_PATTERN_SHOOTING_MODE,
                        Shooting.StockPattern.Sequence.Request.PGP))

                    logM.logMd("\nSTART - PGP Drum Request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}",
                        GENERIC_INFO)
        }   )   )

        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.dpad_right }, {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveDrumRequest(
                            TELEOP_PATTERN_SHOOTING_MODE,
                        Shooting.StockPattern.Sequence.Request.PPG))

                    logM.logMd("\nSTART - PPG Drum Request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}",
                        GENERIC_INFO)
        }   )   )

//        GamepadLI.addListener(
//            createClickDownListener(
//            { it.left_trigger > 0.75 }, {
//
//                    EventBusLI.invoke(
//                        SetLightColorEvent(Light.LightColor.GREEN))
//
//                    EventBusLI.invoke(StorageGiveSingleRequest(BallRequest.Name.PURPLE))
//
//                    logM.logMd("\nSTART - PURPLE Request - GAMEPAD")
//                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}", GENERIC_INFO)
//        }   )   )
//
//        GamepadLI.addListener(
//            createClickDownListener(
//            { it.right_trigger > 0.75 }, {
//
//                    EventBusLI.invoke(
//                        SetLightColorEvent(Light.LightColor.GREEN))
//
//                    EventBusLI.invoke(StorageGiveSingleRequest(BallRequest.Name.GREEN))
//
//                    logM.logMd("\nSTART - GREEN Request - GAMEPAD")
//                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}", GENERIC_INFO)
//        }   )   )
    }
    private fun resetParametersToDefault()
    {
        setIdle()

//        _shotWasFired.set(false)
        _canRestartBrushes.set(false)

        _requestWasTerminated.set(false)
    }



    private suspend fun startIntakeProcess(inputToBottomSlot: Ball.Name): IntakeResult.Name
    {
        if (isBusy)
        {
            reverseAndThenStartBrushesAfterTimePeriod(
                TIME_FOR_BRUSH_REVERSING)

            return Intake.FAIL_IS_BUSY
        }
        _runningIntakeInstances.getAndAdd(1)

        logM.logMd("Started - Intake, INPUT BALL: $inputToBottomSlot", PROCESS_STARTING)
        val intakeResult = _storage.handleIntake(inputToBottomSlot)


        if (_storage.alreadyFull())
        {
            _canRestartBrushes.set(false)
            reverseBrushes(TIME_FOR_BRUSH_REVERSING)
        }

        logM.logMd("FINISHED - INTAKE, result: $intakeResult", PROCESS_ENDING)
        _runningIntakeInstances.getAndAdd(-1)
        setIdle()
        return intakeResult
    }



    private fun startBrushes()
        = EventBusLI.invoke(SwitchBrushStateEvent(
                Brush.BrushState.FORWARD))

    private fun reverseBrushes(reverseTime: Long)
    {
        EventBusLI.invoke(SwitchBrushStateEvent(
                Brush.BrushState.REVERSE,
                reverseTime
        )   )
    }
    private fun reverseAndThenStartBrushesAfterTimePeriod(reverseTime: Long)
    {
        _canRestartBrushes.set(true)
        reverseBrushes(reverseTime)

        EventBusLI.invoke(ReverseAndThenStartBrushesAgain(reverseTime))
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

        while (isBusy || _runningIntakeInstances.get() > 0)
        {
            delay(DELAY.EVENT_AWAITING_MS)

            if (terminateRequest)
            {
                _requestWasTerminated.set(false)
                return Request.TERMINATED
            }
        }
        setBusy()

        EventBusLI.invoke(SetTurretShootTypeEvent(Shooting.ShotType.DRUM))

        logM.logMd("Started - SMART drum request", PROCESS_STARTING)
        val requestResult = _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern,
                failsafePattern,
            SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS,
            SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS,
            SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS,
            SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS)

        logM.logMd("FINISHED - SMART drum request", PROCESS_ENDING)


        resumeLogicAfterShooting(requestResult)
        return requestResult
    }
    private suspend fun startStreamDrumRequest(): RequestResult.Name
    {
        if (_isAlreadyFiring.get()) return Request.FAIL_IS_BUSY
        _isAlreadyFiring.set(true)

        while (isBusy || _runningIntakeInstances.get() > 0)
            delay(DELAY.EVENT_AWAITING_MS)
        setBusy()

        EventBusLI.invoke(SetTurretShootTypeEvent(Shooting.ShotType.DRUM))

        EventBusLI.invoke(SetDriveModeEvent(
            DriveMode.SHOOTING)).process.wait()

        logM.logMd("Started  - StreamDrum request", PROCESS_STARTING)
        val requestResult = _storage.streamDrumRequest()
        logM.logMd("FINISHED - StreamDrum request", PROCESS_ENDING)


        resumeLogicAfterShooting(requestResult)
        return requestResult
    }
    private suspend fun startSingleRequest(ballRequest: BallRequest.Name): RequestResult.Name
    {
        if (_isAlreadyFiring.get()) return Request.FAIL_IS_BUSY
        _isAlreadyFiring.set(true)

        while (isBusy || _runningIntakeInstances.get() > 0)
            delay(DELAY.EVENT_AWAITING_MS)
        setBusy()

        logM.logMd("Started - Single request", PROCESS_STARTING)

        EventBusLI.invoke(SetTurretShootTypeEvent(Shooting.ShotType.SINGLE))

        val requestResult = _storage.handleRequest(ballRequest)
        logM.logMd("FINISHED - Single request", PROCESS_ENDING)


        resumeLogicAfterShooting(requestResult)
        return requestResult
    }


    private suspend fun readyUpForShooting()
    {
        logM.logMd("Starting Drivetrain rotation", PROCESS_STARTING)


        val drivingToSHootingZone = EventBusLI.invoke(
                SetDriveModeEvent(
                    DriveMode.SHOOTING)).process

        do
        {
            delay(DELAY.EVENT_AWAITING_MS)
            val nowInShootingZone = drivingToSHootingZone.closed.get()
        }
        while (!nowInShootingZone && !terminateRequest)

        if (terminateRequest)
        {
            logM.logMd("Canceled driving to shooting zone = request was terminated",
                LOGIC_STEPS)

            _requestWasTerminated.set(false)
            return
        }
        logM.logMd("Drivetrain rotated successfully", LOGIC_STEPS)


        EventBusLI.invoke(CurrentlyShooting())
        _currentlyShooting.set(false)
        _requestWasTerminated.set(false)


//
//
//        while (_currentlyShooting.get())
//            delay(DELAY.EVENT_AWAITING_MS)
//
//        _currentlyShooting.set(true)
//        awaitShotFiring()
    }
//    private suspend fun awaitShotFiring()
//    {
//        logM.logMd("SEND - AWAITING SHOT", EVENTS_FEEDBACK)
//        EventBusLI.invoke(CurrentlyShooting())
//        _storage.hwSmartPushNextBall()
//
//
//        var timePassedWaitingForShot = NOTHING.toLong()
//        while (!_shotWasFired.get() && !_requestWasTerminated.get()
//            && timePassedWaitingForShot < DELAY.SMC_MAX_SHOT_AWAITING_MS)
//        {
//            delay(DELAY.EVENT_AWAITING_MS)
//            timePassedWaitingForShot += DELAY.EVENT_AWAITING_MS
//        }
//
//        if (timePassedWaitingForShot >= DELAY.SMC_MAX_SHOT_AWAITING_MS)
//             logM.logMd("\n\n\nShot timeout, assume success\n", LOGIC_STEPS)
//        else logM.logMd("\n\n\nRECEIVED - SHOT FIRED\n", LOGIC_STEPS)
//
//
//        if (_shotWasFired.get()) EventBusLI.invoke(ShotWasFiredEvent())
//
//        _shotWasFired.set(false)
//        _currentlyShooting.set(false)
//        _requestWasTerminated.set(false)
//    }

    private fun sendFinishedFiringEvent (requestResult: RequestResult.Name)
    {
        logM.logMd("FINISHED all firing", PROCESS_ENDING)
        logM.logMd("Send finished firing EVENT", PROCESS_ENDING)

        EventBusLI.invoke(FullFinishedFiringEvent(requestResult))

        //_storage.tryStartLazyIntake()
    }
    private fun resumeLogicAfterShooting(requestResult: RequestResult.Name)
    {
        tryRestartBrushes()
        setIdle()
        _isAlreadyFiring.set(false)
        sendFinishedFiringEvent(requestResult)
    }





    val isBusy get() = _isBusy.get()
    private fun setBusy() = _isBusy.set(true)
    private fun setIdle() = _isBusy.set(false)


    val terminateRequest = _requestWasTerminated.get()


    private fun tryRestartBrushes()
    {
        if (BallCountInStorageEvent(NOTHING).count < MAX_BALL_COUNT)
            EventBusLI.invoke(SwitchBrushStateEvent(
                Brush.BrushState.FORWARD))
    }
}