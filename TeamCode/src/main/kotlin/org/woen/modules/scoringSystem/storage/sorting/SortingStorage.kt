package org.woen.modules.scoringSystem.storage.sorting


import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult
import org.woen.enumerators.Shooting

import org.woen.hotRun.HotRun
import org.woen.modules.camera.OnPatternDetectedEvent

import org.woen.modules.light.Light.LightColor
import org.woen.modules.light.SetLightColorEvent
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.brush.SwitchBrushStateEvent

import org.woen.modules.scoringSystem.storage.Alias.Delay
import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT
//import org.woen.modules.scoringSystem.storage.Alias.GamepadLI
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.SmartCoroutineLI

import org.woen.modules.scoringSystem.turret.CurrentlyShooting
import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent
import org.woen.modules.scoringSystem.storage.FillStorageWithUnknownColorsEvent

import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StopLazyIntakeEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent

import org.woen.modules.scoringSystem.storage.StorageInitiatePredictSortEvent
import org.woen.modules.scoringSystem.storage.StorageFinishedPredictSortEvent

import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent
import org.woen.modules.scoringSystem.storage.StorageUpdateAfterLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.WaitForTerminateIntakeEvent

//import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.telemetry.LogManager
import org.woen.telemetry.Configs.DELAY

import org.woen.telemetry.Configs.DEBUG_LEVELS.ATTEMPTING_LOGIC
import org.woen.telemetry.Configs.DEBUG_LEVELS.GAMEPAD_FEEDBACK
//import org.woen.telemetry.Configs.DEBUG_LEVELS.GENERIC_INFO
import org.woen.telemetry.Configs.DEBUG_LEVELS.LOGIC_STEPS
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_ENDING
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_NAME
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_STARTING
import org.woen.telemetry.Configs.DEBUG_LEVELS.SSM_DEBUG_LEVELS
import org.woen.telemetry.Configs.DEBUG_LEVELS.SSM_DEBUG_SETTING

import org.woen.telemetry.Configs.PROCESS_ID.INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.LAZY_INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.UPDATE_AFTER_LAZY_INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.DRUM_REQUEST
import org.woen.telemetry.Configs.PROCESS_ID.SINGLE_REQUEST
import org.woen.telemetry.Configs.PROCESS_ID.PREDICT_SORT
import org.woen.telemetry.Configs.PROCESS_ID.STORAGE_CALIBRATION

import org.woen.telemetry.Configs.SORTING_SETTINGS.USE_LAZY_VERSION_OF_STREAM_REQUEST
import org.woen.telemetry.Configs.SORTING_SETTINGS.USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION



class SortingStorage
{
    private val _storageLogic = SortingStorageLogic()
    val logM = LogManager(SSM_DEBUG_SETTING, SSM_DEBUG_LEVELS, "SSM")



    constructor()
    {
        subscribeToInfoEvents()
        subscribeToActionEvents()
        subscribeToGamepadEvents()
        subscribeToTerminateEvents()
        subscribeToSecondDriverPatternRecalibration()

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            SmartCoroutineLI.launch {
                terminateIntake()
                terminateRequest()

                _storageLogic.storageCells.hwSortingM.resetParametersAndLogicToDefault()
                _storageLogic.storageCells.resetParametersToDefault()
                _storageLogic.resetParametersToDefault()
            }
        }
    }

    private fun subscribeToInfoEvents()
    {
        EventBusLI.subscribe(ShotWasFiredEvent::class, {
                _storageLogic.shotWasFired.set(true)
        }   )

        EventBusLI.subscribe(CurrentlyShooting::class, {
                _storageLogic.canShoot.set(true)
        }   )

        EventBusLI.subscribe(BallCountInStorageEvent::class, {
                it.count = _storageLogic.storageCells.anyBallCount()
        }   )

        EventBusLI.subscribe(StorageHandleIdenticalColorsEvent::class, {

                val result = _storageLogic.storageCells.handleIdenticalColorRequest()

                it.maxIdenticalColorCount = result.maxIdenticalColorCount
                it.identicalColor = result.identicalColor
        }   )


        EventBusLI.subscribe(OnPatternDetectedEvent::class, {

                _storageLogic.dynamicMemoryPattern.setPermanent(it.pattern.subsequence)
        }   )
    }
    private fun subscribeToActionEvents()
    {
        EventBusLI.subscribe(StartLazyIntakeEvent::class, {

                logM.logMd("check race condition", ATTEMPTING_LOGIC)

                val canStartLazyIntake = _storageLogic
                    .noIntakeRaceConditionProblems(LAZY_INTAKE)
                it.startingResult = canStartLazyIntake

                if (canStartLazyIntake)
                    SmartCoroutineLI.launch {
                        logM.logMd("IS IDLE = starting lazy intake",
                            PROCESS_STARTING)

                        EventBusLI.invoke(SwitchBrushStateEvent(
                            Brush.BrushState.FORWARD))

                        startLazyIntake()
                    }

                else _storageLogic.resumeLogicAfterIntake(LAZY_INTAKE)
        }   )
        EventBusLI.subscribe(StopLazyIntakeEvent::class, {

                EventBusLI.invoke(SetLightColorEvent(LightColor.BLUE))
                _storageLogic.lazyIntakeIsActive.set(false)
        }   )

        EventBusLI.subscribe(StorageUpdateAfterLazyIntakeEvent::class, {

                val canStartUpdate = _storageLogic.canStartUpdateAfterLazyIntake()
                it.startingResult = canStartUpdate

                if (canStartUpdate)
                {
                    logM.logMd("IS IDLE = updating after lazy intake",
                        PROCESS_STARTING)
                    _storageLogic.safeUpdateAfterLazyIntake(
                        it.inputFromTurretSlotToBottom)
                }
                else _storageLogic.resumeLogicAfterIntake(UPDATE_AFTER_LAZY_INTAKE)
        }   )



        EventBusLI.subscribe(StorageInitiatePredictSortEvent::class, {

                val canInitiate   = _storageLogic.canInitiatePredictSort()
                it.startingResult = canInitiate

                if (canInitiate)
                    SmartCoroutineLI.launch {
                        logM.logMd("IS IDLE = starting predict sort",
                            PROCESS_STARTING)
                        _storageLogic.safeInitiatePredictSort(it.requestedPattern)
                    }
                else _storageLogic.runStatus
                    .safeRemoveThisProcessIdFromQueue(PREDICT_SORT)

            EventBusLI.invoke(StorageFinishedPredictSortEvent())
        }   )
        EventBusLI.subscribe(FillStorageWithUnknownColorsEvent::class, {

                val canStartStorageCalibration = _storageLogic
                   .canStartStorageCalibrationWithCurrent()
                it.startingResult = canStartStorageCalibration

                if (canStartStorageCalibration)
                    SmartCoroutineLI.launch {
                        _storageLogic.safeStartStorageCalibrationWithCurrent()
                    }.join()

                else _storageLogic.runStatus
                    .safeRemoveThisProcessIdFromQueue(STORAGE_CALIBRATION)
        }   )
    }
    private fun subscribeToGamepadEvents()
    {
//        GamepadLI.addGamepad1Listener(
//            createClickDownListener(
//                { it.touchpadWasPressed() }, {
//
//                    logM.logMd("SSM: Touchpad start 100 rotation test", PROCESS_STARTING)
//
//                    SmartCoroutineLI.launch {
//                        unsafeTestSorting()
//                }   }
//        )   )
//
//        GamepadLI.addGamepad1Listener(
//            createClickDownListener({ it.ps }, {
//
//                    SmartCoroutineLI.launch {
//
//                        val pattern = arrayOf(
//                            BallRequest.Name.PREFER_GREEN,
//                            BallRequest.Name.PREFER_PURPLE,
//                            BallRequest.Name.PREFER_PURPLE)
//
//                        val canInitiate = _storageLogic.canInitiatePredictSort()
//                        logM.logMd("SSM: initiating result: $canInitiate",
//                            PROCESS_STARTING)
//
//                        if (canInitiate)
//                            _storageLogic.safeInitiatePredictSort(pattern)
//                    }
//        }   )   )
    }
    private fun subscribeToTerminateEvents()
    {
        EventBusLI.subscribe(TerminateIntakeEvent::class, {

                terminateIntake()
        }   )
        EventBusLI.subscribe(WaitForTerminateIntakeEvent::class, {

                terminateIntake()
        }   )

        EventBusLI.subscribe(TerminateRequestEvent::class, {

                terminateRequest()
        }   )
    }
    private fun subscribeToSecondDriverPatternRecalibration()
    {
        if (USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION)
        {
//            GamepadLI.addGamepad2Listener(
//                createClickDownListener({ it.triangle }, {
//
//                        _storageLogic.dynamicMemoryPattern.resetTemporary()
//            }   )   )
//
//            GamepadLI.addGamepad2Listener(
//                createClickDownListener({ it.square }, {
//
//                        _storageLogic.dynamicMemoryPattern.addToTemporary()
//            }   )   )
//
//            GamepadLI.addGamepad2Listener(
//                createClickDownListener({ it.circle }, {
//
//                        _storageLogic.dynamicMemoryPattern.removeFromTemporary()
//            }   )   )
            logM.logMd("Init settings: USE SECOND DRIVER", GAMEPAD_FEEDBACK)
        }
        else logM.logMd("Init settings: DON'T use second driver", GAMEPAD_FEEDBACK)
    }





    private fun terminateRequest()
    {
        logM.logMd("attempting request termination", ATTEMPTING_LOGIC)
        val activeRequestProcessId = _storageLogic.runStatus.getCurrentActiveProcess()

        if (activeRequestProcessId == SINGLE_REQUEST ||
            activeRequestProcessId == DRUM_REQUEST)
        {
            logM.logMd("\n\tTerminating all requests\n", LOGIC_STEPS)

            _storageLogic.runStatus.addProcessToTerminationList(activeRequestProcessId)
        }
    }
    private suspend fun terminateIntake()
    {
        logM.logMd("attempting intake termination", ATTEMPTING_LOGIC)

        val activeProcessId = _storageLogic.runStatus.getCurrentActiveProcess()
        _storageLogic.lazyIntakeIsActive.set(false)

        if (activeProcessId == LAZY_INTAKE &&
            activeProcessId == INTAKE)
        {
            logM.logMd("\n\tTerminating all intakes", LOGIC_STEPS)

            if (!_storageLogic.pleaseWaitForIntakeEnd.get())
                _storageLogic.runStatus
                    .addProcessToTerminationList(activeProcessId)

            while (_storageLogic.runStatus
                    .isUsedByThisProcess(activeProcessId))
                delay(DELAY.EVENT_AWAITING_MS)
        }
    }

//    fun unsafeTestSorting()
//    {
//        val fill = arrayOf(Ball.Name.PURPLE, Ball.Name.GREEN, Ball.Name.PURPLE)
//        _storageLogic.storageCells.safeUpdateAfterLazyIntake(fill)
//
//        SmartCoroutineLI.launch {
//            var iteration = 0
//            while (iteration < 100)
//            {
//                logM.logMd("\nIteration: $iteration", GENERIC_INFO)
//
//                _storageLogic.storageCells.fullRotate()
//                iteration++
//        }   }
//    }
//    suspend fun hwSmartPushNextBall()
//        = _storageLogic.storageCells.hwSortingM.smartPushNextBall()
    fun alreadyFull() = _storageLogic.storageCells.alreadyFull()



    private suspend fun startLazyIntake()
    {
        logM.logMd("Started LazyIntake", PROCESS_STARTING)

        EventBusLI.invoke(SetLightColorEvent(LightColor.ORANGE))

        _storageLogic.runStatus.setCurrentActiveProcess(LAZY_INTAKE)
        _storageLogic.lazyIntakeIsActive.set(true)

        _storageLogic.storageCells.hwSortingM.slowStartBelts()

        while (_storageLogic.lazyIntakeIsActive.get())
        {
            delay(DELAY.EVENT_AWAITING_MS)

            if (_storageLogic.isForcedToTerminate(LAZY_INTAKE))
                _storageLogic.terminateIntake(LAZY_INTAKE)
        }

        _storageLogic.storageCells.hwSortingM.stopBelts()
        _storageLogic.lazyIntakeIsActive.set(false)

        logM.logMd("Stopped LazyIntake", PROCESS_ENDING)
        _storageLogic.resumeLogicAfterIntake(LAZY_INTAKE)
    }



    suspend fun handleIntake(inputBall: Ball.Name): IntakeResult.Name
    {
        if (_storageLogic.storageCells.alreadyFull()) return Intake.FAIL_IS_FULL

        if (_storageLogic.noIntakeRaceConditionProblems(INTAKE))
        {
            _storageLogic.runStatus.setCurrentActiveProcess(INTAKE)

            if (_storageLogic.isForcedToTerminate(INTAKE))
                return _storageLogic.terminateIntake(INTAKE)

            val intakeResult = _storageLogic.safeSortIntake(inputBall)

            if (_storageLogic.runningIntakeInstances.get() == 0)
                _storageLogic.resumeLogicAfterIntake(INTAKE)
            return intakeResult
        }

        if (_storageLogic.runningIntakeInstances.get() == 0)
            _storageLogic.resumeLogicAfterIntake(INTAKE)

        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    suspend fun handleRequest(request: BallRequest.Name):  RequestResult.Name
    {
        if (_storageLogic.storageCells.isEmpty()) return Request.FAIL_IS_EMPTY
        if (_storageLogic.cantHandleRequestRaceCondition(SINGLE_REQUEST))
            return _storageLogic.terminateRequest(SINGLE_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(SINGLE_REQUEST)

        logM.logMd("searching for request slot", LOGIC_STEPS)
        val requestResult = _storageLogic.storageCells.handleRequest(request)
        logM.logMd("FINISHED searching, result: ${requestResult.name()}", PROCESS_ENDING)


        val shootingResult = _storageLogic.shootRequestFinalPhase(
            requestResult, SINGLE_REQUEST)

        if (shootingResult == Request.TERMINATED)
            return _storageLogic.terminateRequest(SINGLE_REQUEST)

        _storageLogic.resumeLogicAfterRequest(DRUM_REQUEST)
        return shootingResult
    }



    suspend fun streamDrumRequest():             RequestResult.Name
    {
        return if (USE_LAZY_VERSION_OF_STREAM_REQUEST)
             lazyStreamDrumRequest()
        else fastStreamDrumRequest()
    }
    private suspend fun lazyStreamDrumRequest(): RequestResult.Name
    {
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)
        logM.logMd("MODE: LAZY StreamDrum request", PROCESS_NAME)

        _storageLogic.lazyStreamDrumRequest(MAX_BALL_COUNT)

        _storageLogic.resumeLogicAfterRequest(DRUM_REQUEST, false)
        return Request.SUCCESS_NOW_EMPTY
    }
    private suspend fun fastStreamDrumRequest(): RequestResult.Name
    {
        if (_storageLogic.storageCells.isEmpty()) return Request.FAIL_IS_EMPTY
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)
        logM.logMd("MODE: SMART StreamDrum request", PROCESS_NAME)

        _storageLogic.fastStreamDrumRequest()

        _storageLogic.resumeLogicAfterRequest(DRUM_REQUEST, false)
        return Request.SUCCESS_NOW_EMPTY
    }



    suspend fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        includePreviousUnfinishedToRequest: Boolean = true,
        autoUpdateUnfinishedForNextPattern: Boolean = true):       RequestResult.Name
    {
        if (_storageLogic.storageCells.isEmpty()) return Request.FAIL_IS_EMPTY
        if (requestOrder.isEmpty())  return Request.ILLEGAL_ARGUMENT
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)


        val  standardPatternOrder = if (!includePreviousUnfinishedToRequest) requestOrder
        else DynamicPattern.trimPattern(
            _storageLogic.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)

        if (autoUpdateUnfinishedForNextPattern)
            _storageLogic.dynamicMemoryPattern.setTemporary(standardPatternOrder)

        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> _storageLogic.fastStreamDrumRequest()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> _storageLogic.shootDrumCanSkip(
                    standardPatternOrder,
                    autoUpdateUnfinishedForNextPattern)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> _storageLogic.shootDrumUntilPatternBreaks(
                    standardPatternOrder,
                        autoUpdateUnfinishedForNextPattern)

                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
                    -> _storageLogic.shootDrumRequestEntireIsValid(standardPatternOrder)
            }

        if  (Request.wasTerminated(requestResult))
             _storageLogic.terminateRequest(DRUM_REQUEST)
        else
        {
            delay(Delay.HALF_PUSH)
            _storageLogic.resumeLogicAfterRequest(
                DRUM_REQUEST,
                _storageLogic.storageCells.isNotEmpty())
        }
        return requestResult
    }
    suspend fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>? = requestOrder,
        includePreviousUnfinishedToRequest:       Boolean = true,
        includePreviousUnfinishedToFailsafe:      Boolean = true,
        autoUpdateUnfinishedForNextPattern:       Boolean = true,
        ifAutoUpdatingUnfinishedUseFailsafeOrder: Boolean = true): RequestResult.Name
    {
        if (failsafeOrder == null || failsafeOrder.isEmpty() ||
            failsafeOrder.contentEquals(requestOrder))
            return shootEntireDrumRequest(shootingMode, requestOrder, includePreviousUnfinishedToRequest)

        if (_storageLogic.storageCells.isEmpty()) return Request.FAIL_IS_EMPTY
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)


        val  standardPatternOrder = if (!includePreviousUnfinishedToRequest) requestOrder
        else DynamicPattern.trimPattern(
            _storageLogic.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)

        val  failsafePatternOrder = if (!includePreviousUnfinishedToFailsafe) requestOrder
        else DynamicPattern.trimPattern(
            _storageLogic.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)


        val autoUpdateUnfinishedWithFailsafe =
                autoUpdateUnfinishedForNextPattern &&
                ifAutoUpdatingUnfinishedUseFailsafeOrder
        if (autoUpdateUnfinishedForNextPattern)
            _storageLogic.dynamicMemoryPattern.setTemporary(standardPatternOrder)


        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> _storageLogic.fastStreamDrumRequest()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> _storageLogic.shootDrumCanSkip(
                    standardPatternOrder,
                    failsafePatternOrder,
                    autoUpdateUnfinishedForNextPattern,
                    autoUpdateUnfinishedWithFailsafe)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> _storageLogic.shootDrumUntilPatternBreaks(
                    standardPatternOrder,
                    failsafePatternOrder,
                    autoUpdateUnfinishedWithFailsafe)

                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
                    -> _storageLogic.shootDrumRequestEntireIsValid(
                    standardPatternOrder,
                    failsafePatternOrder,
                    autoUpdateUnfinishedWithFailsafe)
            }

        if  (Request.wasTerminated(requestResult))
             _storageLogic.terminateRequest(DRUM_REQUEST)
        else
        {
            delay(Delay.HALF_PUSH)
            _storageLogic.resumeLogicAfterRequest(
                DRUM_REQUEST,
                _storageLogic.storageCells.isNotEmpty())
        }
        return requestResult
    }
}