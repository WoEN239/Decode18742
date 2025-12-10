package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import android.annotation.SuppressLint

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult

import org.woen.enumerators.Shooting
import org.woen.enumerators.StorageSlot

import org.woen.hotRun.HotRun
import org.woen.utils.process.RunStatus

import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.modules.camera.OnPatternDetectedEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent

import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent
import org.woen.modules.scoringSystem.storage.DisableSortingModuleEvent
import org.woen.modules.scoringSystem.storage.EnableSortingModuleEvent
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent

import org.woen.modules.scoringSystem.storage.StartLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StopLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageUpdateAfterLazyIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT

import org.woen.telemetry.Configs.PROCESS_ID.INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.LAZY_INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.DRUM_REQUEST
import org.woen.telemetry.Configs.PROCESS_ID.SINGLE_REQUEST

import org.woen.telemetry.Configs.PROCESS_ID.UNDEFINED_PROCESS_ID
import org.woen.telemetry.Configs.PROCESS_ID.PRIORITY_SETTING_FOR_SORTING_STORAGE

import org.woen.telemetry.Configs.SORTING_AUTO_OPMODE.IS_SORTING_MODULE_ACTIVE_AT_START_UP



class SortingStorage
{
    private val _storageCells = StorageCells()
    private val _dynamicMemoryPattern = DynamicPattern()

    private val _shotWasFired          = AtomicBoolean(false)
    private val _lazyIntakeIsActive    = AtomicBoolean(false)
    private val _isSortingModuleActive = AtomicBoolean(IS_SORTING_MODULE_ACTIVE_AT_START_UP)

    private val _runStatus = RunStatus(PRIORITY_SETTING_FOR_SORTING_STORAGE)



    constructor()
    {
        subscribeToTerminateEvents()
        subscribeToInfoEvents()
        subscribeToGamepadEvents()

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            resetParametersToDefault()
            _storageCells.resetParametersToDefault()
        }
    }

    private fun subscribeToTerminateEvents()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateIntakeEvent::class, {

                val intakeProcessId = getActiveIntakeProcessId()
                if (intakeProcessId != UNDEFINED_PROCESS_ID)
                    _runStatus.addProcessToTerminationList(intakeProcessId)
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateRequestEvent::class, {

                val requestProcessId = getActiveRequestProcessId()
                if (requestProcessId != UNDEFINED_PROCESS_ID)
                    _runStatus.addProcessToTerminationList(requestProcessId)
        }   )
    }
    private fun subscribeToInfoEvents()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            EnableSortingModuleEvent::class, {
                _isSortingModuleActive.set(true)
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            DisableSortingModuleEvent::class, {
                _isSortingModuleActive.set(false)
        }   )


        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StartLazyIntakeEvent::class, {

                if (_isSortingModuleActive.get())
                {
                    val tryToStartLazyIntake = canStartIntakeIsNotBusy()
                    it.startingResult = tryToStartLazyIntake

                    if (tryToStartLazyIntake != IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY)
                        startLazyIntakes()
                }
                else ThreadedTelemetry.LAZY_INSTANCE.log("! Sorting module is inactive")
        }   )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StopLazyIntakeEvent::class, {

                if (_isSortingModuleActive.get())
                     _lazyIntakeIsActive.set(false)
                else ThreadedTelemetry.LAZY_INSTANCE.log("! Sorting module is inactive")
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageUpdateAfterLazyIntakeEvent::class, {

                if (_isSortingModuleActive.get())
                     setAfterLazyIntake(it.inputFromTurretSlotToBottom)
                else ThreadedTelemetry.LAZY_INSTANCE.log("! Sorting module is inactive")
        }   )



        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            ShotWasFiredEvent::class, {

                if (_isSortingModuleActive.get())
                     shotWasFired()
                else ThreadedTelemetry.LAZY_INSTANCE.log("! Sorting module is inactive")
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            BallCountInStorageEvent::class, {

                if (_isSortingModuleActive.get())
                     it.count = anyBallCount()
                else ThreadedTelemetry.LAZY_INSTANCE.log("! Sorting module is inactive")
        }   )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            StorageHandleIdenticalColorsEvent::class, {

                if (_isSortingModuleActive.get())
                {
                    val result = _storageCells.handleIdenticalColorRequest()

                    it.maxIdenticalColorCount = result.maxIdenticalColorCount
                    it.identicalColor = result.identicalColor
                }
                else ThreadedTelemetry.LAZY_INSTANCE.log("! Sorting module is inactive")
        }   )



        ThreadedEventBus.LAZY_INSTANCE.subscribe(OnPatternDetectedEvent::class, {
                _dynamicMemoryPattern.setPermanent(it.pattern.subsequence)
        }   )
    }
    private fun subscribeToGamepadEvents()
    {
        ThreadedGamepad.LAZY_INSTANCE.addListener(
        createClickDownListener({ it.triangle }, {

                    if (_isSortingModuleActive.get())
                         _dynamicMemoryPattern.resetTemporary()
                    else ThreadedTelemetry.LAZY_INSTANCE.log("! Sorting module is inactive")
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
        createClickDownListener({ it.square },   {

                    if (_isSortingModuleActive.get())
                         _dynamicMemoryPattern.addToTemporary()
                    else ThreadedTelemetry.LAZY_INSTANCE.log("! Sorting module is inactive")
        }   )   )

        ThreadedGamepad.LAZY_INSTANCE.addListener(
        createClickDownListener({ it.circle },   {

                    if (_isSortingModuleActive.get())
                         _dynamicMemoryPattern.removeFromTemporary()
                    else ThreadedTelemetry.LAZY_INSTANCE.log("! Sorting module is inactive")
        }   )   )
    }
    private fun resetParametersToDefault()
    {
        _dynamicMemoryPattern.fullReset()
        _shotWasFired.set(false)

        _runStatus.fullResetToActiveState()
    }



    fun setAfterLazyIntake(inputBalls: Array<Ball.Name>)
        = _storageCells.updateAfterLazyIntake(inputBalls)


    suspend fun startLazyIntakes()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("Started LazyIntake")
        _runStatus.setCurrentActiveProcess(LAZY_INTAKE)
        _lazyIntakeIsActive.set(true)

        _storageCells.hwStartBelts()
        while (_lazyIntakeIsActive.get())
        {
            delay(DELAY.EVENT_AWAITING_MS)

            if (isForcedToTerminateIntake(LAZY_INTAKE))
                terminateIntake(LAZY_INTAKE)
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("Stopped LazyIntake")
        resumeLogicAfterIntake(LAZY_INTAKE)
    }
    suspend fun canStartIntakeIsNotBusy(): IntakeResult.Name
    {
        if (noIntakeRaceConditionProblems(LAZY_INTAKE))
            return IntakeResult.Name.STARTED_SUCCESSFULLY

        resumeLogicAfterIntake(LAZY_INTAKE)
        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    suspend fun handleIntake(inputBall: Ball.Name): IntakeResult.Name
    {
        if (_storageCells.alreadyFull()) return IntakeResult.Name.FAIL_STORAGE_IS_FULL

        if (noIntakeRaceConditionProblems(INTAKE))
        {
            _runStatus.setCurrentActiveProcess(INTAKE)

            if (isForcedToTerminateIntake(INTAKE))
                return terminateIntake(INTAKE)

            ThreadedTelemetry.LAZY_INSTANCE.log("SSM - searching for intake slot")
            val storageCanHandle = _storageCells.handleIntake()
            ThreadedTelemetry.LAZY_INSTANCE.log("SSM - DONE Searching, result: " + storageCanHandle.name())

            val intakeResult = safeSortIntake(storageCanHandle, inputBall)

            resumeLogicAfterIntake(INTAKE)
            return intakeResult
        }

        resumeLogicAfterIntake(INTAKE)
        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    private suspend fun safeSortIntake(intakeResult: IntakeResult, inputBall: Ball.Name): IntakeResult.Name
    {
        if (intakeResult.didFail()) return intakeResult.name()   //  Intake failed

        ThreadedTelemetry.LAZY_INSTANCE.log("SSM - Sorting intake")
        _storageCells.hwReAdjustStorage()
        _storageCells.hwForwardBeltsTime(DELAY.ONE_BALL_PUSHING_MS / 2)

        _storageCells.updateAfterIntake(inputBall)
        return IntakeResult.Name.SUCCESS
    }

    private suspend fun intakeRaceConditionIsPresent(processId: Int):  Boolean
    {
        if (_runStatus.isNotBusy())
        {
            _runStatus.addProcessToQueue(processId)

            delay(DELAY.INTAKE_RACE_CONDITION_MS)
            return !_runStatus.isThisProcessHighestPriority(processId)
        }
        return true
    }
    private suspend fun noIntakeRaceConditionProblems(processId: Int): Boolean
    {
        _runStatus.safeRemoveThisProcessFromTerminationList(processId)

        return !intakeRaceConditionIsPresent(processId)
    }

    private fun terminateIntake(processId: Int): IntakeResult.Name
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("[!] Intake is being terminated..")

        _runStatus.safeToRemoveProcessIdFromQueue(processId)
        _runStatus.safeRemoveThisProcessFromTerminationList(processId)

        resumeLogicAfterIntake(processId)
        return IntakeResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }
    private fun getActiveIntakeProcessId(): Int
    {
        val activeProcess = _runStatus.getCurrentActiveProcess()

        return if (activeProcess == INTAKE || activeProcess == LAZY_INTAKE)
            activeProcess else -1
    }
    private fun isForcedToTerminateIntake(processId: Int)
        = _runStatus.isForcedToTerminateThisProcess(processId)



    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
    {
        if (_storageCells.isEmpty()) return RequestResult.Name.FAIL_IS_EMPTY
        if (cantHandleRequestRaceCondition(SINGLE_REQUEST))
            return terminateRequest(SINGLE_REQUEST)

        _runStatus.setCurrentActiveProcess(SINGLE_REQUEST)

        val requestResult = _storageCells.handleRequest(request)
        ThreadedTelemetry.LAZY_INSTANCE.log("FINISHED searching, result: ${requestResult.name()}")


        val shootingResult = shootRequestFinalPhase(requestResult, SINGLE_REQUEST)

        if (shootingResult == RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)
            return terminateRequest(SINGLE_REQUEST)

        resumeLogicAfterRequest(DRUM_REQUEST)
        return shootingResult
    }
    private suspend fun rotateToFoundBall(
        requestResult: RequestResult,
        processId: Int): RequestResult
    {
        if (requestResult.didFail()) return requestResult  //  Request search failed
        if (isForcedToTerminateRequest(processId))
            return RequestResult(
                RequestResult.FAIL_PROCESS_WAS_TERMINATED,
                RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)

        ThreadedTelemetry.LAZY_INSTANCE.log("custom readjusting")
        _storageCells.hwCloseTurretGate()
        _storageCells.hwForwardBeltsTime(DELAY.ONE_BALL_PUSHING_MS / 2)

        val fullRotations = when (requestResult.name())
        {
            RequestResult.Name.SUCCESS_MOBILE -> 3
            RequestResult.Name.SUCCESS_BOTTOM -> 2
            RequestResult.Name.SUCCESS_CENTER -> 1
            else -> -1
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("updating: rotating cur slot")
        if (fullRotations >= 0)
            repeat(fullRotations)
                { _storageCells.fullRotate() }

        _storageCells.hwReAdjustStorage()


        ThreadedTelemetry.LAZY_INSTANCE.log("sorting finished - success", "Getting ready to shoot")
        return RequestResult(
                RequestResult.SUCCESS,
                RequestResult.Name.SUCCESS)
    }
    private suspend fun shootRequestFinalPhase(
        requestResult: RequestResult,
        processId: Int): RequestResult.Name
    {
        if (requestResult.didFail()) return requestResult.name()
        if (isForcedToTerminateRequest(processId))
            return terminateRequest(processId)

        ThreadedTelemetry.LAZY_INSTANCE.log("preparing to update")
        val updateResult = rotateToFoundBall(requestResult, processId)

        return if (updateResult.didSucceed())
        {
            if (!fullWaitForShotFired(processId))
                 RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
            else if (_storageCells.isNotEmpty())
                 RequestResult.Name.SUCCESS
            else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        }
        else updateResult.name()
    }

    private suspend fun requestRaceConditionIsPresent(processId: Int):  Boolean
    {
        if (_runStatus.isNotBusy())
        {
            _runStatus.addProcessToQueue(processId)
            _storageCells.pauseAnyIntake()

            delay(DELAY.REQUEST_RACE_CONDITION_MS)
            return !_runStatus.isThisProcessHighestPriority(processId)
        }
        return true
    }
    private suspend fun cantHandleRequestRaceCondition(processId: Int): Boolean
    {
        _runStatus.safeToRemoveProcessIdFromQueue(processId)
        _runStatus.safeRemoveThisProcessFromTerminationList(processId)

        while (requestRaceConditionIsPresent(processId))
            delay(DELAY.EVENT_AWAITING_MS)

        return isForcedToTerminateRequest(processId)
    }

    private suspend fun terminateRequest(processId: Int): RequestResult.Name
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("[!] Request is being terminated..")

        _runStatus.safeToRemoveProcessIdFromQueue(processId)
        _runStatus.safeRemoveThisProcessFromTerminationList(processId)

        resumeLogicAfterRequest(processId)
        return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }
    private fun getActiveRequestProcessId(): Int
    {
        val activeProcess = _runStatus.getCurrentActiveProcess()

        return if (activeProcess != INTAKE) activeProcess else -1
    }
    private fun isForcedToTerminateRequest(processId: Int)
        = _runStatus.isForcedToTerminateThisProcess(processId)





    suspend fun lazyShootEverything(): RequestResult.Name
    {
        if (cantHandleRequestRaceCondition(DRUM_REQUEST))
            return terminateRequest(DRUM_REQUEST)

        ThreadedTelemetry.LAZY_INSTANCE.log("MODE: Lazy shoot everything")
        _runStatus.setCurrentActiveProcess(DRUM_REQUEST)

        var shotsFired = 0
        while (shotsFired < MAX_BALL_COUNT)
        {
            if (!fullWaitForShotFired(DRUM_REQUEST))
                return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED

            ThreadedTelemetry.LAZY_INSTANCE.log("shot finished, updating..")
            shotsFired++
        }
        return RequestResult.Name.SUCCESS_IS_NOW_EMPTY
    }
    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        if (_storageCells.isEmpty()) return RequestResult.Name.FAIL_IS_EMPTY
        if (cantHandleRequestRaceCondition(DRUM_REQUEST))
            return terminateRequest(DRUM_REQUEST)

        _runStatus.setCurrentActiveProcess(DRUM_REQUEST)
        ThreadedTelemetry.LAZY_INSTANCE.log("MODE: SHOOT EVERYTHING")
        val requestResult = shootEverything()

        resumeLogicAfterRequest(DRUM_REQUEST, false)
        return requestResult
    }
    suspend fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        includeLastUnfinishedPattern: Boolean = true,
        autoUpdateUnfinishedForNextPattern: Boolean = true
    ): RequestResult.Name
    {
        if (_storageCells.isEmpty()) return RequestResult.Name.FAIL_IS_EMPTY
        if (requestOrder.isEmpty())  return RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
        if (cantHandleRequestRaceCondition(DRUM_REQUEST))
            return terminateRequest(DRUM_REQUEST)

        _runStatus.setCurrentActiveProcess(DRUM_REQUEST)

        val patternOrder = if (!includeLastUnfinishedPattern) requestOrder
            else DynamicPattern.trimPattern(
                _dynamicMemoryPattern.lastUnfinished(),
                requestOrder)

        if (autoUpdateUnfinishedForNextPattern)
            _dynamicMemoryPattern.setTemporary(patternOrder)

        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> shootEverything()
                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> shootEntireRequestCanSkip(patternOrder)
                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> shootEntireUntilPatternBreaks(patternOrder)
                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
                    -> shootEntireRequestIsValid(patternOrder)
            }

        resumeLogicAfterRequest(DRUM_REQUEST, _storageCells.isNotEmpty())
        return requestResult
    }
    suspend fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>? = requestOrder,
        includeLastUnfinishedPattern: Boolean = true,
        includeLastUnfinishedPatternToFailSafe: Boolean = true,
        autoUpdateUnfinishedForNextPattern: Boolean = true,
        whenFailedAndSavingLastKeepFailsafeOrder: Boolean = true
    ): RequestResult.Name
    {
        if (failsafeOrder == null || failsafeOrder.isEmpty() ||
            failsafeOrder.contentEquals(requestOrder))
            return shootEntireDrumRequest(shootingMode, requestOrder, includeLastUnfinishedPattern)

        if (_storageCells.isEmpty()) return RequestResult.Name.FAIL_IS_EMPTY
        if (cantHandleRequestRaceCondition(DRUM_REQUEST))
            return terminateRequest(DRUM_REQUEST)



        _runStatus.setCurrentActiveProcess(DRUM_REQUEST)

        val patternOrder = if (!includeLastUnfinishedPattern) requestOrder
        else DynamicPattern.trimPattern(
            _dynamicMemoryPattern.lastUnfinished(),
            requestOrder)

        val failsafePatternOrder = if (!includeLastUnfinishedPatternToFailSafe) requestOrder
        else DynamicPattern.trimPattern(
            _dynamicMemoryPattern.lastUnfinished(),
            requestOrder)



        val saveLastUnfinishedFailsafeOrder = autoUpdateUnfinishedForNextPattern
                && whenFailedAndSavingLastKeepFailsafeOrder
        if (autoUpdateUnfinishedForNextPattern)
            _dynamicMemoryPattern.setTemporary(patternOrder)



        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> shootEverything()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> shootEntireRequestCanSkip(
                            patternOrder,
                            failsafePatternOrder,
                            saveLastUnfinishedFailsafeOrder)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> shootEntireUntilPatternBreaks(
                            patternOrder,
                            failsafePatternOrder,
                            saveLastUnfinishedFailsafeOrder)

                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
                    -> shootEntireRequestIsValid(
                            patternOrder,
                            failsafePatternOrder,
                            saveLastUnfinishedFailsafeOrder)
            }

        resumeLogicAfterRequest(DRUM_REQUEST, _storageCells.isNotEmpty())
        return requestResult
    }



    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEverything(): RequestResult.Name
    {
        var ballCount = _storageCells.anyBallCount()
        if (ballCount == 0) return RequestResult.Name.FAIL_IS_EMPTY

        while (ballCount > 0)
        {
            if (!fullWaitForShotFired(DRUM_REQUEST))
                return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED

            ThreadedTelemetry.LAZY_INSTANCE.log("shot finished, updating..")
            ballCount--
        }
        return RequestResult.Name.SUCCESS_IS_NOW_EMPTY
    }



    private suspend fun shootEntireRequestCanSkip(
        requestOrder: Array<BallRequest.Name>
    ): RequestResult.Name = shootEntireCanSkipLogic(requestOrder)
    private suspend fun shootEntireRequestCanSkip(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>,
        autoUpdateUnfinishedForNextPattern: Boolean = true
    ): RequestResult.Name
    {
        val shootingResult = shootEntireCanSkipLogic(requestOrder)

        if (RequestResult.didSucceed(shootingResult)) return shootingResult
        else if (autoUpdateUnfinishedForNextPattern)
            _dynamicMemoryPattern.setTemporary(failsafeOrder)
        return shootEntireCanSkipLogic(failsafeOrder)
    }
    private suspend fun shootEntireCanSkipLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = StorageSlot.BOTTOM
        while (i < MAX_BALL_COUNT)
        {
            if (isForcedToTerminateRequest(DRUM_REQUEST))
                return terminateRequest(DRUM_REQUEST)

            val requestResult = _storageCells.handleRequest(requestOrder[i])
            shootingResult = shootRequestFinalPhase(requestResult, DRUM_REQUEST)

            if (shootingResult == RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)
                return terminateRequest(DRUM_REQUEST)

            i++
        }
        return shootingResult
    }



    private suspend fun shootEntireUntilPatternBreaks(
        requestOrder:  Array<BallRequest.Name>
    ): RequestResult.Name = shootEntireUntilBreaksLogic(requestOrder)
    private suspend fun shootEntireUntilPatternBreaks(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>,
        autoUpdateUnfinishedForNextPattern: Boolean = true
    ): RequestResult.Name
    {
        val shootingResult = shootEntireUntilBreaksLogic(requestOrder)

        if (RequestResult.didSucceed(shootingResult) ||
            RequestResult.wasTerminated(shootingResult))
            return shootingResult
        else if (autoUpdateUnfinishedForNextPattern)
            _dynamicMemoryPattern.setTemporary(failsafeOrder)
        return shootEntireUntilBreaksLogic(failsafeOrder)
    }
    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEntireUntilBreaksLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = StorageSlot.BOTTOM
        while (i < MAX_BALL_COUNT)
        {
            if (isForcedToTerminateRequest(DRUM_REQUEST))
                return terminateRequest(DRUM_REQUEST)

            val requestResult = _storageCells.handleRequest(requestOrder[i])
            shootingResult = shootRequestFinalPhase(requestResult, DRUM_REQUEST)

            if (RequestResult.wasTerminated(shootingResult))
                return terminateRequest(DRUM_REQUEST)

            if (RequestResult.didFail(shootingResult))
                i += MAX_BALL_COUNT  //  Fast break if next ball is not present

            i++
        }
        return shootingResult
    }



    private suspend fun shootEntireRequestIsValid(
        requestOrder:  Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val countPG = _storageCells.ballColorCountPG()
        val requestCountPGA = countPGA(requestOrder)

        if (validateEntireRequestDidSucceed(countPG, requestCountPGA))
            return shootEntireValidRequestLogic(requestOrder)

        return RequestResult.Name.FAIL_NOT_ENOUGH_COLORS
    }
    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEntireRequestIsValid(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>,
        autoUpdateUnfinishedForNextPattern: Boolean = true
    ): RequestResult.Name
    {
        val countPG = _storageCells.ballColorCountPG()
        var requestCountPGA = countPGA(requestOrder)

        if (validateEntireRequestDidSucceed(countPG, requestCountPGA))  //  All good
            return shootEntireValidRequestLogic(requestOrder)


        if (autoUpdateUnfinishedForNextPattern)
            _dynamicMemoryPattern.setTemporary(failsafeOrder)
        requestCountPGA = countPGA(failsafeOrder)


        if (validateEntireRequestDidSucceed(countPG, requestCountPGA))  //  Failsafe good
            return shootEntireValidRequestLogic(failsafeOrder)

        return RequestResult.Name.FAIL_NOT_ENOUGH_COLORS  //  All bad
    }

    private fun countPGA(requestOrder: Array<BallRequest.Name>): IntArray
    {
        val countPGA = intArrayOf(0, 0, 0)

        var i = 0; val ballCountInRequest = min(requestOrder.size, MAX_BALL_COUNT)
        while (i < ballCountInRequest)
        {
            if      (requestOrder[i] == BallRequest.Name.PURPLE) countPGA[0]++
            else if (requestOrder[i] == BallRequest.Name.GREEN)  countPGA[1]++
            else if (BallRequest.isAbstractAny(requestOrder[i])) countPGA[2]++

            i++
        }
        return countPGA
    }
    private fun validateEntireRequestDidSucceed(countPG: IntArray, requestCountPGA: IntArray): Boolean
    {
        val storageDeltaAfterRequests = intArrayOf(
            countPG[0] - requestCountPGA[0],
            countPG[1] - requestCountPGA[1]
        )

        return storageDeltaAfterRequests[0] >= 0 && storageDeltaAfterRequests[1] >= 0 &&
                storageDeltaAfterRequests[0] + storageDeltaAfterRequests[1] >= requestCountPGA[2]
    }
    private suspend fun shootEntireValidRequestLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_UNKNOWN

        var i = StorageSlot.BOTTOM
        while (i < MAX_BALL_COUNT)
        {
            if (isForcedToTerminateRequest(DRUM_REQUEST))
                return terminateRequest(DRUM_REQUEST)

            val requestResult = _storageCells.handleRequest(requestOrder[i])
            shootingResult = shootRequestFinalPhase(requestResult, DRUM_REQUEST)

            if (RequestResult.wasTerminated(shootingResult))
                return terminateRequest(DRUM_REQUEST)
            if (RequestResult.didFail(shootingResult)) return shootingResult
            //  Fast break if UNEXPECTED ERROR or TERMINATION

            i++
        }
        return shootingResult
    }





    private suspend fun resumeLogicAfterRequest(
        processId: Int,
        doAutoCalibration: Boolean = true)
    {
        if (doAutoCalibration)
        {
            _storageCells.hwStopBelts()
            _storageCells.hwReverseBeltsTime(DELAY.ONE_BALL_PUSHING_MS * 2)

            _storageCells.fullCalibrate()
            _storageCells.hwForwardBeltsTime(DELAY.ONE_BALL_PUSHING_MS)
        }
        else _storageCells.fullCalibrate()

        _runStatus.safeToRemoveProcessIdFromQueue(processId)
        _runStatus.clearCurrentActiveProcess()
        _storageCells.resumeIntakes()
    }

    private fun resumeLogicAfterIntake(processId: Int)
    {
        _runStatus.safeToRemoveProcessIdFromQueue(processId)
        _runStatus.clearCurrentActiveProcess()
    }



    private fun shotWasFired() = _shotWasFired.set(true)
    private suspend fun fullWaitForShotFired(processId: Int): Boolean
    {
        _storageCells.hwOpenTurretGate()
        ThreadedTelemetry.LAZY_INSTANCE.log("SSM waiting for shot - event send")
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageRequestIsReadyEvent())

        var timePassedWaitingForShot: Long = 0

        while (!_shotWasFired.get() &&
            timePassedWaitingForShot < DELAY.MAX_SHOT_AWAITING_MS)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            timePassedWaitingForShot += DELAY.EVENT_AWAITING_MS
            if (isForcedToTerminateRequest(processId)) return false
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("SSM - DONE waiting for shot")
        ThreadedTelemetry.LAZY_INSTANCE.log("SSM: fired? ${_shotWasFired.get()}," +
                " delta time: $timePassedWaitingForShot")

        _storageCells.updateAfterRequest()
        _dynamicMemoryPattern.removeFromTemporary()
        _shotWasFired.set(false)
        return true
    }



    fun hwStartBelts() = _storageCells.hwStartBelts()
    fun hwStopBelts()  = _storageCells.hwStopBelts()



    fun storageData()  = _storageCells.storageData()
    fun anyBallCount() = _storageCells.anyBallCount()
    fun alreadyFull()  = _storageCells.alreadyFull()

    fun ballColorCountPG() = _storageCells.ballColorCountPG()
    fun selectedBallCount(ball: Ball.Name) = _storageCells.selectedBallCount(ball)
}