package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference
import android.annotation.SuppressLint

import woen239.enumerators.Ball
import woen239.enumerators.BallRequest

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import woen239.enumerators.RunStatus
import woen239.enumerators.StorageSlot

import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.modules.camera.OnPatternDetectedEvent

import org.woen.modules.scoringSystem.storage.TerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.TerminateRequestEvent

import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent
import org.woen.modules.scoringSystem.storage.BallWasEatenByTheStorageEvent

import org.woen.modules.scoringSystem.storage.StorageFinishedIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageFinishedEveryRequestEvent

import org.woen.modules.scoringSystem.storage.StorageOpenTurretGateEvent
import org.woen.modules.scoringSystem.storage.StorageIsReadyToEatIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent

import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_ONE_BALL_PUSHING_MS

import org.woen.telemetry.Configs.STORAGE.INTAKE_RACE_CONDITION_DELAY_MS
import org.woen.telemetry.Configs.STORAGE.REQUEST_RACE_CONDITION_DELAY_MS
import woen239.enumerators.Shooting


class SortingStorage
{
    private val _storageCells = StorageCells()
    private val _dynamicMemoryPattern = DynamicPattern()

    private var _shotWasFired = AtomicReference(false)
    private var _ballWasEaten = AtomicReference(false)

    private val _intakeRunStatus  = RunStatus(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
    private val _requestRunStatus = RunStatus(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)



    constructor()
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateIntakeEvent::class, {
            eventTerminateIntake()
        } )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateRequestEvent::class, {
            eventTerminateRequest()
        } )


        ThreadedEventBus.LAZY_INSTANCE.subscribe(BallCountInStorageEvent::class, {
            it.count = anyBallCount()
        })



        ThreadedEventBus.LAZY_INSTANCE.subscribe(OnPatternDetectedEvent::class, {
            _dynamicMemoryPattern.setPermanent(it.pattern.subsequence)
        } )
        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener({ it.triangle }, {
                    _dynamicMemoryPattern.resetTemporary()
        }   )   )
        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener({ it.square },   {
                    _dynamicMemoryPattern.addToTemporary()
        }   )   )
        ThreadedGamepad.LAZY_INSTANCE.addListener(
            createClickDownListener({ it.circle },   {
                    _dynamicMemoryPattern.removeFromTemporary()
        }   )   )



        ThreadedEventBus.LAZY_INSTANCE.subscribe(ShotWasFiredEvent::class, {
            shotWasFired()
        } )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(BallWasEatenByTheStorageEvent::class, {
            ballWasEaten()
        } )
    }




    suspend fun handleIntake(inputBall: Ball.Name): IntakeResult.Name
    {
        if (_storageCells.anyBallCount() >= MAX_BALL_COUNT)
            return IntakeResult.Name.FAIL_STORAGE_IS_FULL

        ThreadedTelemetry.LAZY_INSTANCE.log("START SEARCHING INTAKE")

        if (noIntakeRaceConditionProblems())
        {
            if (doTerminateIntake()) return terminateIntake()


            val storageCanHandle = _storageCells.handleIntake()
            ThreadedTelemetry.LAZY_INSTANCE.log("DONE SEARCHING INTAKE")
            ThreadedTelemetry.LAZY_INSTANCE.log("SEARCH RESULT: " + storageCanHandle.Name())


            val intakeResult = updateAfterInput(storageCanHandle, inputBall)
            //  Safe updating storage after intake  - wont update if an error occurs

            fullResumeRequestLogic(intakeResult)
            return intakeResult
        }

        val intakeFail = IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        fullResumeRequestLogic(intakeFail)
        return intakeFail
    }
    private suspend fun updateAfterInput(intakeResult: IntakeResult, inputBall: Ball.Name): IntakeResult.Name
    {
        if (intakeResult.DidFail()) return intakeResult.Name()   //  Intake failed


        ThreadedTelemetry.LAZY_INSTANCE.log("SORTING INTAKE")
        _storageCells.hwReAdjustStorage()
        ThreadedTelemetry.LAZY_INSTANCE.log("DONE MOVING")


        if (!fullWaitForIntakeIsFinishedEvent()) return terminateIntake()

        return if (_storageCells.updateAfterIntake(inputBall))
             IntakeResult.Name.SUCCESS
        else IntakeResult.Name.FAIL_UNKNOWN
    }

    private suspend fun intakeRaceConditionIsPresent():  Boolean
    {
        if (_intakeRunStatus.IsActive())
        {
            forceStopRequest()

            delay(INTAKE_RACE_CONDITION_DELAY_MS)
            return _intakeRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private suspend fun noIntakeRaceConditionProblems(): Boolean
    {
        _intakeRunStatus.SafeResetTermination()

        return !intakeRaceConditionIsPresent()
    }

    private fun terminateIntake(): IntakeResult.Name
    {
        _intakeRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        val intakeFail = IntakeResult.Name.FAIL_PROCESS_WAS_TERMINATED
        fullResumeRequestLogic(intakeFail)

        return intakeFail
    }
    private fun doTerminateIntake() = _intakeRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    fun eventTerminateIntake() = _intakeRunStatus.DoTerminate()



    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
    {
        if (_storageCells.anyBallCount() <= 0)
            return RequestResult.Name.FAIL_IS_EMPTY

        if (cantHandleRequestRaceCondition()) return terminateRequest()

        ThreadedTelemetry.LAZY_INSTANCE.log("searching..")
        val requestResult = _storageCells.handleRequest(request)


        if (doTerminateRequest()) return terminateRequest()
        val shootingResult = shootRequestFinalPhase(requestResult)

        if (shootingResult == RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)
            return terminateRequest()

        fullResumeIntakeLogic(shootingResult)
        return shootingResult
    }
    private suspend fun updateAfterRequest(requestResult: RequestResult): RequestResult
    {
        if (requestResult.DidFail()) return requestResult   //  Intake failed
        if (doTerminateRequest())    return RequestResult(
            RequestResult.FAIL_PROCESS_WAS_TERMINATED,
            RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)

        ThreadedTelemetry.LAZY_INSTANCE.log("before updating, result: ${requestResult.Name()}")
        val fullRotations = when (requestResult.Name())
        {
            RequestResult.Name.SUCCESS_MOBILE_IN -> 3
            RequestResult.Name.SUCCESS_BOTTOM -> 2
            RequestResult.Name.SUCCESS_CENTER -> 1
            else -> -1
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("UPDATING: rotating cur slot")
        if (fullRotations >= 0)
        {
            repeat(fullRotations) {
                _storageCells.fullRotate()
                delay(DELAY_FOR_ONE_BALL_PUSHING_MS)
            }
        }
        _storageCells.hwReAdjustStorage()


        ThreadedTelemetry.LAZY_INSTANCE.log("sorting finished - success\nGetting ready to shoot")
        return RequestResult(
                RequestResult.SUCCESS,
                RequestResult.Name.SUCCESS)
    }
    private suspend fun shootRequestFinalPhase(requestResult: RequestResult) : RequestResult.Name
    {
        if (requestResult.DidFail()) return requestResult.Name()

        ThreadedTelemetry.LAZY_INSTANCE.log("preparing to update")
        val updateResult = updateAfterRequest(requestResult)
        if (updateResult.DidSucceed())
        {
            ThreadedTelemetry.LAZY_INSTANCE.log("Waiting for shot")
            if (!fullWaitForShotFired()) return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED

            return if (_storageCells.updateAfterRequest())
            {
                if  (_storageCells.anyBallCount() > 0)
                     RequestResult.Name.SUCCESS
                else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
            }
            else
            {
                //!  _storageCells.fastFixStorageDesync()
                if  (_storageCells.updateAfterRequest())
                     RequestResult.Name.SUCCESS
                else RequestResult.Name.FAIL_SOFTWARE_STORAGE_DESYNC
            }
        }
        else return updateResult.Name()
    }

    private suspend fun requestRaceConditionIsPresent():  Boolean
    {
        if (_requestRunStatus.IsActive())
        {
            forceStopIntake()
            _storageCells.pauseAnyIntake()

            delay(REQUEST_RACE_CONDITION_DELAY_MS)
            return _requestRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private suspend fun cantHandleRequestRaceCondition(): Boolean
    {
        _requestRunStatus.SafeResetTermination()

        while (requestRaceConditionIsPresent())
            delay(DELAY_FOR_EVENT_AWAITING_MS)

        return doTerminateRequest()
    }

    private suspend fun terminateRequest(): RequestResult.Name
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("[!] Request was terminated, adjusting..")

        _requestRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        val requestFail = RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
        fullResumeIntakeLogic(requestFail)

        return requestFail
    }
    private fun doTerminateRequest() = _requestRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    fun eventTerminateRequest() = _requestRunStatus.DoTerminate()



    suspend fun pushNextWithoutUpdating()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("> SW: OPEN GATE")
        _storageCells.openTurretGate()

        _storageCells.hwRotateBeltForward(DELAY_FOR_ONE_BALL_PUSHING_MS)
        delay(DELAY_FOR_ONE_BALL_PUSHING_MS)

        //_storageCells.closeTurretGate()
    }



    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        if (_storageCells.anyBallCount() <= 0) return RequestResult.Name.FAIL_IS_EMPTY
        if (cantHandleRequestRaceCondition())  return terminateRequest()

        ThreadedTelemetry.LAZY_INSTANCE.log("MODE: SHOOT EVERYTHING")
        val requestResult = shootEverything()

        fullResumeIntakeLogic(requestResult)
        return requestResult
    }
    suspend fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        includeLastUnfinishedPattern: Boolean = true,
        autoUpdateUnfinishedForNextPattern: Boolean = true
    ): RequestResult.Name
    {
        if (_storageCells.anyBallCount() <= 0) return RequestResult.Name.FAIL_IS_EMPTY
        if (cantHandleRequestRaceCondition())  return terminateRequest()

        val patternOrder = if (!includeLastUnfinishedPattern) requestOrder
            else DynamicPattern.trimPattern(
                _dynamicMemoryPattern.lastUnfinished(),
                requestOrder)

        if (autoUpdateUnfinishedForNextPattern)
            _dynamicMemoryPattern.setTemporary(patternOrder)


        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE -> shootEverything()
                Shooting.Mode.FIRE_PATTERN_CAN_SKIP -> shootEntireRequestCanSkip(patternOrder)
                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN -> shootEntireUntilPatternBreaks(patternOrder)
                else -> shootEntireRequestIsValid(patternOrder)
            }

        fullResumeIntakeLogic(requestResult)
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

        if (_storageCells.anyBallCount() <= 0) return RequestResult.Name.FAIL_IS_EMPTY
        if (cantHandleRequestRaceCondition())  return terminateRequest()



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
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE -> shootEverything()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP -> shootEntireRequestCanSkip(
                    patternOrder, failsafePatternOrder,
                    saveLastUnfinishedFailsafeOrder)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN -> shootEntireUntilPatternBreaks(
                    patternOrder, failsafePatternOrder,
                    saveLastUnfinishedFailsafeOrder)

                else -> shootEntireRequestIsValid(
                    patternOrder, failsafePatternOrder,
                    saveLastUnfinishedFailsafeOrder)
            }

        fullResumeIntakeLogic(requestResult)
        return requestResult
    }



    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEverything(): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_IS_EMPTY

        var i = StorageSlot.BOTTOM
        while (i < MAX_BALL_COUNT)
        {
            ThreadedTelemetry.LAZY_INSTANCE.log("shot ${i+1}")

                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(BallRequest.Name.ANY_CLOSEST)
            ThreadedTelemetry.LAZY_INSTANCE.log("shot ${i+1} request finished, updating..")

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult)

                if (shootingResult == RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)
                    return terminateRequest()

            i++
        }
        return shootingResult
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

        if (RequestResult.DidSucceed(shootingResult)) return shootingResult
        else if (autoUpdateUnfinishedForNextPattern)
            _dynamicMemoryPattern.setTemporary(failsafeOrder)
        return shootEntireCanSkipLogic(failsafeOrder)
    }
    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEntireCanSkipLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = StorageSlot.BOTTOM
        while (i < MAX_BALL_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(requestOrder[i])

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult)

                if (shootingResult == RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)
                    return terminateRequest()

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

        if (RequestResult.DidSucceed(shootingResult) ||
            RequestResult.WasTerminated(shootingResult))
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
                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(requestOrder[i])

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult)

                if (RequestResult.WasTerminated(shootingResult))
                    return terminateRequest()

            if (RequestResult.DidFail(shootingResult))
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

        if (doTerminateRequest()) return terminateRequest()
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

            if (doTerminateRequest()) return terminateRequest()
        if (validateEntireRequestDidSucceed(countPG, requestCountPGA))  //  All good
            return shootEntireValidRequestLogic(requestOrder)

        if (autoUpdateUnfinishedForNextPattern)
            _dynamicMemoryPattern.setTemporary(failsafeOrder)

        requestCountPGA = countPGA(failsafeOrder)

            if (doTerminateRequest()) return terminateRequest()
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
            else if (BallRequest.IsAbstractAny(requestOrder[i])) countPGA[2]++

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
    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEntireValidRequestLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_UNKNOWN

        var i = StorageSlot.BOTTOM
        while (i < MAX_BALL_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(requestOrder[i])

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult)

            if (RequestResult.WasTerminated(shootingResult)) return terminateRequest()
            if (RequestResult.DidFail(shootingResult)) return shootingResult
            //  Fast break if UNEXPECTED ERROR or TERMINATION

            i++
        }
        return shootingResult
    }





    fun forceStopIntake() = _intakeRunStatus.SetAlreadyUsed()
    fun safeResumeIntakeLogic()
    {
        if (_intakeRunStatus.IsUsedByAnotherProcess())
            _intakeRunStatus.SetActive()
    }
    private suspend fun fullResumeIntakeLogic(requestResult: RequestResult.Name)
    {
        _storageCells.hwForcePauseBelt()
        _storageCells.hwReverseBelt(DELAY_FOR_ONE_BALL_PUSHING_MS * 2)
        _storageCells.fullCalibrate()
        _storageCells.hwRotateBeltForward(DELAY_FOR_ONE_BALL_PUSHING_MS)

        safeResumeIntakeLogic()

        _storageCells.resumeIntakes(anyBallCount() < MAX_BALL_COUNT)

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            StorageFinishedEveryRequestEvent(requestResult)
        )
    }

    fun forceStopRequest() = _requestRunStatus.SetAlreadyUsed()
    private fun fullResumeRequestLogic(intakeResult: IntakeResult.Name)
    {
        if (_requestRunStatus.IsUsedByAnotherProcess())
            _requestRunStatus.SetActive()

        ThreadedEventBus.LAZY_INSTANCE.invoke(
            StorageFinishedIntakeEvent(intakeResult)
        )
    }



    fun shotWasFired() = _shotWasFired.set(true)
    private suspend fun fullWaitForShotFired(): Boolean
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("WAITING FOR SHOT - EVENT SEND")
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageOpenTurretGateEvent())
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageRequestIsReadyEvent())

        while (!_shotWasFired.get())
        {
            delay(DELAY_FOR_EVENT_AWAITING_MS)
            if (doTerminateRequest()) return false
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("DONE - Shot fired")
        ThreadedTelemetry.LAZY_INSTANCE.log("ball count - " + anyBallCount())
        _dynamicMemoryPattern.removeFromTemporary()
        _shotWasFired.set(false)
        return true
    }



    fun ballWasEaten() = _ballWasEaten.set(true)

    private suspend fun fullWaitForIntakeIsFinishedEvent(): Boolean
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageIsReadyToEatIntakeEvent())

        while (!_ballWasEaten.get())
        {
            delay(DELAY_FOR_EVENT_AWAITING_MS)
            if (doTerminateIntake()) return false
        }

        _ballWasEaten.set(false)
        return true
    }



    fun storageData() = _storageCells.storageData()
    fun anyBallCount() = _storageCells.anyBallCount()

    fun ballColorCountPG() = _storageCells.ballColorCountPG()
    fun selectedBallCount(ball: Ball.Name) = _storageCells.selectedBallCount(ball)
}