package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.Shooting
import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult

import org.woen.modules.light.Light
import org.woen.modules.light.SetLightColorEvent

import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.storage.sorting.SortingStorage

import org.woen.hotRun.HotRun
import org.woen.utils.process.RunStatus

import org.woen.telemetry.LogManager
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.modules.driveTrain.SetDriveModeEvent
import org.woen.modules.driveTrain.DriveTrain.DriveMode

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
import org.woen.modules.scoringSystem.storage.Alias.HotRunLI
import org.woen.modules.scoringSystem.storage.Alias.GamepadLI
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.SmartCoroutineLI
import org.woen.modules.scoringSystem.storage.Alias.NOTHING
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT

import org.woen.telemetry.configs.Debug
import org.woen.telemetry.configs.Delay
import org.woen.telemetry.configs.ProcessId
import org.woen.telemetry.configs.Configs.BRUSH
import org.woen.telemetry.configs.RobotSettings.CONTROLS
import org.woen.telemetry.configs.RobotSettings.TELEOP
import org.woen.telemetry.configs.RobotSettings.AUTONOMOUS



class ReverseAndThenStartBrushesAgain(var reverseTime: Long)



class ScoringModulesConnector
{
    private val _storage   = SortingStorage()
    private val _runStatus = RunStatus(ProcessId.PRIORITY_SETTING_FOR_SMC)
    private val logM = LogManager(Debug.SMC)
    //  don't use _naming for shortening reasons

//    private val _shotWasFired      = AtomicBoolean(false)



    constructor()
    {
        subscribeToEvents()
        subscribeToGamepad()
//        subscribeToGamepadTests()

        HotRunLI.opModeInitEvent += {
            resetParametersToDefault()
            //  SortingStorageModule resets automatically
        }
    }

    private fun subscribeToEvents()
    {
        EventBusLI.subscribe(StorageGetReadyForIntakeEvent::class, {
                SmartCoroutineLI.launch {

                    val curMode = HotRunLI.currentRunMode
                    if (curMode == HotRun.RunMode.AUTO
                        && AUTONOMOUS.IGNORE_COLOR_SENSORS
                    ||  curMode == HotRun.RunMode.MANUAL
                        && TELEOP.IGNORE_COLOR_SENSORS)
                        return@launch

                    EventBusLI.invoke(SetLightColorEvent(
                        Light.LightColor.ORANGE))

                    startIntakeProcess(it.inputToBottomSlot)
        }   }   )



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

                    logM.logMd("Gamepad: try start lazy intake", Debug.GAMEPAD)
                    val startingResult = EventBusLI.invoke(StartLazyIntakeEvent())

                    if (startingResult.startingResult)
                    {
                        EventBusLI.invoke(SetLightColorEvent(
                            Light.LightColor.ORANGE))

                        startBrushes()
                        setActiveProcess(ProcessId.LAZY_INTAKE)
                    }
                    else
                    {
                        EventBusLI.invoke(SetLightColorEvent(
                            Light.LightColor.BLUE))

                        reverseBrushes(BRUSH.REVERSE_TIME)
                    }

                    logM.logMd("\ntry start LazyIntake: " +
                            "${startingResult.startingResult}", Debug.TRYING)
        }   )   )
        GamepadLI.addGamepad1Listener(createClickDownListener(
            { it.left_trigger > 0.5  }, {

                    if (EventBusLI.invoke(TerminateIntakeEvent()).stoppingResult)
                    {
                        _runStatus.removeProcessFromQueue(
                            _runStatus.getActiveProcess())
                        _runStatus.clearActiveProcess()
                    }
                    EventBusLI.invoke(SetLightColorEvent(
                        Light.LightColor.BLUE))

                    reverseAndThenStartBrushesAfterTimePeriod(
                        BRUSH.REVERSE_TIME)

                    logM.logMd("\nSTOP  - INTAKE - GAMEPAD",  Debug.GAMEPAD)
                    logM.logMd("isBusy: $isUsedByAnyProcess", Debug.RACE_CONDITION)
        }   )   )


        GamepadLI.addGamepad1Listener(createClickDownListener(
            { it.left_bumper  }, {

                    val activeProcessId = _runStatus.getActiveProcess()
                    if (activeProcessId == ProcessId.DRUM_REQUEST ||
                        activeProcessId == ProcessId.SINGLE_REQUEST)
                        _runStatus.addProcessToTermination(activeProcessId)

                    EventBusLI.invoke(TerminateRequestEvent())
                    EventBusLI.invoke(SetDriveModeEvent(DriveMode.DRIVE))

                    logM.logMd("STOP  - ANY Request - GAMEPAD", Debug.GAMEPAD)
                    logM.logMd("isBusy: $isUsedByAnyProcess",   Debug.RACE_CONDITION)
        }   )   )
        GamepadLI.addGamepad1Listener(createClickDownListener(
            { it.right_bumper }, {

                    _runStatus.removeProcessFromTermination(
                        ProcessId.DRUM_REQUEST, ProcessId.SINGLE_REQUEST)

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveStreamDrumRequest())

                    logM.logMd("\nSTART - STREAM Drum request - GAMEPAD", Debug.GAMEPAD)
                    logM.logMd("isBusy: $isUsedByAnyProcess", Debug.RACE_CONDITION)
        }   )   )


        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.dpad_left  }, {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveDrumRequest(
                        TELEOP.PATTERN_SHOOTING_MODE,
                            Shooting.StockPattern.Sequence.Request.GPP))

                    logM.logMd("\nSTART - GPP Drum Request - GAMEPAD", Debug.GAMEPAD)
                    logM.logMd("isBusy: $isUsedByAnyProcess", Debug.RACE_CONDITION)
        }   )   )
        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.dpad_up    }, {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveDrumRequest(
                        TELEOP.PATTERN_SHOOTING_MODE,
                        Shooting.StockPattern.Sequence.Request.PGP))

                    logM.logMd("\nSTART - PGP Drum Request - GAMEPAD", Debug.GAMEPAD)
                    logM.logMd("isBusy: $isUsedByAnyProcess", Debug.RACE_CONDITION)
        }   )   )
        GamepadLI.addGamepad1Listener(createClickDownListener(
                { it.dpad_right }, {

                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.GREEN))
                    EventBusLI.invoke(StorageGiveDrumRequest(
                            TELEOP.PATTERN_SHOOTING_MODE,
                        Shooting.StockPattern.Sequence.Request.PPG))

                    logM.logMd("\nSTART - PPG Drum Request - GAMEPAD", Debug.GAMEPAD)
                    logM.logMd("isBusy: $isUsedByAnyProcess", Debug.RACE_CONDITION)
        }   )   )
    }
    private fun subscribeToGamepadTests()
    {
//        GamepadLI.addGamepad1Listener(createClickDownListener(
//            { it.square }, {
//
//                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.ORANGE))
//                    EventBusLI.invoke(StorageGetReadyForIntakeEvent(
//                            Ball.Name.PURPLE))
//
//                    logM.logMd("\nSTART - PURPLE Intake - GAMEPAD",  Debug.GAMEPAD)
//                    logM.logMd("isBusy: $isUsedByAnyProcess", Debug.RACE_CONDITION)
//        }   )   )
//
//        GamepadLI.addGamepad1Listener(createClickDownListener(
//            { it.circle }, {
//
//                    EventBusLI.invoke(SetLightColorEvent(Light.LightColor.ORANGE))
//                    EventBusLI.invoke(StorageGetReadyForIntakeEvent(
//                            Ball.Name.GREEN))
//
//                    logM.logMd("\nSTART - GREEN Intake - GAMEPAD", Debug.GAMEPAD)
//                    logM.logMd("isBusy: $isUsedByAnyProcess", Debug.RACE_CONDITION)
//        }   )   )

        GamepadLI.addGamepad1Listener(
            createClickDownListener(
                { it.touchpadWasPressed() }, {

                    logM.logMd("SSM: Touchpad start 100 rotation test", Debug.START)
                    _runStatus.addProcessToQueue(ProcessId.SORTING_TESTING)

                    SmartCoroutineLI.launch {
                        _storage.unsafeTestSorting()
                    }

                    _runStatus.removeProcessFromQueue(ProcessId.SORTING_TESTING)
                }
        )   )


    }
    private fun resetParametersToDefault()
    {
        _runStatus.fullResetToActiveState()
        logM.reset(Debug.SMC)

//        _shotWasFired.set(false)
    }



    private suspend fun startIntakeProcess(inputToBottomSlot: Ball.Name): IntakeResult.Name
    {
        logM.logMd("Checking race condition before intake", Debug.RACE_CONDITION)
        if (_runStatus.isUsedByAnotherProcess(
                ProcessId.INTAKE,
                ProcessId.RUNNING_INTAKE_INSTANCE))
        {
            reverseAndThenStartBrushesAfterTimePeriod(
                BRUSH.REVERSE_TIME)

            logM.logMd("Intake race condition: IS_BUSY", Debug.RACE_CONDITION)
            return Intake.FAIL_IS_BUSY
        }
        setActiveProcess(ProcessId.INTAKE)
        _runStatus.addProcessToQueue(ProcessId.RUNNING_INTAKE_INSTANCE)

        logM.logMd("Started - Intake, INPUT BALL: $inputToBottomSlot", Debug.START)
        val intakeResult = _storage.handleIntake(inputToBottomSlot)


        if (_storage.alreadyFull()) reverseBrushes(BRUSH.REVERSE_TIME)


        logM.logMd("FINISHED - INTAKE, result: $intakeResult", Debug.END)

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
        EventBusLI.invoke(
            ReverseAndThenStartBrushesAgain(reverseTime))
    }
    private suspend fun startBrushesAfterDelay(delay: Long)
    {
        delay(delay)
        logM.logMd("Going to restart brushes: " +
                "${!_storage.alreadyFull()}", Debug.LOGIC)

        if (!_storage.alreadyFull()) startBrushes()
    }



    private suspend fun startDrumRequest(
        shootingMode:    Shooting.Mode,
        requestPattern:  Array<BallRequest.Name>,
        failsafePattern: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val startingResult = handleRequestRaceCondition(ProcessId.DRUM_REQUEST)
        if (RequestResult.didFail(startingResult)) return startingResult


        logM.logMd("Started - SMART drum request", Debug.START)
        val requestResult = _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern,
                failsafePattern,
            TELEOP.INCLUDE_PREVIOUS_UNFINISHED_TO_REQUEST_ORDER,
            TELEOP.INCLUDE_PREVIOUS_UNFINISHED_TO_FAILSAFE_ORDER,
            TELEOP.AUTO_UPDATE_UNFINISHED_FOR_NEXT_PATTERN,
            TELEOP.IF_AUTO_UPDATE_UNFINISHED_USE_FAILSAFE_ORDER)


        logM.logMd("FINISHED - SMART drum request", Debug.END)
        return resumeLogicAfterShooting(requestResult, ProcessId.DRUM_REQUEST)
    }
    private suspend fun startStreamDrumRequest(): RequestResult.Name
    {
        val startingResult = handleRequestRaceCondition(ProcessId.DRUM_REQUEST)
        if (RequestResult.didFail(startingResult)) return startingResult

        if (!tryDrivingToShootingZone(ProcessId.DRUM_REQUEST))
            return resumeLogicAfterShooting(
                Request.TERMINATED, ProcessId.DRUM_REQUEST)

        logM.logMd("Started  - StreamDrum request", Debug.START)
        val requestResult = _storage.streamDrumRequest()
        logM.logMd("FINISHED - StreamDrum request", Debug.END)

        return resumeLogicAfterShooting(requestResult, ProcessId.DRUM_REQUEST)
    }
    private suspend fun startSingleRequest(
        ballRequest: BallRequest.Name): RequestResult.Name
    {
        val startingResult = handleRequestRaceCondition(ProcessId.SINGLE_REQUEST)
        if (RequestResult.didFail(startingResult)) return startingResult

        logM.logMd("Started - Single request", Debug.START)
        val requestResult = _storage.handleRequest(ballRequest)
        logM.logMd("FINISHED - Single request", Debug.END)

        return resumeLogicAfterShooting(requestResult, ProcessId.SINGLE_REQUEST)
    }



    private suspend fun tryDrivingToShootingZone(processId: Int): Boolean
    {
        logM.logMd("Attempting to drive to shooting zone: " +
                "${CONTROLS.DRIVE_TO_SHOOTING_ZONE}", Debug.TRYING)

        if (CONTROLS.DRIVE_TO_SHOOTING_ZONE)
        {
            val imDriving = EventBusLI.invoke(SetDriveModeEvent(
                DriveMode.SHOOTING)).process

            while (!imDriving.closed.get() &&
                !_runStatus.isForcedToTerminateThisProcess(processId))
                delay(Delay.MS.AWAIT.EVENTS)
        }

        logM.logMd("Finished driving, terminated: " +
                "${_runStatus.isForcedToTerminateThisProcess(processId)}", Debug.END)

        return !_runStatus.isForcedToTerminateThisProcess(processId)
    }
    private suspend fun readyUpForShooting()
    {
        logM.logMd("Starting Drivetrain rotation", Debug.START)

        var activeProcessId  = _runStatus.getActiveProcess()
        if (activeProcessId != ProcessId.DRUM_REQUEST &&
            activeProcessId != ProcessId.SINGLE_REQUEST)
            activeProcessId  = ProcessId.DRUM_REQUEST

        if (!tryDrivingToShootingZone(activeProcessId)) return
        logM.logMd("Drivetrain rotated successfully", Debug.LOGIC)

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

    private fun sendFinishedFiringEvent(requestResult: RequestResult.Name)
    {
        logM.logMd("FINISHED all firing", Debug.END)
        logM.logMd("Send finished firing EVENT", Debug.END)

        EventBusLI.invoke(FullFinishedFiringEvent(requestResult))

//        _storage.tryStartLazyIntake()
    }
    private fun resumeLogicAfterIntake()
    {
        _runStatus.removeProcessFromQueue(ProcessId.INTAKE)
        _runStatus.removeOneInstanceOfProcessFromQueue(
            ProcessId.RUNNING_INTAKE_INSTANCE)
    }
    private fun resumeLogicAfterShooting(
        requestResult:   RequestResult.Name,
        processId: Int): RequestResult.Name
    {
        tryRestartBrushes()

        _runStatus.removeProcessFromQueue(processId)
        _runStatus.removeProcessFromTermination(processId)

        _runStatus.clearActiveProcess()
        sendFinishedFiringEvent(requestResult)

        return requestResult
    }



    private suspend fun handleRequestRaceCondition(processId: Int): RequestResult.Name
    {
        logM.logMd("Checking race condition before shooting", Debug.RACE_CONDITION)
        while (isUsedByAnyProcess)
        {
            delay(Delay.MS.AWAIT.EVENTS)

            if (isDuplicateProcess(processId))
            {
                logM.logMd("Ignored duplicate shooting command", Debug.LOGIC)
                return Request.FAIL_IS_BUSY
            }

            if (tryTerminateIntake())
            {
                logM.logMd("Attempting intake termination for shooting", Debug.LOGIC)

                if (!EventBusLI.invoke(TerminateIntakeEvent()).stoppingResult)
                    return Request.FAIL_IS_BUSY

                logM.logMd("Successful intake termination", Debug.LOGIC)

                _runStatus.removeProcessFromQueue(
                    _runStatus.getActiveProcess())
                _runStatus.clearActiveProcess()

                return Request.SUCCESS
            }


            if (_runStatus.isForcedToTerminateThisProcess(processId))
                return Request.TERMINATED
        }

        logM.logMd("Initial race condition check passed",
            Debug.RACE_CONDITION)
        setActiveProcess(processId)
        return Request.SUCCESS
    }
    private fun setActiveProcess(processId: Int)
    {
        _runStatus.addProcessToQueue(processId)
        _runStatus.setActiveProcess(processId)
    }
    private fun isDuplicateProcess(processId: Int)
        = CONTROLS.IGNORE_DUPLICATE_SHOOTING_COMMAND &&
            _runStatus.isUsedByThisProcess(processId)
    private fun tryTerminateIntake()
        = CONTROLS.TRY_TERMINATE_INTAKE_WHEN_SHOOTING &&
            isIntakeActive()
    private fun isIntakeActive(): Boolean
    {
        val    activeProcess = _runStatus.getActiveProcess()
        return activeProcess == ProcessId.INTAKE ||
               activeProcess == ProcessId.LAZY_INTAKE
    }



    val isUsedByAnyProcess get() = _runStatus.isUsedByAnyProcess()



    private fun tryRestartBrushes()
    {
        if (BallCountInStorageEvent(
                NOTHING).count < MAX_BALL_COUNT)
            startBrushes()
    }
}