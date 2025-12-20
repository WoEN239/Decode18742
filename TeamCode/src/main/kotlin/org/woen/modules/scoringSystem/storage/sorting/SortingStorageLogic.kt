package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult

import org.woen.modules.scoringSystem.storage.Alias.Delay
import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.ShortScale

import org.woen.utils.process.RunStatus

import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedEventBus

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
        ThreadedTelemetry.LAZY_INSTANCE.log("SSM: Try initiating predict sort")

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
            return Intake.STARTED_SUCCESSFULLY

        resumeLogicAfterIntake(LAZY_INTAKE)
        return Intake.IS_BUSY
    }
    suspend fun safeSortIntake(intakeResult: IntakeResult, inputBall: Ball.Name): IntakeResult.Name
    {
        if (intakeResult.didFail()) return intakeResult.name()   //  Intake failed

        ThreadedTelemetry.LAZY_INSTANCE.log("SSM - Sorting intake")
        storageCells.hwReAdjustStorage()
        storageCells.hwSortingM.hwForwardBeltsTime(Delay.HALF_PUSH)

        storageCells.updateAfterIntake(inputBall)
        return Intake.SUCCESS
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
        return Intake.TERMINATED
    }




    private suspend fun rotateToFoundBall(
        requestResult: RequestResult,
        processId: Int): RequestResult
    {
        if (requestResult.didFail()) return requestResult  //  Request search failed
        if (isForcedToTerminate(processId))
            return Request.F_TERMINATED

        val fullRotations = when (requestResult.name())
        {
            Request.MOBILE_SLOT -> 3
            Request.BOTTOM_SLOT -> 2
            Request.CENTER_SLOT -> 1
            else -> -1
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("updating: rotating cur slot")
        if (fullRotations >= NOTHING)
            repeat(fullRotations)
                { storageCells.fullRotate() }


        ThreadedTelemetry.LAZY_INSTANCE.log("sorting finished - success", "Getting ready to shoot")
        return Request.F_SUCCESS
    }
    suspend fun shootRequestFinalPhase(
        requestResult: RequestResult,
        processId: Int,
        isNowPerfectlySorted:         Boolean = false,
        autoUpdatePatternWhenSucceed: Boolean = false): RequestResult.Name
    {
        if (requestResult.didFail()) return requestResult.name()
        if (isForcedToTerminate(processId))
            return Request.TERMINATED

        val updateResult = if (!isNowPerfectlySorted)
            rotateToFoundBall(requestResult, processId)
        else Request.TURRET_SLOT

        ThreadedTelemetry.LAZY_INSTANCE.log("SSML: Finished updating")

        return if (updateResult.didSucceed())
        {
            if (!fullWaitForShotFired(processId, autoUpdatePatternWhenSucceed))
                 Request.TERMINATED
            else if (storageCells.isNotEmpty())
                 Request.SUCCESS
            else Request.SUCCESS_NOW_EMPTY
        }
        else updateResult.name()
    }

    private suspend fun requestRaceConditionIsPresent(processId: Int):  Boolean
    {
        if (isForcedToTerminate(processId)) return false
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

        return isForcedToTerminate(processId)
    }

    suspend fun terminateRequest(processId: Int): RequestResult.Name
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("[!] Request is being terminated..")

        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

        resumeLogicAfterRequest(processId)
        return Request.TERMINATED
    }
    fun isForcedToTerminate(processId: Int)
        = runStatus.isForcedToTerminateThisProcess(processId)





    suspend fun shootEverything(): RequestResult.Name
    {
        var ballCount = storageCells.anyBallCount()
        if (ballCount == NOTHING) return Request.FAIL_IS_EMPTY

        while (ballCount > NOTHING)
        {
            if (!fullWaitForShotFired(
                    DRUM_REQUEST,
                    false))
                return Request.TERMINATED

            ThreadedTelemetry.LAZY_INSTANCE.log("shot finished, updating..")
            ballCount--
        }
        return Request.SUCCESS_NOW_EMPTY
    }



    suspend fun shootEntireCanSkip(
        requested: Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed: Boolean = true
    ): RequestResult.Name
        = shootEntireCanSkipLogic(
            requested,
            storageCells
                .tryInitiatePredictSort(requested),
            autoUpdatePatternWhenSucceed)
    suspend fun shootEntireCanSkip(
        requested: Array<BallRequest.Name>,
        failsafe:  Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed:     Boolean = true,
        autoUpdateUnfinishedWithFailsafe: Boolean = true
    ): RequestResult.Name
    {
        var isNowPerfectlySorted = storageCells.tryInitiatePredictSort(requested)

        val shootingResult = shootEntireCanSkipLogic(
            requested, isNowPerfectlySorted,
            autoUpdatePatternWhenSucceed)

        if (Request.didSucceed(shootingResult) ||
            Request.wasTerminated(shootingResult))
            return shootingResult
        else if (autoUpdateUnfinishedWithFailsafe) dynamicMemoryPattern.setTemporary(failsafe)

        isNowPerfectlySorted = storageCells.tryInitiatePredictSort(failsafe)

        return shootEntireCanSkipLogic(
            failsafe,
            isNowPerfectlySorted,
            autoUpdateUnfinishedWithFailsafe)
    }
    suspend fun shootEntireCanSkipLogic(
        requested: Array<BallRequest.Name>,
        isNowPerfectlySorted:         Boolean = false,
        autoUpdatePatternWhenSucceed: Boolean = true): RequestResult.Name
    {
        var shootingResult = Request.COLOR_NOT_PRESENT

        var curRequestId = NOTHING
        while (curRequestId < MAX_BALL_COUNT)
        {
            if (isForcedToTerminate(DRUM_REQUEST))
                return Request.TERMINATED

            val requestResult = if (!isNowPerfectlySorted)
                     storageCells.handleRequest(requested[curRequestId])
                else Request.TURRET_SLOT

            shootingResult = shootRequestFinalPhase(
                requestResult, DRUM_REQUEST,
                isNowPerfectlySorted, autoUpdatePatternWhenSucceed)

            curRequestId++
        }
        return shootingResult
    }



    suspend fun shootEntireUntilPatternBreaks(
        requested: Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed: Boolean
    ): RequestResult.Name
        = shootEntireUntilBreaksLogic(
            requested,
            storageCells
                .tryInitiatePredictSort(requested),
            autoUpdatePatternWhenSucceed)
    suspend fun shootEntireUntilPatternBreaks(
        requested: Array<BallRequest.Name>,
        failsafe:  Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed:     Boolean = true,
        autoUpdateUnfinishedWithFailsafe: Boolean = true
    ): RequestResult.Name
    {
        var isNowPerfectlySorted = storageCells.tryInitiatePredictSort(requested)

        val shootingResult = shootEntireUntilBreaksLogic(
            requested, isNowPerfectlySorted,
            autoUpdatePatternWhenSucceed)

        if (Request.didSucceed   (shootingResult) ||
            Request.wasTerminated(shootingResult))
            return shootingResult
        else if (autoUpdateUnfinishedWithFailsafe)
            dynamicMemoryPattern.setTemporary(failsafe)

        isNowPerfectlySorted = storageCells.tryInitiatePredictSort(failsafe)

        return shootEntireUntilBreaksLogic(
            failsafe, isNowPerfectlySorted,
            autoUpdateUnfinishedWithFailsafe)
    }
    suspend fun shootEntireUntilBreaksLogic(
        requested: Array<BallRequest.Name>,
        isNowPerfectlySorted:         Boolean = false,
        autoUpdatePatternWhenSucceed: Boolean = true): RequestResult.Name
    {
        var shootingResult = Request.COLOR_NOT_PRESENT

        var curRequestId = NOTHING
        while (curRequestId < MAX_BALL_COUNT)
        {
            if (isForcedToTerminate(DRUM_REQUEST))
                return Request.TERMINATED

            val requestResult = if (!isNowPerfectlySorted)
                    storageCells.handleRequest(requested[curRequestId])
                else Request.TURRET_SLOT

            shootingResult = shootRequestFinalPhase(
                requestResult, DRUM_REQUEST,
                isNowPerfectlySorted, autoUpdatePatternWhenSucceed)

            if (Request.wasTerminated(shootingResult))
                return Request.TERMINATED

            if (Request.didFail(shootingResult))
                curRequestId += MAX_BALL_COUNT  //  Fast break if next ball is not present

            curRequestId++
        }
        return shootingResult
    }



    suspend fun shootEntireRequestIsValid(
        requested: Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed: Boolean = true
    ): RequestResult.Name
    {
        val curStoragePG = storageCells.ballCountPG()
        val requestPGA   = countPGA(requested)

        if (canCompleteEntireRequest(curStoragePG, requestPGA))
            return shootEntireValidRequestLogic(
                requested,
                storageCells
                    .tryInitiatePredictSort(requested),
                autoUpdatePatternWhenSucceed)

        return Request.NOT_ENOUGH_COLORS
    }
    suspend fun shootEntireRequestIsValid(
        requested: Array<BallRequest.Name>,
        failsafe:  Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed:     Boolean = true,
        autoUpdateUnfinishedWithFailsafe: Boolean = true
    ): RequestResult.Name
    {
        val curStoragePG = storageCells.ballCountPG()
        var requestPGA   = countPGA(requested)

        if (canCompleteEntireRequest(curStoragePG, requestPGA))  //  All good
            return shootEntireValidRequestLogic(
                requested,
                storageCells
                    .tryInitiatePredictSort(requested),
                autoUpdatePatternWhenSucceed)

        if (autoUpdateUnfinishedWithFailsafe)
            dynamicMemoryPattern.setTemporary(failsafe)

        requestPGA = countPGA(failsafe)

        if (canCompleteEntireRequest(curStoragePG, requestPGA))  //  Failsafe good
            return shootEntireValidRequestLogic(
                failsafe,
                storageCells
                    .tryInitiatePredictSort(failsafe),
                autoUpdateUnfinishedWithFailsafe)

        return Request.NOT_ENOUGH_COLORS
    }

    private fun countPGA(order: Array<BallRequest.Name>): CountPGA
    {
        val intPGAN = intArrayOf(NOTHING, NOTHING, NOTHING, NOTHING)
        //  Purple, Green, AbstractAny, Nothing

        var curRequestId = NOTHING
        val ballCountInRequest = min(order.size, MAX_BALL_COUNT)
        while (curRequestId < ballCountInRequest)
        {
            intPGAN[ShortScale.to(order[curRequestId])]++
            curRequestId++
        }
        return storageCells.toCountPGA(intPGAN)
    }
    private fun canCompleteEntireRequest(curStoragePG: CountPGA, requestPGA: CountPGA): Boolean
    {
        val futureStorage = CountPGA(
            curStoragePG.purple - requestPGA.purple,
            curStoragePG.green  - requestPGA.green)

        return futureStorage.purple >= NOTHING
            && futureStorage.green  >= NOTHING

            && futureStorage.purple + futureStorage.green >= requestPGA.any
    }
    suspend fun shootEntireValidRequestLogic(
        requested: Array<BallRequest.Name>,
        isNowPerfectlySorted:         Boolean = false,
        autoUpdatePatternWhenSucceed: Boolean): RequestResult.Name
    {
        var shootingResult = Request.FAIL_UNKNOWN

        var curRequestId = NOTHING
        while (curRequestId < MAX_BALL_COUNT)
        {
            if (isForcedToTerminate(DRUM_REQUEST))
                return Request.TERMINATED

            val requestResult = if (!isNowPerfectlySorted)
                storageCells.handleRequest(requested[curRequestId])
            else Request.TURRET_SLOT

            shootingResult = shootRequestFinalPhase(
                requestResult, DRUM_REQUEST,
                isNowPerfectlySorted, autoUpdatePatternWhenSucceed)

            if (Request.wasTerminated(shootingResult))
                return Request.TERMINATED

            if (Request.didFail(shootingResult))
                return shootingResult

            curRequestId++
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
            storageCells.hwSortingM.hwReverseBeltsTime(Delay.HALF_PUSH)
            ThreadedTelemetry.LAZY_INSTANCE.log("SSM: FINISHED REVERSING")

            storageCells.hwSortingM.fullCalibrate()
            storageCells.hwSortingM.hwForwardBeltsTime(Delay.HALF_PUSH)
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
    suspend fun fullWaitForShotFired(
        processId: Int,
        autoUpdatePatternWhenSucceed : Boolean = true
    ): Boolean
    {
        storageCells.hwSortingM.openTurretGate()
        ThreadedTelemetry.LAZY_INSTANCE.log("SSM waiting for shot - event send")
        ThreadedEventBus.LAZY_INSTANCE.invoke(Request.IsReadyEvent)

        var timePassedWaiting: Long = NOTHING.toLong()

        while (!shotWasFired.get() &&
            timePassedWaiting < DELAY.MAX_SHOT_AWAITING_MS)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            timePassedWaiting += DELAY.EVENT_AWAITING_MS
            if (isForcedToTerminate(processId)) return false
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("SSM - DONE waiting for shot")
        ThreadedTelemetry.LAZY_INSTANCE.log("SSM: fired? ${shotWasFired.get()}," +
                " delta time: $timePassedWaiting")

        shotWasFired.set(false)
        storageCells.hwSortingM.hwReverseBeltsTime(Delay.FULL_PUSH)
        storageCells.updateAfterRequest()

        if (autoUpdatePatternWhenSucceed)
            dynamicMemoryPattern.removeFromTemporary()

        return true
    }
}