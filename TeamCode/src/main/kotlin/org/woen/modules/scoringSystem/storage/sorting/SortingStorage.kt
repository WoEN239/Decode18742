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
import org.woen.modules.light.Light
import org.woen.modules.light.SetLightColorEvent

import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request

import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent
import org.woen.modules.scoringSystem.storage.FillStorageWithUnknownColorsEvent

import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StopLazyIntakeEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent

import org.woen.modules.scoringSystem.storage.StorageInitiatePredictSortEvent
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent
import org.woen.modules.scoringSystem.storage.StorageUpdateAfterLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.WaitForTerminateIntakeEvent
import org.woen.telemetry.Configs.DEBUG_LEVELS.ATTEMPTING_LOGIC
import org.woen.telemetry.Configs.DEBUG_LEVELS.GAMEPAD_FEEDBACK
import org.woen.telemetry.Configs.DEBUG_LEVELS.GENERIC_INFO
import org.woen.telemetry.Configs.DEBUG_LEVELS.LOGIC_STEPS
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_ENDING
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_NAME
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_STARTING
import org.woen.telemetry.Configs.DEBUG_LEVELS.SSM_DEBUG_LEVELS
import org.woen.telemetry.Configs.DEBUG_LEVELS.SSM_DEBUG_SETTING

import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
//import org.woen.threading.ThreadedGamepad
//import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.PROCESS_ID.INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.LAZY_INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.UPDATE_AFTER_LAZY_INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.DRUM_REQUEST
import org.woen.telemetry.Configs.PROCESS_ID.SINGLE_REQUEST
import org.woen.telemetry.Configs.PROCESS_ID.PREDICT_SORT
import org.woen.telemetry.Configs.PROCESS_ID.STORAGE_CALIBRATION

import org.woen.telemetry.Configs.SORTING_SETTINGS.USE_LAZY_VERSION_OF_STREAM_REQUEST
import org.woen.telemetry.Configs.SORTING_SETTINGS.USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION
import org.woen.telemetry.LogManager


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
            ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
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
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            ShotWasFiredEvent::class, {
                _storageLogic.shotWasFired()
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            BallCountInStorageEvent::class, {
                it.count = _storageLogic.storageCells.anyBallCount()
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageHandleIdenticalColorsEvent::class, {

                val result = _storageLogic.storageCells.handleIdenticalColorRequest()

                it.maxIdenticalColorCount = result.maxIdenticalColorCount
                it.identicalColor = result.identicalColor
        }   )


        ThreadedEventBus.LAZY_INSTANCE.subscribe(OnPatternDetectedEvent::class, {

                _storageLogic.dynamicMemoryPattern.setPermanent(it.pattern.subsequence)
        }   )
    }
    private fun subscribeToActionEvents()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StartLazyIntakeEvent::class, {

                logM.logMd("check race condition", ATTEMPTING_LOGIC)
                val canStartLazyIntake = _storageLogic
                    .noIntakeRaceConditionProblems(LAZY_INTAKE)
                it.startingResult = canStartLazyIntake

                if (canStartLazyIntake)
                    ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                        logM.logMd("IS IDLE = starting lazy intake", PROCESS_STARTING)
                        startLazyIntake()
                    }

                else _storageLogic.resumeLogicAfterIntake(LAZY_INTAKE)
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StopLazyIntakeEvent::class, {

                _storageLogic.lazyIntakeIsActive.set(false)
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageUpdateAfterLazyIntakeEvent::class, {

                val canStartUpdateAfterLazyIntake = _storageLogic.canStartUpdateAfterLazyIntake()
                it.startingResult = canStartUpdateAfterLazyIntake

                if (canStartUpdateAfterLazyIntake)
                    _storageLogic.trySafeStartUpdateAfterLazyIntake(
                        it.inputFromTurretSlotToBottom)

                else _storageLogic.resumeLogicAfterIntake(UPDATE_AFTER_LAZY_INTAKE)
        }   )



        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageInitiatePredictSortEvent::class, {

                val canInitiate   = _storageLogic.canInitiatePredictSort()
                it.startingResult = canInitiate

                if (canInitiate)
                    ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                        _storageLogic.safeInitiatePredictSort(it.requestedPattern)
                    }

                else _storageLogic.runStatus
                    .safeRemoveThisProcessIdFromQueue(PREDICT_SORT)
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            FillStorageWithUnknownColorsEvent::class, {

                val canStartStorageCalibration = _storageLogic
                   .canStartStorageCalibrationWithCurrent()
                it.startingResult = canStartStorageCalibration

                if (canStartStorageCalibration)
                    ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                        _storageLogic.safeStartStorageCalibrationWithCurrent()
                    }

                else _storageLogic.runStatus
                    .safeRemoveThisProcessIdFromQueue(STORAGE_CALIBRATION)
        }   )
    }
    private fun subscribeToGamepadEvents()
    {
//        ThreadedGamepad.LAZY_INSTANCE.addListener(
//            createClickDownListener(
//                { it.touchpadWasPressed() }, {
//
//                    logM.logMd("SSM: Touchpad start 100 rotation test")
//
//                    ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
//                        unsafeTestSorting()
//                }   }
//        )   )
//
//        GamepadLI.addListener(
//            createClickDownListener({ it.ps }, {
//
//                     ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
//
//                        val pattern = arrayOf(
//                            BallRequest.Name.PREFER_GREEN,
//                            BallRequest.Name.PREFER_PURPLE,
//                            BallRequest.Name.PREFER_PURPLE)
//
//                        val canInitiate = _storageLogic.canInitiatePredictSort()
//                        logM.logMd("SSM: initiating result: $canInitiate")
//
//                        if (canInitiate)
//                            _storageLogic.safeInitiatePredictSort(pattern)
//                    }
//        }   )   )
    }
    private fun subscribeToTerminateEvents()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateIntakeEvent::class, {

                terminateIntake()
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(WaitForTerminateIntakeEvent::class, {

                terminateIntake()
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateRequestEvent::class, {

                terminateRequest()
        }   )
    }
    private fun subscribeToSecondDriverPatternRecalibration()
    {
        if (USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION)
        {
//            GamepadLI.addListener(
//            createClickDownListener({ it.triangle }, {
//
//                        _storageLogic.dynamicMemoryPattern.resetTemporary()
//            }   )   )
//
//            GamepadLI.addListener(
//            createClickDownListener({ it.square },   {
//
//                        _storageLogic.dynamicMemoryPattern.addToTemporary()
//            }   )   )
//
//            GamepadLI.addListener(
//            createClickDownListener({ it.circle },   {
//
//                        _storageLogic.dynamicMemoryPattern.removeFromTemporary()
//            }   )   )
            logM.logMd("Init settings: USE SECOND DRIVER", GAMEPAD_FEEDBACK)
        }
        else logM.logMd("Init settings: DON'T use second driver", GAMEPAD_FEEDBACK)
    }





    fun terminateRequest()
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
    suspend fun terminateIntake()
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

    fun unsafeTestSorting()
    {
        val fill = arrayOf(Ball.Name.PURPLE, Ball.Name.GREEN, Ball.Name.PURPLE)
        _storageLogic.storageCells.safeUpdateAfterLazyIntake(fill)

        ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            var iteration = 0
            while (iteration < 100)
            {
                logM.logMd("\nIteration: $iteration", GENERIC_INFO)

                _storageLogic.storageCells.fullRotate()
                iteration++
        }   }
    }
    suspend fun hwSmartPushNextBall()
        = _storageLogic.storageCells.hwSortingM.hwSmartPushNextBall()
    fun alreadyFull()  = _storageLogic.storageCells.alreadyFull()



    suspend fun tryStartLazyIntake()
    {
        if (!_storageLogic.storageCells.alreadyFull()
            && _storageLogic.noIntakeRaceConditionProblems(LAZY_INTAKE))
            startLazyIntake()
        else _storageLogic.resumeLogicAfterIntake(LAZY_INTAKE)
    }
    private suspend fun startLazyIntake()
    {
        logM.logMd("Started LazyIntake", PROCESS_STARTING)
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SetLightColorEvent(Light.LightColor.ORANGE))
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



    suspend fun handleIntake(inputToBottomSlot: Ball.Name): IntakeResult.Name
    {
        if (_storageLogic.storageCells.alreadyFull()) return Intake.FAIL_IS_FULL

        if (_storageLogic.noIntakeRaceConditionProblems(INTAKE))
        {
            _storageLogic.runStatus.setCurrentActiveProcess(INTAKE)

            if (_storageLogic.isForcedToTerminate(INTAKE))
                return _storageLogic.terminateIntake(INTAKE)

            logM.logMd("searching for intake slot", LOGIC_STEPS)
            val storageCanHandle = _storageLogic.storageCells.handleIntake()
            logM.logMd("DONE Searching, result: ${storageCanHandle.name()}", PROCESS_ENDING)

            val intakeResult = _storageLogic.safeSortIntake(
                storageCanHandle, inputToBottomSlot)

            _storageLogic.resumeLogicAfterIntake(INTAKE)
            return intakeResult
        }

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



    suspend fun streamDrumRequest():              RequestResult.Name
    {
        return if (USE_LAZY_VERSION_OF_STREAM_REQUEST)
             lazyDrumRequest()
        else shootEntireDrumRequest()
    }
    private suspend fun lazyDrumRequest():        RequestResult.Name
    {
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)
        logM.logMd("MODE: LAZY SHOOT EVERYTHING", PROCESS_NAME)
        val requestResult = _storageLogic.lazyShootEverything()

        _storageLogic.resumeLogicAfterRequest(DRUM_REQUEST, false)
        return requestResult
    }
    private suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        if (_storageLogic.storageCells.isEmpty()) return Request.FAIL_IS_EMPTY
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)
        logM.logMd("MODE: SHOOT EVERYTHING", PROCESS_NAME)
        val requestResult = _storageLogic.shootEverything()

        _storageLogic.resumeLogicAfterRequest(DRUM_REQUEST, false)
        return requestResult
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
                    -> _storageLogic.shootEverything()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> _storageLogic.shootEntireCanSkip(
                    standardPatternOrder,
                    autoUpdateUnfinishedForNextPattern)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> _storageLogic.shootEntireUntilPatternBreaks(
                    standardPatternOrder,
                        autoUpdateUnfinishedForNextPattern)

                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
                    -> _storageLogic.shootEntireRequestIsValid(standardPatternOrder)
            }

        if  (Request.wasTerminated(requestResult))
             _storageLogic.terminateRequest(DRUM_REQUEST)
        else _storageLogic.resumeLogicAfterRequest(
            DRUM_REQUEST,
            _storageLogic.storageCells.isNotEmpty())
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
                    -> _storageLogic.shootEverything()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> _storageLogic.shootEntireCanSkip(
                    standardPatternOrder,
                    failsafePatternOrder,
                    autoUpdateUnfinishedForNextPattern,
                    autoUpdateUnfinishedWithFailsafe)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> _storageLogic.shootEntireUntilPatternBreaks(
                    standardPatternOrder,
                    failsafePatternOrder,
                    autoUpdateUnfinishedWithFailsafe)

                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
                    -> _storageLogic.shootEntireRequestIsValid(
                    standardPatternOrder,
                    failsafePatternOrder,
                    autoUpdateUnfinishedWithFailsafe)
            }

        if  (Request.wasTerminated(requestResult))
            _storageLogic.terminateRequest(DRUM_REQUEST)
        else _storageLogic.resumeLogicAfterRequest(
            DRUM_REQUEST,
            _storageLogic.storageCells.isNotEmpty())
        return requestResult
    }
}