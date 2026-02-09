package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun

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

import org.woen.utils.process.RunStatus
import org.woen.telemetry.LogManager
import org.woen.telemetry.configs.Configs.DELAY
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent
import org.woen.modules.scoringSystem.storage.OdometryIsAlignedForShootingEvent

//import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent
import org.woen.modules.scoringSystem.storage.FullFinishedFiringEvent

import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
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

import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.SMC_DEBUG_LEVELS
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.SMC_DEBUG_SETTING

import org.woen.telemetry.configs.Configs.BRUSH.TIME_FOR_BRUSH_REVERSING

import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.ATTEMPTING_LOGIC
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.GAMEPAD_FEEDBACK
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.LOGIC_STEPS
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.PROCESS_ENDING
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.PROCESS_STARTING
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.RACE_CONDITION

import org.woen.telemetry.configs.Configs.PROCESS_ID.INTAKE
import org.woen.telemetry.configs.Configs.PROCESS_ID.RUNNING_INTAKE_INSTANCE
import org.woen.telemetry.configs.Configs.PROCESS_ID.DRUM_REQUEST
import org.woen.telemetry.configs.Configs.PROCESS_ID.LAZY_INTAKE
import org.woen.telemetry.configs.Configs.PROCESS_ID.SINGLE_REQUEST
import org.woen.telemetry.configs.Configs.PROCESS_ID.PRIORITY_SETTING_FOR_SCORING_CONNECTOR

import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.TELEOP_PATTERN_SHOOTING_MODE
import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.INCLUDE_PREVIOUS_UNFINISHED_TO_REQUEST_ORDER
import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.INCLUDE_PREVIOUS_UNFINISHED_TO_FAILSAFE_ORDER
import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.AUTO_UPDATE_UNFINISHED_FOR_NEXT_PATTERN
import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.IF_AUTO_UPDATE_UNFINISHED_USE_FAILSAFE_ORDER

import org.woen.telemetry.configs.RobotSettings.CONTROLS.DRIVE_TO_SHOOTING_ZONE
import org.woen.telemetry.configs.RobotSettings.CONTROLS.IGNORE_DUPLICATE_SHOOTING_COMMAND
import org.woen.telemetry.configs.RobotSettings.CONTROLS.TRY_TERMINATE_INTAKE_WHEN_SHOOTING



class ReverseAndThenStartBrushesAgain(var reverseTime: Long)



class ScoringModulesConnector
{
    private val _storage   = SortingStorage()
    private val _runStatus = RunStatus(PRIORITY_SETTING_FOR_SCORING_CONNECTOR)
    val logM = LogManager(SMC_DEBUG_SETTING, SMC_DEBUG_LEVELS, "SMC")

//    private val _shotWasFired      = AtomicBoolean(false)



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


//        EventBusLI.subscribe(OpticDetectedShot::class, {
//
//                logM.logMd("RECEIVED - Optic: Shot fired", EVENTS_FEEDBACK)
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

                    logM.logMd("Gamepad: try start lazy intake", GAMEPAD_FEEDBACK)
                    val startingResult = EventBusLI.invoke(StartLazyIntakeEvent())

                    if (startingResult.startingResult)
                    {
                        startBrushes()
                        setActiveProcess(LAZY_INTAKE)
                    }
                    else
                    {
                        EventBusLI.invoke(SetLightColorEvent(
                            Light.LightColor.BLUE))

                        reverseBrushes(TIME_FOR_BRUSH_REVERSING)
                    }

                    logM.logMd("\ntry start LazyIntake: ${startingResult.startingResult}",
                        ATTEMPTING_LOGIC)
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
//                        RACE_CONDITION)
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
//                        RACE_CONDITION)
//        }   )   )

//        GamepadLI.addGamepad1Listener(createClickDownListener(
//            { it.square }, {
//
//                EventBusLI.invoke(StorageUpdateAfterLazyIntakeEvent(
//                        Shooting
//                            .StockPattern
//                            .Sequence
//                            .Storage
//                            .Name.GPP
//                    )   )
//        }   )   )

        GamepadLI.addGamepad1Listener(createClickDownListener(
            { it.left_trigger > 0.5 }, {

                    if (EventBusLI.invoke(TerminateIntakeEvent()).stoppingResult)
                    {
                        _runStatus.safeRemoveThisProcessIdFromQueue(
                            _runStatus.getCurrentActiveProcess())
                        _runStatus.clearCurrentActiveProcess()
                    }
                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.BLUE))

                    reverseAndThenStartBrushesAfterTimePeriod(TIME_FOR_BRUSH_REVERSING)

                    logM.logMd("\nSTOP  - INTAKE - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: $isUsedByAnyProcess", RACE_CONDITION)
        }   )   )



        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.left_bumper }, {

                    val activeProcessId = _runStatus.getCurrentActiveProcess()
                    if (activeProcessId == DRUM_REQUEST ||
                        activeProcessId == SINGLE_REQUEST)
                        _runStatus.addProcessToTerminationList(activeProcessId)

                    EventBusLI.invoke(TerminateRequestEvent())
                    EventBusLI.invoke(SetDriveModeEvent(DriveMode.DRIVE))

                    logM.logMd("STOP  - ANY Request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: $isUsedByAnyProcess", RACE_CONDITION)
        }   )   )

        GamepadLI.addGamepad1Listener(createClickDownListener(
            { it.right_bumper }, {

                    _runStatus.safeRemoveThisProcessFromTerminationList(
                        DRUM_REQUEST, SINGLE_REQUEST)

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveStreamDrumRequest())

                    logM.logMd("\nSTART - STREAM Drum request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: $isUsedByAnyProcess", RACE_CONDITION)
        }   )   )

        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.dpad_left }, {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveDrumRequest(
                        TELEOP_PATTERN_SHOOTING_MODE,
                            Shooting.StockPattern.Sequence.Request.GPP))

                    logM.logMd("\nSTART - GPP Drum Request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: $isUsedByAnyProcess", RACE_CONDITION)
        }   )   )
        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.dpad_up }, {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveDrumRequest(
                        TELEOP_PATTERN_SHOOTING_MODE,
                        Shooting.StockPattern.Sequence.Request.PGP))

                    logM.logMd("\nSTART - PGP Drum Request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: $isUsedByAnyProcess", RACE_CONDITION)
        }   )   )

        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.dpad_right }, {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveDrumRequest(
                            TELEOP_PATTERN_SHOOTING_MODE,
                        Shooting.StockPattern.Sequence.Request.PPG))

                    logM.logMd("\nSTART - PPG Drum Request - GAMEPAD", GAMEPAD_FEEDBACK)
                    logM.logMd("isBusy: $isUsedByAnyProcess", RACE_CONDITION)
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
//                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}", RACE_CONDITION)
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
//                    logM.logMd("isBusy: ${isBusy || _runningIntakeInstances.get() > 0}", RACE_CONDITION)
//        }   )   )
    }
    private fun resetParametersToDefault()
    {
        _runStatus.fullResetToActiveState()

//        _shotWasFired.set(false)
    }



    private suspend fun startIntakeProcess(inputToBottomSlot: Ball.Name): IntakeResult.Name
    {
        logM.logMd("Checking race condition before intake", RACE_CONDITION)
        if (_runStatus.isUsedByAnotherProcess(
                INTAKE, RUNNING_INTAKE_INSTANCE))
        {
            reverseAndThenStartBrushesAfterTimePeriod(
                TIME_FOR_BRUSH_REVERSING)

            logM.logMd("Intake race condition: IS_BUSY", RACE_CONDITION)
            return Intake.FAIL_IS_BUSY
        }
        setActiveProcess(INTAKE)
        _runStatus.addProcessToQueue(RUNNING_INTAKE_INSTANCE)

        logM.logMd("Started - Intake, INPUT BALL: $inputToBottomSlot", PROCESS_STARTING)
        val intakeResult = _storage.handleIntake(inputToBottomSlot)


        if (_storage.alreadyFull()) reverseBrushes(TIME_FOR_BRUSH_REVERSING)


        logM.logMd("FINISHED - INTAKE, result: $intakeResult", PROCESS_ENDING)

        resumeLogicAfterIntake()
        return intakeResult
    }



    private fun startBrushes()
        = EventBusLI.invoke(SwitchBrushStateEvent(
                Brush.BrushState.FORWARD))
    private fun reverseBrushes(reverseTime: Long)
        = EventBusLI.invoke(SwitchBrushStateEvent(
                Brush.BrushState.INFINITE_REVERSE, reverseTime))

    private fun reverseAndThenStartBrushesAfterTimePeriod(reverseTime: Long)
    {
        reverseBrushes(reverseTime)
        EventBusLI.invoke(ReverseAndThenStartBrushesAgain(reverseTime))
    }
    private suspend fun startBrushesAfterDelay(delay: Long)
    {
        delay(delay)
        logM.logMd("Going to restart brushes: ${!_storage.alreadyFull()}",
            LOGIC_STEPS)

        if (!_storage.alreadyFull()) startBrushes()
    }



    private suspend fun startDrumRequest(
        shootingMode:    Shooting.Mode,
        requestPattern:  Array<BallRequest.Name>,
        failsafePattern: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val startingResult = handleRequestRaceCondition(DRUM_REQUEST)
        if (RequestResult.didFail(startingResult)) return startingResult


        logM.logMd("Started - SMART drum request", PROCESS_STARTING)
        val requestResult = _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern,
                failsafePattern,
            INCLUDE_PREVIOUS_UNFINISHED_TO_REQUEST_ORDER,
            INCLUDE_PREVIOUS_UNFINISHED_TO_FAILSAFE_ORDER,
            AUTO_UPDATE_UNFINISHED_FOR_NEXT_PATTERN,
            IF_AUTO_UPDATE_UNFINISHED_USE_FAILSAFE_ORDER)


        logM.logMd("FINISHED - SMART drum request", PROCESS_ENDING)
        return resumeLogicAfterShooting(requestResult, DRUM_REQUEST)
    }
    private suspend fun startStreamDrumRequest(): RequestResult.Name
    {
        val startingResult = handleRequestRaceCondition(DRUM_REQUEST)
        if (RequestResult.didFail(startingResult)) return startingResult

        if (!tryDrivingToShootingZone(DRUM_REQUEST))
            return resumeLogicAfterShooting(
                Request.TERMINATED, DRUM_REQUEST)

        logM.logMd("Started  - StreamDrum request", PROCESS_STARTING)
        val requestResult = _storage.streamDrumRequest()
        logM.logMd("FINISHED - StreamDrum request", PROCESS_ENDING)

        return resumeLogicAfterShooting(requestResult, DRUM_REQUEST)
    }
    private suspend fun startSingleRequest(ballRequest: BallRequest.Name): RequestResult.Name
    {
        val startingResult = handleRequestRaceCondition(SINGLE_REQUEST)
        if (RequestResult.didFail(startingResult)) return startingResult

        logM.logMd("Started - Single request", PROCESS_STARTING)
        val requestResult = _storage.handleRequest(ballRequest)
        logM.logMd("FINISHED - Single request", PROCESS_ENDING)

        return resumeLogicAfterShooting(requestResult, SINGLE_REQUEST)
    }



    private suspend fun tryDrivingToShootingZone(processId: Int): Boolean
    {
        logM.logMd("Attempting to drive to shooting zone: $DRIVE_TO_SHOOTING_ZONE",
            ATTEMPTING_LOGIC)

        if (DRIVE_TO_SHOOTING_ZONE)
        {
            val imDriving = EventBusLI.invoke(SetDriveModeEvent(
                DriveMode.SHOOTING)).process

            while (!imDriving.closed.get() &&
                !_runStatus.isForcedToTerminateThisProcess(processId))
                delay(DELAY.EVENT_AWAITING_MS)
        }

        logM.logMd("Finished driving, terminated: " +
                "${_runStatus.isForcedToTerminateThisProcess(processId)}",
            PROCESS_ENDING)

        return !_runStatus.isForcedToTerminateThisProcess(processId)
    }
    private suspend fun readyUpForShooting()
    {
        logM.logMd("Starting Drivetrain rotation", PROCESS_STARTING)

        var activeProcessId = _runStatus.getCurrentActiveProcess()
        if (activeProcessId != DRUM_REQUEST &&
            activeProcessId != SINGLE_REQUEST) activeProcessId = DRUM_REQUEST

        if (!tryDrivingToShootingZone(activeProcessId)) return
        logM.logMd("Drivetrain rotated successfully", LOGIC_STEPS)

        EventBusLI.invoke(OdometryIsAlignedForShootingEvent())

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
//        while (!_shotWasFired.get() && !isUsedByAnyProcess)
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
//        _shotWasFired.set(false)
//    }

    private fun sendFinishedFiringEvent (requestResult: RequestResult.Name)
    {
        logM.logMd("FINISHED all firing", PROCESS_ENDING)
        logM.logMd("Send finished firing EVENT", PROCESS_ENDING)

        EventBusLI.invoke(FullFinishedFiringEvent(requestResult))

//        _storage.tryStartLazyIntake()
    }
    private fun resumeLogicAfterIntake()
    {
        _runStatus.safeRemoveThisProcessIdFromQueue(INTAKE)
        _runStatus.safeRemoveOnlyOneInstanceOfThisProcessFromQueue(
            RUNNING_INTAKE_INSTANCE)
    }
    private fun resumeLogicAfterShooting(
        requestResult: RequestResult.Name,
        processId: Int): RequestResult.Name
    {
        tryRestartBrushes()

        _runStatus.safeRemoveOnlyOneInstanceOfThisProcessFromQueue(processId)
        _runStatus.safeRemoveThisProcessFromTerminationList(processId)

        _runStatus.clearCurrentActiveProcess()
        sendFinishedFiringEvent(requestResult)

        return requestResult
    }



    private suspend fun handleRequestRaceCondition(processId: Int): RequestResult.Name
    {
        logM.logMd("Checking race condition before shooting", RACE_CONDITION)
        while (isUsedByAnyProcess)
        {
            delay(DELAY.EVENT_AWAITING_MS)


            if (IGNORE_DUPLICATE_SHOOTING_COMMAND &&
                _runStatus.isUsedByThisProcess(processId))
            {
                logM.logMd("Ignored duplicate shooting command", LOGIC_STEPS)
                return Request.FAIL_IS_BUSY
            }


            if (TRY_TERMINATE_INTAKE_WHEN_SHOOTING && isIntakeActive())
            {
                logM.logMd("Attempting intake termination for shooting",
                    LOGIC_STEPS)

                if (!EventBusLI.invoke(TerminateIntakeEvent()).stoppingResult)
                    return Request.FAIL_IS_BUSY

                logM.logMd("Successful intake termination",
                    LOGIC_STEPS)

                _runStatus.safeRemoveThisProcessIdFromQueue(
                    _runStatus.getCurrentActiveProcess())
                _runStatus.clearCurrentActiveProcess()

                return Request.SUCCESS
            }


            if (_runStatus.isForcedToTerminateThisProcess(processId))
                return Request.TERMINATED
        }

        logM.logMd("Initial race condition check passed", RACE_CONDITION)
        setActiveProcess(processId)
        return Request.SUCCESS
    }
    private fun setActiveProcess(processId: Int)
    {
        _runStatus.addProcessToQueue(processId)
        _runStatus.setCurrentActiveProcess(processId)
    }
    private fun isDuplicateProcess(processId: Int)
        = _runStatus.isUsedByThisProcess(DRUM_REQUEST)
    private fun isIntakeActive(): Boolean
    {
        val    activeProcess = _runStatus.getCurrentActiveProcess()
        return activeProcess == INTAKE || activeProcess == LAZY_INTAKE
    }



    val isUsedByAnyProcess get() = _runStatus.isUsedByAnyProcess()



    private fun tryRestartBrushes()
    {
        if (BallCountInStorageEvent(NOTHING).count < MAX_BALL_COUNT)
            EventBusLI.invoke(SwitchBrushStateEvent(
                Brush.BrushState.FORWARD))
    }
}