package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult

import org.woen.enumerators.StorageSlot

import org.woen.utils.process.RunStatus

import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedEventBus
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.GENERIC.NOTHING
import org.woen.telemetry.Configs.GENERIC.MAX_BALL_COUNT

import org.woen.telemetry.Configs.PROCESS_ID.LAZY_INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.DRUM_REQUEST
import org.woen.telemetry.Configs.PROCESS_ID.PREDICT_SORT

import org.woen.telemetry.Configs.PROCESS_ID.PRIORITY_SETTING_FOR_SORTING_STORAGE



class SortingStorageLogic
{
    val storageCells = StorageCells()
    val dynamicMemoryPattern = DynamicPattern()

    val shotWasFired          = AtomicBoolean(false)
    val lazyIntakeIsActive    = AtomicBoolean(false)

    val runStatus = RunStatus(PRIORITY_SETTING_FOR_SORTING_STORAGE)


    fun resetParametersToDefault()
    {
        dynamicMemoryPattern.fullReset()
        shotWasFired.set(false)

        runStatus.fullResetToActiveState()
    }



    suspend fun canInitiatePredictSort(): Boolean
    {
        if (runStatus.isUsedByAnyProcess()) return false

        runStatus.addProcessToQueue(PREDICT_SORT)
        delay(DELAY.PREDICT_SORT_RACE_CONDITION_MS)

        return runStatus.isThisProcessHighestPriority(PREDICT_SORT)
    }
    suspend fun safeInitiatePredictSort(requested: Array<BallRequest.Name>)
    {
        runStatus.setCurrentActiveProcess(PREDICT_SORT)

        storageCells.initiatePredictSort(requested)

        runStatus.clearCurrentActiveProcess()
        runStatus.safeRemoveThisProcessIdFromQueue(PREDICT_SORT)
    }



    suspend fun canStartIntakeIsNotBusy(): IntakeResult.Name
    {
        if (noIntakeRaceConditionProblems(LAZY_INTAKE))
            return IntakeResult.Name.STARTED_SUCCESSFULLY

        resumeLogicAfterIntake(LAZY_INTAKE)
        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    suspend fun safeSortIntake(intakeResult: IntakeResult, inputBall: Ball.Name): IntakeResult.Name
    {
        if (intakeResult.didFail()) return intakeResult.name()   //  Intake failed

        ThreadedTelemetry.LAZY_INSTANCE.log("SSM - Sorting intake")
        storageCells.hwReAdjustStorage()
        storageCells.hwSortingM.hwForwardBeltsTime(DELAY.ONE_BALL_PUSHING_MS / 2)

        storageCells.updateAfterIntake(inputBall)
        return IntakeResult.Name.SUCCESS
    }

    private suspend fun intakeRaceConditionIsPresent(processId: Int):  Boolean
    {
        if (runStatus.isUsedByAnyProcess()) return true

        runStatus.addProcessToQueue(processId)
        delay(DELAY.INTAKE_RACE_CONDITION_MS)

        return !runStatus.isThisProcessHighestPriority(processId)
    }
    suspend fun noIntakeRaceConditionProblems(processId: Int): Boolean
    {
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

        return !intakeRaceConditionIsPresent(processId)
    }

    fun terminateIntake(processId: Int): IntakeResult.Name
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("[!] Intake is being terminated..")

        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

        resumeLogicAfterIntake(processId)
        return IntakeResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }
    fun isForcedToTerminateIntake(processId: Int)
        = runStatus.isForcedToTerminateThisProcess(processId)




    private suspend fun rotateToFoundBall(
        requestResult: RequestResult,
        processId: Int): RequestResult
    {
        if (requestResult.didFail()) return requestResult  //  Request search failed
        if (isForcedToTerminateRequest(processId))
            return RequestResult(
                RequestResult.FAIL_PROCESS_WAS_TERMINATED,
                RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)

        val fullRotations = when (requestResult.name())
        {
            RequestResult.Name.SUCCESS_MOBILE -> 3
            RequestResult.Name.SUCCESS_BOTTOM -> 2
            RequestResult.Name.SUCCESS_CENTER -> 1
            else -> -1
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("updating: rotating cur slot")
        if (fullRotations >= NOTHING)
            repeat(fullRotations)
                { storageCells.fullRotate() }


        ThreadedTelemetry.LAZY_INSTANCE.log("sorting finished - success", "Getting ready to shoot")
        return RequestResult(
                RequestResult.SUCCESS,
                RequestResult.Name.SUCCESS)
    }
    suspend fun shootRequestFinalPhase(
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
            else if (storageCells.isNotEmpty())
                 RequestResult.Name.SUCCESS
            else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        }
        else updateResult.name()
    }

    private suspend fun requestRaceConditionIsPresent(processId: Int):  Boolean
    {
        if (isForcedToTerminateRequest(processId)) return false
        else if (runStatus.isUsedByAnotherProcess(processId)) return true


        runStatus.addProcessToQueue(processId)
        storageCells.pauseAnyIntake()

        delay(DELAY.REQUEST_RACE_CONDITION_MS)
        return !runStatus.isThisProcessHighestPriority(processId)
    }
    suspend fun cantHandleRequestRaceCondition(processId: Int): Boolean
    {
        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

        lazyIntakeIsActive.set(false)

        while (requestRaceConditionIsPresent(processId))
            delay(DELAY.EVENT_AWAITING_MS)

        return isForcedToTerminateRequest(processId)
    }

    suspend fun terminateRequest(processId: Int): RequestResult.Name
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("[!] Request is being terminated..")

        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

        resumeLogicAfterRequest(processId)
        return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }
    fun isForcedToTerminateRequest(processId: Int)
        = runStatus.isForcedToTerminateThisProcess(processId)





    suspend fun shootEverything(): RequestResult.Name
    {
        var ballCount = storageCells.anyBallCount()
        if (ballCount == NOTHING) return RequestResult.Name.FAIL_IS_EMPTY

        while (ballCount > NOTHING)
        {
            if (!fullWaitForShotFired(DRUM_REQUEST))
                return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED

            ThreadedTelemetry.LAZY_INSTANCE.log("shot finished, updating..")
            ballCount--
        }
        return RequestResult.Name.SUCCESS_IS_NOW_EMPTY
    }



    suspend fun shootEntireRequestCanSkip(
        requestOrder: Array<BallRequest.Name>
    ): RequestResult.Name = shootEntireCanSkipLogic(requestOrder)
    suspend fun shootEntireRequestCanSkip(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>,
        autoUpdateUnfinishedForNextPattern: Boolean = true
    ): RequestResult.Name
    {
        val shootingResult = shootEntireCanSkipLogic(requestOrder)

        if (RequestResult.didSucceed(shootingResult)) return shootingResult
        else if (autoUpdateUnfinishedForNextPattern)
            dynamicMemoryPattern.setTemporary(failsafeOrder)
        return shootEntireCanSkipLogic(failsafeOrder)
    }
    suspend fun shootEntireCanSkipLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = StorageSlot.BOTTOM
        while (i < MAX_BALL_COUNT)
        {
            if (isForcedToTerminateRequest(DRUM_REQUEST))
                return terminateRequest(DRUM_REQUEST)

            val requestResult = storageCells.handleRequest(requestOrder[i])
            shootingResult = shootRequestFinalPhase(requestResult, DRUM_REQUEST)

            if (shootingResult == RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)
                return terminateRequest(DRUM_REQUEST)

            i++
        }
        return shootingResult
    }



    suspend fun shootEntireUntilPatternBreaks(
        requestOrder:  Array<BallRequest.Name>
    ): RequestResult.Name = shootEntireUntilBreaksLogic(requestOrder)
    suspend fun shootEntireUntilPatternBreaks(
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
            dynamicMemoryPattern.setTemporary(failsafeOrder)
        return shootEntireUntilBreaksLogic(failsafeOrder)
    }
    suspend fun shootEntireUntilBreaksLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = StorageSlot.BOTTOM
        while (i < MAX_BALL_COUNT)
        {
            if (isForcedToTerminateRequest(DRUM_REQUEST))
                return terminateRequest(DRUM_REQUEST)

            val requestResult = storageCells.handleRequest(requestOrder[i])
            shootingResult = shootRequestFinalPhase(requestResult, DRUM_REQUEST)

            if (RequestResult.wasTerminated(shootingResult))
                return terminateRequest(DRUM_REQUEST)

            if (RequestResult.didFail(shootingResult))
                i += MAX_BALL_COUNT  //  Fast break if next ball is not present

            i++
        }
        return shootingResult
    }



    suspend fun shootEntireRequestIsValid(
        requestOrder:  Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val countPG = storageCells.ballColorCountPG()
        val requestCountPGA = countPGA(requestOrder)

        if (validateEntireRequestDidSucceed(countPG, requestCountPGA))
            return shootEntireValidRequestLogic(requestOrder)

        return RequestResult.Name.FAIL_NOT_ENOUGH_COLORS
    }
    suspend fun shootEntireRequestIsValid(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>,
        autoUpdateUnfinishedForNextPattern: Boolean = true
    ): RequestResult.Name
    {
        val countPG = storageCells.ballColorCountPG()
        var requestCountPGA = countPGA(requestOrder)

        if (validateEntireRequestDidSucceed(countPG, requestCountPGA))  //  All good
            return shootEntireValidRequestLogic(requestOrder)


        if (autoUpdateUnfinishedForNextPattern)
            dynamicMemoryPattern.setTemporary(failsafeOrder)
        requestCountPGA = countPGA(failsafeOrder)


        if (validateEntireRequestDidSucceed(countPG, requestCountPGA))  //  Failsafe good
            return shootEntireValidRequestLogic(failsafeOrder)

        return RequestResult.Name.FAIL_NOT_ENOUGH_COLORS  //  All bad
    }

    private fun countPGA(requestOrder: Array<BallRequest.Name>): IntArray
    {
        val countPGA = intArrayOf(NOTHING, NOTHING, NOTHING, NOTHING)

        var i = NOTHING
        val ballCountInRequest = min(requestOrder.size, MAX_BALL_COUNT)
        while (i < ballCountInRequest)
        {
            countPGA[
                BallRequest.ShortScale.toShortScale(
                    requestOrder[i])]++
            i++
        }
        return countPGA.copyOf(BallRequest.ShortScale.NONE)
    }
    private fun validateEntireRequestDidSucceed(countPG: IntArray, requestCountPGA: IntArray): Boolean
    {
        val storageDeltaAfterRequests = intArrayOf(
            countPG[BallRequest.ShortScale.PURPLE] - requestCountPGA[BallRequest.ShortScale.PURPLE],
            countPG[BallRequest.ShortScale.GREEN]  - requestCountPGA[BallRequest.ShortScale.GREEN]
        )

        return storageDeltaAfterRequests[BallRequest.ShortScale.PURPLE] >= NOTHING
            && storageDeltaAfterRequests[BallRequest.ShortScale.GREEN]  >= NOTHING

            && storageDeltaAfterRequests[BallRequest.ShortScale.PURPLE] +
               storageDeltaAfterRequests[BallRequest.ShortScale.GREEN] >=
                requestCountPGA[BallRequest.ShortScale.ABSTRACT_ANY]
    }
    suspend fun shootEntireValidRequestLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_UNKNOWN

        var i = StorageSlot.BOTTOM
        while (i < MAX_BALL_COUNT)
        {
            if (isForcedToTerminateRequest(DRUM_REQUEST))
                return terminateRequest(DRUM_REQUEST)

            val requestResult = storageCells.handleRequest(requestOrder[i])
            shootingResult = shootRequestFinalPhase(requestResult, DRUM_REQUEST)

            if (RequestResult.wasTerminated(shootingResult))
                return terminateRequest(DRUM_REQUEST)
            if (RequestResult.didFail(shootingResult)) return shootingResult
            //  Fast break if UNEXPECTED ERROR or TERMINATION

            i++
        }
        return shootingResult
    }





    suspend fun resumeLogicAfterRequest(
        processId: Int,
        doAutoCalibration: Boolean = true)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("SSM: RESUME AFTER REQUEST, process: $processId")

        if (doAutoCalibration)
        {
            ThreadedTelemetry.LAZY_INSTANCE.log("SSM: REVERSING BELTS AFTER SHOT")
            storageCells.hwSortingM.hwReverseBeltsTime(DELAY.ONE_BALL_PUSHING_MS / 2)
            ThreadedTelemetry.LAZY_INSTANCE.log("SSM: FINISHED REVERSING")

            storageCells.hwSortingM.fullCalibrate()
            storageCells.hwSortingM.hwForwardBeltsTime(DELAY.ONE_BALL_PUSHING_MS / 2)
        }
        else storageCells.hwSortingM.fullCalibrate()

        ThreadedTelemetry.LAZY_INSTANCE.log("SSM: phase 2 RESUME AFTER REQUEST, process: $processId")
        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)
        runStatus.clearCurrentActiveProcess()
        storageCells.resumeIntakes()
    }
    fun resumeLogicAfterIntake(processId: Int)
    {
        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)
        runStatus.clearCurrentActiveProcess()
    }



    fun shotWasFired() = shotWasFired.set(true)
    suspend fun fullWaitForShotFired(processId: Int): Boolean
    {
        storageCells.hwSortingM.openTurretGate()
        ThreadedTelemetry.LAZY_INSTANCE.log("SSM waiting for shot - event send")
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageRequestIsReadyEvent())

        var timePassedWaitingForShot: Long = NOTHING.toLong()

        while (!shotWasFired.get() &&
            timePassedWaitingForShot < DELAY.MAX_SHOT_AWAITING_MS)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            timePassedWaitingForShot += DELAY.EVENT_AWAITING_MS
            if (isForcedToTerminateRequest(processId)) return false
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("SSM - DONE waiting for shot")
        ThreadedTelemetry.LAZY_INSTANCE.log("SSM: fired? ${shotWasFired.get()}," +
                " delta time: $timePassedWaitingForShot")

        storageCells.hwSortingM.hwReverseBeltsTime(DELAY.ONE_BALL_PUSHING_MS)
        storageCells.updateAfterRequest()
        dynamicMemoryPattern.removeFromTemporary()
        shotWasFired.set(false)
        return true
    }
}