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

import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.GamepadLI
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.TelemetryLI
import org.woen.modules.scoringSystem.storage.Alias.SmartCoroutineLI

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

import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

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



class SortingStorage
{
    private val _storageLogic = SortingStorageLogic()



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
        EventBusLI.subscribe(
            ShotWasFiredEvent::class, {
                _storageLogic.shotWasFired()
        }   )

        EventBusLI.subscribe(
            BallCountInStorageEvent::class, {
                it.count = _storageLogic.storageCells.anyBallCount()
        }   )

        EventBusLI.subscribe(
            StorageHandleIdenticalColorsEvent::class, {

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
        EventBusLI.subscribe(
            StartLazyIntakeEvent::class, {

                val canStartLazyIntake = _storageLogic
                    .noIntakeRaceConditionProblems(LAZY_INTAKE)
                it.startingResult = canStartLazyIntake

                if (canStartLazyIntake)
                    SmartCoroutineLI.launch {
                        TelemetryLI.log("SSM Is idle: starting lazy intake")
                        startLazyIntake()
                    }

                else _storageLogic.resumeLogicAfterIntake(LAZY_INTAKE)
        }   )
        EventBusLI.subscribe(
            StopLazyIntakeEvent::class, {

                _storageLogic.lazyIntakeIsActive.set(false)
        }   )

        EventBusLI.subscribe(
            StorageUpdateAfterLazyIntakeEvent::class, {

                val canStartUpdateAfterLazyIntake = _storageLogic.canStartUpdateAfterLazyIntake()
                it.startingResult = canStartUpdateAfterLazyIntake

                if (canStartUpdateAfterLazyIntake)
                    _storageLogic.trySafeStartUpdateAfterLazyIntake(
                        it.inputFromTurretSlotToBottom)

                else _storageLogic.resumeLogicAfterIntake(UPDATE_AFTER_LAZY_INTAKE)
        }   )



        EventBusLI.subscribe(
            StorageInitiatePredictSortEvent::class, {

                val canInitiate   = _storageLogic.canInitiatePredictSort()
                it.startingResult = canInitiate

                if (canInitiate)
                    SmartCoroutineLI.launch {
                        _storageLogic.safeInitiatePredictSort(it.requestedPattern)
                    }

                else _storageLogic.runStatus
                    .safeRemoveThisProcessIdFromQueue(PREDICT_SORT)
        }   )
        EventBusLI.subscribe(
            FillStorageWithUnknownColorsEvent::class, {

                val canStartStorageCalibration = _storageLogic
                   .canStartStorageCalibrationWithCurrent()
                it.startingResult = canStartStorageCalibration

                if (canStartStorageCalibration)
                    SmartCoroutineLI.launch {
                        _storageLogic.safeStartStorageCalibrationWithCurrent()
                    }

                else _storageLogic.runStatus
                    .safeRemoveThisProcessIdFromQueue(STORAGE_CALIBRATION)
        }   )
    }
    private fun subscribeToGamepadEvents()
    {
        GamepadLI.addListener(
            createClickDownListener(
                { it.touchpadWasPressed() }, {

                    TelemetryLI.log("SSM: Touchpad start 100 rotation test")

                    SmartCoroutineLI.launch {
                        unsafeTestSorting()
                }   }
        )   )

//        GamepadLI.addListener(
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
//                        TelemetryLI.log("SSM: initiating result: $canInitiate")
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
            TelemetryLI.log("SSM Init settings: USE SECOND DRIVER")
        }
        else TelemetryLI.log("SSM Init settings: DON'T use second driver")
    }





    fun terminateRequest()
    {
        TelemetryLI.log("attempting request termination")
        val activeRequestProcessId = _storageLogic.runStatus.getCurrentActiveProcess()

        if (activeRequestProcessId == SINGLE_REQUEST ||
            activeRequestProcessId == DRUM_REQUEST)
        {
            TelemetryLI.log("\n\tTerminating all requests\n")

            _storageLogic.runStatus.addProcessToTerminationList(activeRequestProcessId)
        }
    }
    suspend fun terminateIntake()
    {
        TelemetryLI.log("attempting intake termination")

        val activeProcessId = _storageLogic.runStatus.getCurrentActiveProcess()
        _storageLogic.lazyIntakeIsActive.set(false)

        if (activeProcessId == LAZY_INTAKE &&
            activeProcessId == INTAKE)
        {
            TelemetryLI.log("\n\tTerminating all intakes")

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

        SmartCoroutineLI.launch {
            var iteration = 0
            while (iteration < 100)
            {
                TelemetryLI.log("\nIteration: $iteration")

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
        TelemetryLI.log("Started LazyIntake")
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

        TelemetryLI.log("Stopped LazyIntake")
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

            TelemetryLI.log("SSM - searching for intake slot")
            val storageCanHandle = _storageLogic.storageCells.handleIntake()
            TelemetryLI.log("SSM - DONE Searching, result: " + storageCanHandle.name())

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

        val requestResult = _storageLogic.storageCells.handleRequest(request)
        TelemetryLI.log("FINISHED searching, result: ${requestResult.name()}")


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
        TelemetryLI.log("MODE: LAZY SHOOT EVERYTHING")
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
        TelemetryLI.log("MODE: SHOOT EVERYTHING")
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