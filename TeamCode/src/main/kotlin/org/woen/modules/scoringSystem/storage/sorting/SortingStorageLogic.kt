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
import org.woen.modules.scoringSystem.storage.Alias.NOTHING
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT

import org.woen.telemetry.LogManager
import org.woen.telemetry.Configs.DELAY
import org.woen.utils.process.RunStatus
import org.woen.threading.ThreadedEventBus

import org.woen.telemetry.Configs.PROCESS_ID.DRUM_REQUEST
import org.woen.telemetry.Configs.PROCESS_ID.UPDATE_AFTER_LAZY_INTAKE
import org.woen.telemetry.Configs.PROCESS_ID.PREDICT_SORT
import org.woen.telemetry.Configs.PROCESS_ID.STORAGE_CALIBRATION

import org.woen.telemetry.Configs.PROCESS_ID.PRIORITY_SETTING_FOR_SORTING_STORAGE

import org.woen.telemetry.Configs.DEBUG_LEVELS.SSL_DEBUG_LEVELS
import org.woen.telemetry.Configs.DEBUG_LEVELS.SSL_DEBUG_SETTING
import org.woen.telemetry.Configs.DEBUG_LEVELS.EVENTS_FEEDBACK
import org.woen.telemetry.Configs.DEBUG_LEVELS.ATTEMPTING_LOGIC
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_STARTING
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_ENDING
import org.woen.telemetry.Configs.DEBUG_LEVELS.GENERIC_INFO
import org.woen.telemetry.Configs.DEBUG_LEVELS.LOGIC_STEPS
import org.woen.telemetry.Configs.DEBUG_LEVELS.PROCESS_NAME
import org.woen.telemetry.Configs.DEBUG_LEVELS.TERMINATION



class SortingStorageLogic
{
    val storageCells           = StorageCells()
    val dynamicMemoryPattern   = DynamicPattern()

    val shotWasFired           = AtomicBoolean(false)
    val lazyIntakeIsActive     = AtomicBoolean(false)
    val pleaseWaitForIntakeEnd = AtomicBoolean(false)

    val runStatus = RunStatus(PRIORITY_SETTING_FOR_SORTING_STORAGE)
    val logM = LogManager(SSL_DEBUG_SETTING,
        SSL_DEBUG_LEVELS, "SSL")


    fun resetParametersToDefault()
    {
        dynamicMemoryPattern.fullReset()
        shotWasFired.set(false)

        runStatus.fullResetToActiveState()
        logM.updateDebugSetting(SSL_DEBUG_SETTING)
        logM.setShowedDebugLevels(SSL_DEBUG_LEVELS)
    }



    suspend fun canInitiatePredictSort(): Boolean
    {
        logM.logMd("Try initiating predict sort", ATTEMPTING_LOGIC)

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


    suspend fun canStartStorageCalibrationWithCurrent(): Boolean
    {
        logM.logMd("Try starting storage calibration with current", ATTEMPTING_LOGIC)

        if (runStatus.isUsedByAnyProcess()) return false

        runStatus.addProcessToQueue(STORAGE_CALIBRATION)
        delay(DELAY.STORAGE_CALIBRATION_RACE_CONDITION_MS)

        return runStatus.isThisProcessHighestPriority(STORAGE_CALIBRATION)
    }
    suspend fun safeStartStorageCalibrationWithCurrent()
    {
        runStatus.setCurrentActiveProcess(STORAGE_CALIBRATION)

        storageCells.safeFillWithUnknown()

        runStatus.clearCurrentActiveProcess()
        runStatus.safeRemoveThisProcessIdFromQueue(STORAGE_CALIBRATION)
    }


    suspend fun canStartUpdateAfterLazyIntake(): Boolean
    {
        logM.logMd("Try starting updating after lazy intake", ATTEMPTING_LOGIC)

        if (runStatus.isUsedByAnyProcess()) return false

        runStatus.addProcessToQueue(UPDATE_AFTER_LAZY_INTAKE)
        delay(DELAY.LAZY_INTAKE_RACE_CONDITION_MS)

        return runStatus.isThisProcessHighestPriority(UPDATE_AFTER_LAZY_INTAKE)
    }
    fun trySafeStartUpdateAfterLazyIntake(inputFromTurretSlotToBottom: Array<Ball.Name>)
    {
        runStatus.setCurrentActiveProcess(UPDATE_AFTER_LAZY_INTAKE)

        storageCells.safeUpdateAfterLazyIntake(inputFromTurretSlotToBottom)

        runStatus.clearCurrentActiveProcess()
        runStatus.safeRemoveThisProcessIdFromQueue(UPDATE_AFTER_LAZY_INTAKE)
    }



    suspend fun safeSortIntake(intakeResult: IntakeResult, inputToBottomSlot: Ball.Name): IntakeResult.Name
    {
        if (intakeResult.didFail()) return intakeResult.name()   //  Intake failed

        pleaseWaitForIntakeEnd.set(true)
        logM.logMd("Sorting intake", PROCESS_NAME)

        storageCells.hwReAdjustStorage()
        storageCells.hwSortingM.hwForwardBeltsTime(Delay.HALF_PUSH)

        storageCells.updateAfterIntake(inputToBottomSlot)
        return Intake.SUCCESS
    }

    private suspend fun intakeRaceConditionIsPresent(processId: Int):  Boolean
    {
        logM.logMd("CHECKING RACE CONDITION", PROCESS_NAME)
        if (runStatus.isUsedByAnyProcess()) return true

        logM.logMd("Currently not busy", GENERIC_INFO)
        runStatus.addProcessToQueue(processId)
        delay(DELAY.INTAKE_RACE_CONDITION_MS)

        logM.logMd("Highest processId: " +
                "${runStatus.getHighestPriorityProcessId()}",
            GENERIC_INFO)
        return !runStatus.isThisProcessHighestPriority(processId)
    }
    suspend fun noIntakeRaceConditionProblems(processId: Int): Boolean
    {
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

        return !intakeRaceConditionIsPresent(processId)
    }

    fun terminateIntake(processId: Int): IntakeResult.Name
    {
        logM.logMd("Intake is being terminated", TERMINATION)

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

        logM.logMd("rotating cur slot", LOGIC_STEPS)
        if (fullRotations >= NOTHING)
            repeat(fullRotations)
                { storageCells.fullRotate() }


        logM.logMd("sorting finished - success", PROCESS_ENDING)
        logM.logMd("Getting ready to shoot",     PROCESS_STARTING)
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

        logM.logMd("Finished updating", PROCESS_ENDING)

        return if (updateResult.didSucceed())
        {
            storageCells.hwSortingM.helpPushLastBall.set(
                storageCells.onlyOneBallLeft())

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
        logM.logMd("Request is being terminated", TERMINATION)

        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

        resumeLogicAfterRequest(processId)
        return Request.TERMINATED
    }
    fun isForcedToTerminate(processId: Int)
        = runStatus.isForcedToTerminateThisProcess(processId)





    suspend fun lazyShootEverything():    RequestResult.Name
    {
        var shotsFired = NOTHING
        while (shotsFired < MAX_BALL_COUNT)
        {
            storageCells.hwSortingM.helpPushLastBall.set(
                shotsFired == MAX_BALL_COUNT - 1)

            if (!fullWaitForShotFired(
                    DRUM_REQUEST,
                    false))
                return Request.TERMINATED

            logM.logMd("shot finished, updating..", PROCESS_ENDING)
            shotsFired++
        }

        return Request.SUCCESS_NOW_EMPTY
    }
    suspend fun shootEverything(): RequestResult.Name
    {
        var ballCount = storageCells.anyBallCount()
        if (ballCount == NOTHING) return Request.FAIL_IS_EMPTY

        while (ballCount > NOTHING)
        {
            storageCells.hwSortingM.helpPushLastBall.set(ballCount == 1)

            if (!fullWaitForShotFired(
                    DRUM_REQUEST,
                    false))
                return Request.TERMINATED

            logM.logMd("shot finished, updating..", PROCESS_ENDING)
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
            min(requested.size, MAX_BALL_COUNT),
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
        var trimmedRequestSize   = min(requested.size, MAX_BALL_COUNT)

        val shootingResult = shootEntireCanSkipLogic(
                requested, trimmedRequestSize,
                isNowPerfectlySorted,
                autoUpdatePatternWhenSucceed)

        if (Request.didSucceed(shootingResult) ||
            Request.wasTerminated(shootingResult))
            return shootingResult
        else if (autoUpdateUnfinishedWithFailsafe) dynamicMemoryPattern.setTemporary(failsafe)

        isNowPerfectlySorted = storageCells.tryInitiatePredictSort(failsafe)
        trimmedRequestSize   = min(failsafe.size, MAX_BALL_COUNT)

        return shootEntireCanSkipLogic(
                failsafe, trimmedRequestSize,
                isNowPerfectlySorted,
                autoUpdateUnfinishedWithFailsafe)
    }
    suspend fun shootEntireCanSkipLogic(
        requested: Array<BallRequest.Name>,
        trimmedRequestSize: Int,
        isNowPerfectlySorted:         Boolean = false,
        autoUpdatePatternWhenSucceed: Boolean = true): RequestResult.Name
    {
        var shootingResult  = Request.COLOR_NOT_PRESENT
        var curRequestId    = NOTHING

        while (curRequestId < trimmedRequestSize)
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
            min(requested.size, MAX_BALL_COUNT),
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
        var trimmedRequestSize   = min(requested.size, MAX_BALL_COUNT)

        val shootingResult = shootEntireUntilBreaksLogic(
                requested, trimmedRequestSize,
                isNowPerfectlySorted,
                autoUpdatePatternWhenSucceed)

        if (Request.didSucceed   (shootingResult) ||
            Request.wasTerminated(shootingResult))
            return shootingResult
        else if (autoUpdateUnfinishedWithFailsafe)
            dynamicMemoryPattern.setTemporary(failsafe)

        isNowPerfectlySorted = storageCells.tryInitiatePredictSort(failsafe)
        trimmedRequestSize   = min(failsafe.size, MAX_BALL_COUNT)

        return shootEntireUntilBreaksLogic(
                failsafe, trimmedRequestSize,
                isNowPerfectlySorted,
                autoUpdateUnfinishedWithFailsafe)
    }
    suspend fun shootEntireUntilBreaksLogic(
        requested: Array<BallRequest.Name>,
        trimmedRequestSize: Int,
        isNowPerfectlySorted:         Boolean = false,
        autoUpdatePatternWhenSucceed: Boolean = true): RequestResult.Name
    {
        var shootingResult  = Request.COLOR_NOT_PRESENT
        var curRequestId    = NOTHING

        while (curRequestId < trimmedRequestSize)
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
        val curStoragePG = storageCells.ballCountPGA()
        val requestPGA   = countPGA(requested)

        if (canCompleteEntireRequest(curStoragePG, requestPGA))
            return shootEntireValidRequestLogic(
                requested,
                min(requested.size, MAX_BALL_COUNT),
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
        val curStoragePG = storageCells.ballCountPGA()
        var requestPGA   = countPGA(requested)

        if (canCompleteEntireRequest(curStoragePG, requestPGA))  //  All good
            return shootEntireValidRequestLogic(
                requested,
                min(requested.size, MAX_BALL_COUNT),
                storageCells
                    .tryInitiatePredictSort(requested),
                autoUpdatePatternWhenSucceed)

        if (autoUpdateUnfinishedWithFailsafe)
            dynamicMemoryPattern.setTemporary(failsafe)

        requestPGA = countPGA(failsafe)

        if (canCompleteEntireRequest(curStoragePG, requestPGA))  //  Failsafe good
            return shootEntireValidRequestLogic(
                failsafe,
                min(failsafe.size, MAX_BALL_COUNT),
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
            intPGAN[
                BallRequest.toAbstractBallId
                (
                    order[curRequestId]
                )
            ]++

            curRequestId++
        }
        return storageCells.toCountPGA(intPGAN)
    }
    private fun canCompleteEntireRequest(curStoragePG: CountPGA, requestPGA: CountPGA): Boolean
    {
        val futureStorage = CountPGA(
            curStoragePG.purple - requestPGA.purple,
            curStoragePG.green  - requestPGA.green,
              curStoragePG.any)

        return futureStorage.purple >= NOTHING
            && futureStorage.green  >= NOTHING

            && futureStorage.purple +
               futureStorage.green  +
               futureStorage.any   >= requestPGA.any
    }
    suspend fun shootEntireValidRequestLogic(
        requested: Array<BallRequest.Name>,
        trimmedRequestSize: Int,
        isNowPerfectlySorted:         Boolean = false,
        autoUpdatePatternWhenSucceed: Boolean): RequestResult.Name
    {
        var shootingResult  = Request.FAIL_UNKNOWN
        var curRequestId    = NOTHING

        while (curRequestId < trimmedRequestSize)
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
        logM.logMd("RESUME AFTER REQUEST, process: $processId", PROCESS_NAME)

        if (doAutoCalibration)
        {
            logM.logMd("Reversing belts for calibration", PROCESS_STARTING)
            storageCells.hwSortingM.hwReverseBeltsTime(Delay.HALF_PUSH)
            logM.logMd("Finished reversing", PROCESS_ENDING)

            logM.logMd("Starting calibration", PROCESS_STARTING)
            storageCells.hwSortingM.fullCalibrate()
            storageCells.hwSortingM.hwForwardBeltsTime(Delay.HALF_PUSH)
        }
        else storageCells.hwSortingM.fullCalibrate()


        logM.logMd("Phase 2 - RESUME AFTER REQUEST, process: $processId", LOGIC_STEPS)

        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)
        runStatus.clearCurrentActiveProcess()
        storageCells.resumeIntakes()

        logM.logMd("FINISHED resume logic", PROCESS_ENDING)
    }
    fun resumeLogicAfterIntake(processId: Int)
    {
        pleaseWaitForIntakeEnd.set(false)
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
        logM.logMd("waiting for shot - event send", EVENTS_FEEDBACK)
        ThreadedEventBus.LAZY_INSTANCE.invoke(Request.IsReadyEvent)

        var timePassedWaiting: Long = NOTHING.toLong()

        while (!shotWasFired.get() &&
            timePassedWaiting < DELAY.SSM_MAX_SHOT_AWAITING_MS)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            timePassedWaiting += DELAY.EVENT_AWAITING_MS
            if (isForcedToTerminate(processId)) return false
        }

        logM.logMd("DONE waiting for shot", PROCESS_ENDING)
        logM.logMd("fired? ${shotWasFired.get()}," +
                " delta time: $timePassedWaiting", GENERIC_INFO)

        shotWasFired.set(false)
        //storageCells.hwSortingM.hwReverseBeltsTime(Delay.FULL_PUSH)
        storageCells.updateAfterRequest()

        if (autoUpdatePatternWhenSucceed)
            dynamicMemoryPattern.removeFromTemporary()

        return true
    }
}