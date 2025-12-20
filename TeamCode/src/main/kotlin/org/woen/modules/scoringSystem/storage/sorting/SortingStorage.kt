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
import org.woen.modules.scoringSystem.storage.Alias.NOTHING
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT

import org.woen.modules.scoringSystem.storage.Alias.GamepadLI
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.TelemetryLI
import org.woen.modules.scoringSystem.storage.Alias.SmartCoroutineLI

import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent

import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StopLazyIntakeEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent

import org.woen.modules.scoringSystem.storage.StorageInitiatePredictSortEvent
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent
import org.woen.modules.scoringSystem.storage.StorageUpdateAfterLazyIntakeEvent

import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.telemetry.Configs.DELAY
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



        EventBusLI.subscribe(
            StorageUpdateAfterLazyIntakeEvent::class, {

                _storageLogic.storageCells.updateAfterLazyIntake(
                    it.inputFromTurretSlotToBottom)
        }   )

        EventBusLI.subscribe(OnPatternDetectedEvent::class, {

                _storageLogic.dynamicMemoryPattern.setPermanent(it.pattern.subsequence)
        }   )
    }
    private fun subscribeToActionEvents()
    {
        EventBusLI.subscribe(
            StartLazyIntakeEvent::class, {

                val tryToStartLazyIntake = _storageLogic.canStartIntakeIsNotBusy()
                it.startingResult = tryToStartLazyIntake

                if (tryToStartLazyIntake != Intake.IS_BUSY)
                    SmartCoroutineLI.launch {
                        startLazyIntake()
                    }
        }   )
        EventBusLI.subscribe(
            StopLazyIntakeEvent::class, {

                _storageLogic.lazyIntakeIsActive.set(false)
        }   )


        EventBusLI.subscribe(
            StorageInitiatePredictSortEvent::class, {

                val canInitiate   = _storageLogic.canInitiatePredictSort()
                it.startingResult = canInitiate

                if (canInitiate)
                    SmartCoroutineLI.launch {
                        _storageLogic.safeInitiatePredictSort(it.requestedPattern)
                    }
            }
        )
    }
    private fun subscribeToGamepadEvents()
    {
        GamepadLI.addListener(
        createClickDownListener({ it.triangle }, {

                    _storageLogic.dynamicMemoryPattern.resetTemporary()
        }   )   )

        GamepadLI.addListener(
        createClickDownListener({ it.square },   {

                    _storageLogic.dynamicMemoryPattern.addToTemporary()
        }   )   )

        GamepadLI.addListener(
        createClickDownListener({ it.circle },   {

                    _storageLogic.dynamicMemoryPattern.removeFromTemporary()
        }   )   )



//        GamepadLI.addListener(
//            createClickDownListener(
//                { it.touchpadWasPressed() }, {
//
//                    SmartCoroutineLI.launch {
//                        var iteration = NOTHING
//                        while (iteration < 100)
//                        {
//                            _storageLogic.storageCells.fullRotate()
//                            iteration++
//                    }   }
//        }   )   )
//
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

            val activeIntakeProcessId = _storageLogic.runStatus.getCurrentActiveProcess()
            TelemetryLI.log("SSM Terminate intake process id: $activeIntakeProcessId")

            if (activeIntakeProcessId == INTAKE ||
                activeIntakeProcessId == LAZY_INTAKE)
            {
                _storageLogic.runStatus.addProcessToTerminationList(activeIntakeProcessId)
                if (activeIntakeProcessId == LAZY_INTAKE)
                    _storageLogic.lazyIntakeIsActive.set(false)
            }
        }   )

        EventBusLI.subscribe(TerminateRequestEvent::class, {

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
            && _storageLogic.canStartIntakeIsNotBusy() != Intake.IS_BUSY)
            startLazyIntake()
    }
    suspend fun startLazyIntake()
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



    suspend fun handleIntake(inputBall: Ball.Name):        IntakeResult.Name
    {
        if (_storageLogic.storageCells.alreadyFull()) return Intake.IS_FULL

        if (_storageLogic.noIntakeRaceConditionProblems(INTAKE))
        {
            _storageLogic.runStatus.setCurrentActiveProcess(INTAKE)

            if (_storageLogic.isForcedToTerminate(INTAKE))
                return _storageLogic.terminateIntake(INTAKE)

            TelemetryLI.log("SSM - searching for intake slot")
            val storageCanHandle = _storageLogic.storageCells.handleIntake()
            TelemetryLI.log("SSM - DONE Searching, result: " + storageCanHandle.name())

            val intakeResult = _storageLogic.safeSortIntake(storageCanHandle, inputBall)

            _storageLogic.resumeLogicAfterIntake(INTAKE)
            return intakeResult
        }

        _storageLogic.resumeLogicAfterIntake(INTAKE)
        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
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
             lazyShootEverything()
        else shootEntireDrumRequest()
    }
    private suspend fun lazyShootEverything():    RequestResult.Name
    {
        if (_storageLogic.cantHandleRequestRaceCondition(DRUM_REQUEST))
            return _storageLogic.terminateRequest(DRUM_REQUEST)

        TelemetryLI.log("MODE: Lazy shoot everything")
        _storageLogic.runStatus.setCurrentActiveProcess(DRUM_REQUEST)

        var shotsFired = NOTHING
        while (shotsFired < MAX_BALL_COUNT)
        {
            if (!_storageLogic.fullWaitForShotFired(
                    DRUM_REQUEST,
                    false))
                return Request.TERMINATED

            TelemetryLI.log("shot finished, updating..")
            shotsFired++
        }

        _storageLogic.resumeLogicAfterRequest(DRUM_REQUEST, false)
        return Request.SUCCESS_NOW_EMPTY
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
        autoUpdateUnfinishedForNextPattern: Boolean = true): RequestResult.Name
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