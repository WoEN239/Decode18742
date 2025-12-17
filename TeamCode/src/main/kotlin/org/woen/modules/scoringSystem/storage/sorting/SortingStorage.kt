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

import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent

import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StopLazyIntakeEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent

import org.woen.modules.scoringSystem.storage.StorageInitiatePredictSortEvent
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent
import org.woen.modules.scoringSystem.storage.StorageUpdateAfterLazyIntakeEvent

import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.GENERIC.NOTHING
import org.woen.telemetry.Configs.GENERIC.MAX_BALL_COUNT

import org.woen.telemetry.Configs.PROCESS_ID.INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.LAZY_INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.DRUM_REQUEST
import org.woen.telemetry.Configs.PROCESS_ID.SINGLE_REQUEST

import org.woen.telemetry.Configs.SORTING_SETTINGS.USE_LAZY_VERSION_OF_STREAM_REQUEST



class SortingStorage
{
    private val _storageLogic = SortingStorageLogic()
    


    constructor()
    {
        subscribeToInfoEvents()
        subscribeToActionEvents()
        subscribeToGamepadEvents()
        subscribeToTerminateEvents()

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _storageLogic.storageCells.hwSortingM.resetParametersAndLogicToDefault()
            _storageLogic.storageCells.resetParametersToDefault()
            _storageLogic.resetParametersToDefault()
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



        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageUpdateAfterLazyIntakeEvent::class, {

                _storageLogic.storageCells.updateAfterLazyIntake(
                    it.inputFromTurretSlotToBottom)
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(OnPatternDetectedEvent::class, {

                _storageLogic.dynamicMemoryPattern.setPermanent(it.pattern.subsequence)
        }   )
    }
    private fun subscribeToActionEvents()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StartLazyIntakeEvent::class, {

                val tryToStartLazyIntake = _storageLogic.canStartIntakeIsNotBusy()
                it.startingResult = tryToStartLazyIntake

                if (tryToStartLazyIntake != IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY)
                    ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                        startLazyIntake()
                    }
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StopLazyIntakeEvent::class, {

                _storageLogic.lazyIntakeIsActive.set(false)
        }   )


        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageInitiatePredictSortEvent::class, {

                val canInitiate   = _storageLogic.canInitiatePredictSort()
                it.startingResult = canInitiate

                if (canInitiate)
                    ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                        _storageLogic.safeInitiatePredictSort(it.requestedPattern)
                    }
            }
        )
    }
    private fun subscribeToGamepadEvents()
    {
        ThreadedGamepad.LAZY_INSTANCE.addListener(
        createClickDownListener({ it.triangle }, {

                    _storageLogic.dynamicMemoryPattern.resetTemporary()
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
        createClickDownListener({ it.square },   {

                    _storageLogic.dynamicMemoryPattern.addToTemporary()
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
        createClickDownListener({ it.circle },   {

                    _storageLogic.dynamicMemoryPattern.removeFromTemporary()
        }   )   )



        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener(
                { it.touchpadWasPressed() }, {

                    ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                        var iteration = NOTHING
                        while (iteration < 100)
                        {
                            _storageLogic.storageCells.fullRotate()
                            iteration++
                    }   }
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener({ it.ps }, {

                    ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {

                        val pattern = arrayOf(BallRequest.Name.PURPLE, BallRequest.Name.GREEN, BallRequest.Name.PURPLE)

                        val canInitiate = _storageLogic.canInitiatePredictSort()
                        ThreadedTelemetry.LAZY_INSTANCE.log("SSM: initiating result: $canInitiate")

                        if (canInitiate)
                            _storageLogic.safeInitiatePredictSort(pattern)
                    }
        }   )   )
    }
    private fun subscribeToTerminateEvents()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateIntakeEvent::class, {

            val activeIntakeProcessId = _storageLogic.runStatus.getCurrentActiveProcess()
            ThreadedTelemetry.LAZY_INSTANCE.log("SSM Terminate intake process id: $activeIntakeProcessId")

            if (activeIntakeProcessId == INTAKE ||
                activeIntakeProcessId == LAZY_INTAKE)
            {
                _storageLogic.runStatus.addProcessToTerminationList(activeIntakeProcessId)
                if (activeIntakeProcessId == LAZY_INTAKE)
                    _storageLogic.lazyIntakeIsActive.set(false)
            }
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateRequestEvent::class, {

            val activeRequestProcessId = _storageLogic.runStatus.getCurrentActiveProcess()

            if (activeRequestProcessId == SINGLE_REQUEST ||
                activeRequestProcessId == DRUM_REQUEST)
                _storageLogic.runStatus.addProcessToTerminationList(activeRequestProcessId)
        }   )
    }





    fun hwStartBelts() = _storageLogic.storageCells.hwSortingM.startBelts()
    fun alreadyFull()  = _storageLogic.storageCells.alreadyFull()



    suspend fun tryStartLazyIntake()
    {
        if (!_storageLogic.storageCells.alreadyFull()
            && _storageLogic.canStartIntakeIsNotBusy() != IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY)
            startLazyIntake()
    }
    suspend fun startLazyIntake()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("Started LazyIntake")
        _storageLogic.runStatus.setCurrentActiveProcess(LAZY_INTAKE)
        _storageLogic.lazyIntakeIsActive.set(true)

        _storageLogic.storageCells.hwSortingM.slowStartBelts()
        while (_storageLogic.lazyIntakeIsActive.get())
        {
            delay(DELAY.EVENT_AWAITING_MS)

            if (_storageLogic.isForcedToTerminateIntake(LAZY_INTAKE))
                _storageLogic.terminateIntake(LAZY_INTAKE)
        }
        _storageLogic.storageCells.hwSortingM.stopBelts()
        _storageLogic.lazyIntakeIsActive.set(false)

        ThreadedTelemetry.LAZY_INSTANCE.log("Stopped LazyIntake")
        _storageLogic.resumeLogicAfterIntake(LAZY_INTAKE)
    }



    suspend fun handleIntake(inputBall: Ball.Name):        IntakeResult.Name
    {
        if (_storageLogic.storageCells.alreadyFull()) return IntakeResult.Name.FAIL_STORAGE_IS_FULL

        if (_storageLogic.noIntakeRaceConditionProblems(INTAKE))
        {
            _storageLogic.runStatus.setCurrentActiveProcess(INTAKE)

            if (_storageLogic.isForcedToTerminateIntake(INTAKE))
                return _storageLogic.terminateIntake(INTAKE)

            ThreadedTelemetry.LAZY_INSTANCE.log("SSM - searching for intake slot")
            val storageCanHandle = _storageLogic.storageCells.handleIntake()
            ThreadedTelemetry.LAZY_INSTANCE.log("SSM - DONE Searching, result: " + storageCanHandle.name())

            val intakeResult = _storageLogic.safeSortIntake(storageCanHandle, inputBall)

            _storageLogic.resumeLogicAfterIntake(INTAKE)
            return intakeResult
        }

        _storageLogic.resumeLogicAfterIntake(INTAKE)
        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
    {
        if (_storageLogic.storageCells.isEmpty()) return RequestResult.Name.FAIL_IS_EMPTY
        if (_storageLogic.cantHandleRequestRaceCondition(SINGLE_REQUEST))
            return _storageLogic.terminateRequest(SINGLE_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(SINGLE_REQUEST)

        val requestResult = _storageLogic.storageCells.handleRequest(request)
        ThreadedTelemetry.LAZY_INSTANCE.log("FINISHED searching, result: ${requestResult.name()}")


        val shootingResult = _storageLogic.shootRequestFinalPhase(requestResult, SINGLE_REQUEST)

        if (shootingResult == RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)
            return _storageLogic.terminateRequest(SINGLE_REQUEST)

        _storageLogic.resumeLogicAfterRequest(DRUM_REQUEST)
        return shootingResult
    }



    suspend fun streamDrumRequest():              RequestResult.Name
    {
        return if (USE_LAZY_VERSION_OF_STREAM_REQUEST)
             lazyShootEverything()
        else shootEntireDrumRequest()
    }
    private suspend fun lazyShootEverything():    RequestResult.Name
    {
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        ThreadedTelemetry.LAZY_INSTANCE.log("MODE: Lazy shoot everything")
        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)

        var shotsFired = NOTHING
        while (shotsFired < MAX_BALL_COUNT)
        {
            if (!_storageLogic.fullWaitForShotFired(DRUM_REQUEST))
                return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED

            ThreadedTelemetry.LAZY_INSTANCE.log("shot finished, updating..")
            shotsFired++
        }

        _storageLogic.resumeLogicAfterRequest(DRUM_REQUEST, false)
        return RequestResult.Name.SUCCESS_IS_NOW_EMPTY
    }
    private suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        if (_storageLogic.storageCells.isEmpty()) return RequestResult.Name.FAIL_IS_EMPTY
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)
        ThreadedTelemetry.LAZY_INSTANCE.log("MODE: SHOOT EVERYTHING")
        val requestResult = _storageLogic.shootEverything()

        _storageLogic.resumeLogicAfterRequest(DRUM_REQUEST, false)
        return requestResult
    }
    suspend fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        includeLastUnfinishedPattern:       Boolean = true,
        autoUpdateUnfinishedForNextPattern: Boolean = true): RequestResult.Name
    {
        if (_storageLogic.storageCells.isEmpty()) return RequestResult.Name.FAIL_IS_EMPTY
        if (requestOrder.isEmpty())  return RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)

        val patternOrder = if (!includeLastUnfinishedPattern) requestOrder
        else DynamicPattern.trimPattern(
            _storageLogic.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)

        if (autoUpdateUnfinishedForNextPattern)
            _storageLogic.dynamicMemoryPattern.setTemporary(patternOrder)

        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> _storageLogic.shootEverything()
                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> _storageLogic.shootEntireRequestCanSkip(patternOrder)
                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> _storageLogic.shootEntireUntilPatternBreaks(patternOrder)
                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
                    -> _storageLogic.shootEntireRequestIsValid(patternOrder)
            }

        _storageLogic.resumeLogicAfterRequest(
            DRUM_REQUEST,
            _storageLogic.storageCells.isNotEmpty())
        return requestResult
    }
    suspend fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>? = requestOrder,
        includeLastUnfinishedPattern:             Boolean = true,
        includeLastUnfinishedPatternToFailSafe:   Boolean = true,
        autoUpdateUnfinishedForNextPattern:       Boolean = true,
        whenFailedAndSavingLastKeepFailsafeOrder: Boolean = true): RequestResult.Name
    {
        if (failsafeOrder == null || failsafeOrder.isEmpty() ||
            failsafeOrder.contentEquals(requestOrder))
            return shootEntireDrumRequest(shootingMode, requestOrder, includeLastUnfinishedPattern)

        if (_storageLogic.storageCells.isEmpty()) return RequestResult.Name.FAIL_IS_EMPTY
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)



        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)

        val patternOrder = if (!includeLastUnfinishedPattern) requestOrder
        else DynamicPattern.trimPattern(
            _storageLogic.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)

        val failsafePatternOrder = if (!includeLastUnfinishedPatternToFailSafe) requestOrder
        else DynamicPattern.trimPattern(
            _storageLogic.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)



        val saveLastUnfinishedFailsafeOrder = autoUpdateUnfinishedForNextPattern
                && whenFailedAndSavingLastKeepFailsafeOrder
        if (autoUpdateUnfinishedForNextPattern)
            _storageLogic.dynamicMemoryPattern.setTemporary(patternOrder)



        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> _storageLogic.shootEverything()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> _storageLogic.shootEntireRequestCanSkip(
                    patternOrder,
                    failsafePatternOrder,
                    saveLastUnfinishedFailsafeOrder)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> _storageLogic.shootEntireUntilPatternBreaks(
                    patternOrder,
                    failsafePatternOrder,
                    saveLastUnfinishedFailsafeOrder)

                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
                    -> _storageLogic.shootEntireRequestIsValid(
                    patternOrder,
                    failsafePatternOrder,
                    saveLastUnfinishedFailsafeOrder)
            }

        _storageLogic.resumeLogicAfterRequest(
            DRUM_REQUEST,
            _storageLogic.storageCells.isNotEmpty())
        return requestResult
    }
}