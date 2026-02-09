package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult
import org.woen.modules.light.Light.LightColor
import org.woen.modules.light.SetLightColorEvent

import org.woen.modules.scoringSystem.storage.FullFinishedIntakeEvent

import org.woen.modules.scoringSystem.storage.Alias.Delay
import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.NOTHING
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT

import org.woen.utils.process.RunStatus
import org.woen.telemetry.LogManager

import org.woen.telemetry.configs.Configs.DELAY
import org.woen.telemetry.configs.Configs.DELAY.BETWEEN_SHOTS_MS

import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.DO_WAIT_BEFORE_NEXT_SHOT

import org.woen.telemetry.configs.Configs.PROCESS_ID.INTAKE
import org.woen.telemetry.configs.Configs.PROCESS_ID.RUNNING_INTAKE_INSTANCE
import org.woen.telemetry.configs.Configs.PROCESS_ID.UPDATE_AFTER_LAZY_INTAKE
import org.woen.telemetry.configs.Configs.PROCESS_ID.DRUM_REQUEST
import org.woen.telemetry.configs.Configs.PROCESS_ID.PREDICT_SORT
import org.woen.telemetry.configs.Configs.PROCESS_ID.STORAGE_CALIBRATION

import org.woen.telemetry.configs.Configs.PROCESS_ID.PRIORITY_SETTING_FOR_SORTING_STORAGE

import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.SSL_DEBUG_LEVELS
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.SSL_DEBUG_SETTING
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.EVENTS_FEEDBACK
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.RACE_CONDITION
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.ATTEMPTING_LOGIC
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.PROCESS_STARTING
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.PROCESS_ENDING
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.GENERIC_INFO
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.LOGIC_STEPS
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.PROCESS_NAME
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.TERMINATION

import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.ALWAYS_TRY_PREDICT_SORTING
import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.TRY_ADDITIONAl_PREDICT_SORTING_WHILE_SHOOTING



class SortingStorageLogic
{
    val storageCells           = StorageCells()
    val dynamicMemoryPattern   = DynamicPattern()

    val canShoot     = AtomicBoolean(false)
    val shotWasFired = AtomicBoolean(false)

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
    fun safeUpdateAfterLazyIntake(inputFromTurretSlotToBottom: Array<Ball.Name>)
    {
        runStatus.setCurrentActiveProcess(UPDATE_AFTER_LAZY_INTAKE)

        storageCells.updateAfterLazyIntake(inputFromTurretSlotToBottom)

        runStatus.clearCurrentActiveProcess()
        runStatus.safeRemoveThisProcessIdFromQueue(UPDATE_AFTER_LAZY_INTAKE)
    }



    suspend fun safeSortIntake(inputBall: Ball.Name): IntakeResult.Name
    {
        if (storageCells.alreadyFull()) return Intake.FAIL_IS_FULL   //  Intake failed

        runStatus.addProcessToQueue(RUNNING_INTAKE_INSTANCE)

        logM.logMd("Sorting intake", LOGIC_STEPS)
        storageCells.updateAfterIntake(inputBall)

        runStatus.safeRemoveOnlyOneInstanceOfThisProcessFromQueue(
            RUNNING_INTAKE_INSTANCE)

        return Intake.SUCCESS
    }

    private suspend fun intakeRaceConditionIsPresent(
        processId: Int,
        vararg exceptionProcessesId: Int):  Boolean
    {
        logM.logMd("CHECKING RACE CONDITION", RACE_CONDITION)
        if (runStatus.isUsedByAnotherProcess(
            processId, *exceptionProcessesId)) return true

        logM.logMd("Currently not busy", RACE_CONDITION)
        runStatus.addProcessToQueue(processId)
        delay(DELAY.INTAKE_RACE_CONDITION_MS)

        logM.logMd("Highest processId: " +
                "${runStatus.getHighestPriorityProcessId(*exceptionProcessesId)}",
            RACE_CONDITION)

        return !runStatus.isThisProcessHighestPriority(
            processId, *exceptionProcessesId)
    }
    suspend fun noIntakeRaceConditionProblems(
        processId: Int,
        vararg exceptionProcessesId: Int): Boolean
    {
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

        return !intakeRaceConditionIsPresent(processId,
                *exceptionProcessesId)
    }



    private suspend fun rotateToFoundBall(
        requestResult: RequestResult,
        processId: Int,
        doExtraRecalibration: Boolean): RequestResult
    {
        if (requestResult.didFail()) return requestResult  //  Request search failed
        if (isForcedToTerminate(processId))
            return Request.F_TERMINATED

        if (doExtraRecalibration)
        {
            storageCells.hwSortingM.reverseBeltsTime(Delay.PART_PUSH)
            storageCells.hwSortingM.closeTurretGate()
            storageCells.hwSortingM.slowStartBelts()
            delay(Delay.HALF_PUSH)
            storageCells.hwSortingM.stopBelts()
        }

        val fullRotations = when (requestResult.name())
        {
            Request.MOBILE_SLOT -> 2
            Request.BOTTOM_SLOT -> 2
            Request.CENTER_SLOT -> 1
            else -> -1
        }

        logM.logMd("rotating cur slot times: $fullRotations", LOGIC_STEPS)
        repeat(fullRotations)
            { storageCells.fullRotate() }

        logM.logMd("sorting finished - success", PROCESS_ENDING)
        logM.logMd("Getting ready to shoot",     PROCESS_STARTING)
        return Request.F_SUCCESS
    }
    suspend fun shootRequestFinalPhase(
        requestResult: RequestResult,
        processId: Int,
        autoUpdatePatternWhenSucceed: Boolean = false): RequestResult.Name
    {
        if (requestResult.didFail()) return requestResult.name()
        if (isForcedToTerminate(processId))
            return Request.TERMINATED

        val updateResult = if (requestResult == Request.TURRET_SLOT) Request.TURRET_SLOT
            else rotateToFoundBall(requestResult, processId, true)

        logM.logMd("Finished updating", PROCESS_ENDING)

        return if (updateResult.didSucceed())
        {
            val isLastBall = storageCells.onlyOneBallLeft()
            storageCells.hwSortingM.helpPushLastBall.set(isLastBall)

            val doWaitBeforeNextShot = DO_WAIT_BEFORE_NEXT_SHOT && !isLastBall

            if (!fullWaitForShotFired(processId, doWaitBeforeNextShot, autoUpdatePatternWhenSucceed))
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
        storageCells.hwSortingM.stopAwaitingEating(true)

        delay(DELAY.REQUEST_RACE_CONDITION_MS)
        return !runStatus.isThisProcessHighestPriority(processId)
    }
    suspend fun cantHandleRequestRaceCondition(processId: Int): Boolean
    {
        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

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
    @Synchronized
    fun isForcedToTerminate(processId: Int)
        = runStatus.isForcedToTerminateThisProcess(processId)






    suspend fun lazyStreamDrumRequest(ballCount: Int)
    {
        logM.logMd("Starting Lazy stream shooting", PROCESS_STARTING)

        val beltPushTime = when (ballCount)
        {
            3    -> DELAY.FIRE_3_BALLS_FOR_SHOOTING_MS
            2    -> DELAY.FIRE_2_BALLS_FOR_SHOOTING_MS
            else -> DELAY.FIRE_1_BALLS_FOR_SHOOTING_MS
        }

        logM.logMd("Firing time: $beltPushTime", GENERIC_INFO)

        storageCells.hwSortingM.stopBelts()
        storageCells.hwSortingM.openTurretGate()

        storageCells.hwSortingM.shootStartBelts()
        delay(beltPushTime)
        storageCells.hwSortingM.pushLastBallFast()

        storageCells.fullEmptyStorageCells()
    }
    suspend fun fastStreamDrumRequest(): RequestResult.Name
    {
        val ballCount = storageCells.anyBallCount()
        logM.logMd("Expected shot count: $ballCount", GENERIC_INFO)

        lazyStreamDrumRequest(ballCount)
        return Request.SUCCESS_NOW_EMPTY
    }



    suspend fun shootDrumCanSkip(
        requested: Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed: Boolean = true
    ): RequestResult.Name
        = shootDrumCanSkipLogic(
            requested,
            min(requested.size, MAX_BALL_COUNT),
            storageCells
                .tryInitiatePredictSort(requested),
            autoUpdatePatternWhenSucceed)
    suspend fun shootDrumCanSkip(
        requested: Array<BallRequest.Name>,
        failsafe:  Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed:     Boolean = true,
        autoUpdateUnfinishedWithFailsafe: Boolean = true
    ): RequestResult.Name
    {
        var isNowPerfectlySorted = storageCells.tryInitiatePredictSort(requested)
        var trimmedRequestSize   = min(requested.size, MAX_BALL_COUNT)

        val shootingResult = shootDrumCanSkipLogic(
                requested, trimmedRequestSize,
                isNowPerfectlySorted,
                autoUpdatePatternWhenSucceed)

        if (Request.didSucceed(shootingResult) ||
            Request.wasTerminated(shootingResult))
            return shootingResult
        else if (autoUpdateUnfinishedWithFailsafe) dynamicMemoryPattern.setTemporary(failsafe)

        isNowPerfectlySorted = storageCells.tryInitiatePredictSort(failsafe)
        trimmedRequestSize   = min(failsafe.size, MAX_BALL_COUNT)

        return shootDrumCanSkipLogic(
                failsafe, trimmedRequestSize,
                isNowPerfectlySorted,
                autoUpdateUnfinishedWithFailsafe)
    }
    suspend fun shootDrumCanSkipLogic(
        requested: Array<BallRequest.Name>,
        trimmedRequestSize: Int,
        isNowPerfectlySorted:         Boolean = false,
        autoUpdatePatternWhenSucceed: Boolean = true): RequestResult.Name
    {
        var isNowPerfectlySorted = isNowPerfectlySorted
        var shootingResult  = Request.COLOR_NOT_PRESENT
        var curRequestId    = NOTHING

        while (curRequestId < trimmedRequestSize)
        {
            if (isForcedToTerminate(DRUM_REQUEST))
                return Request.TERMINATED

            if (!(ALWAYS_TRY_PREDICT_SORTING && curRequestId == NOTHING)
                && !isNowPerfectlySorted && TRY_ADDITIONAl_PREDICT_SORTING_WHILE_SHOOTING)
                isNowPerfectlySorted = storageCells.tryInitiatePredictSort(requested)

            if (isNowPerfectlySorted)
            {
                lazyStreamDrumRequest(storageCells.anyBallCount())
//                lazyStreamDrumRequest(2)
                return Request.SUCCESS_NOW_EMPTY
            }


            val requestResult = storageCells.handleRequest(requested[curRequestId])

            shootingResult = shootRequestFinalPhase(
                requestResult, DRUM_REQUEST,
                autoUpdatePatternWhenSucceed)

            curRequestId++
        }
        return shootingResult
    }



    suspend fun shootDrumUntilPatternBreaks(
        requested: Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed: Boolean
    ): RequestResult.Name
        = shootDrumUntilBreaksLogic(
            requested,
            min(requested.size, MAX_BALL_COUNT),
            Request.COLOR_NOT_PRESENT,
            storageCells
                .tryInitiatePredictSort(requested),
            autoUpdatePatternWhenSucceed)
    suspend fun shootDrumUntilPatternBreaks(
        requested: Array<BallRequest.Name>,
        failsafe:  Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed:     Boolean = true,
        autoUpdateUnfinishedWithFailsafe: Boolean = true
    ): RequestResult.Name
    {
        var isNowPerfectlySorted = storageCells.tryInitiatePredictSort(requested)
        var trimmedRequestSize   = min(requested.size, MAX_BALL_COUNT)

        val shootingResult = shootDrumUntilBreaksLogic(
                requested, trimmedRequestSize,
                Request.COLOR_NOT_PRESENT,
                isNowPerfectlySorted,
                autoUpdatePatternWhenSucceed)

        if (Request.didSucceed   (shootingResult) ||
            Request.wasTerminated(shootingResult))
            return shootingResult
        else if (autoUpdateUnfinishedWithFailsafe)
            dynamicMemoryPattern.setTemporary(failsafe)

        isNowPerfectlySorted = storageCells.tryInitiatePredictSort(failsafe)
        trimmedRequestSize   = min(failsafe.size, MAX_BALL_COUNT)

        return shootDrumUntilBreaksLogic(
                failsafe, trimmedRequestSize,
                Request.COLOR_NOT_PRESENT,
                isNowPerfectlySorted,
                autoUpdateUnfinishedWithFailsafe)
    }
    suspend fun shootDrumUntilBreaksLogic(
        requested: Array<BallRequest.Name>,
        trimmedRequestSize: Int,
        defaultError: RequestResult.Name,
        isNowPerfectlySorted:         Boolean = false,
        autoUpdatePatternWhenSucceed: Boolean = true): RequestResult.Name
    {
        var isNowPerfectlySorted = isNowPerfectlySorted
        var shootingResult  = defaultError
        var curRequestId    = NOTHING

        while (curRequestId < trimmedRequestSize)
        {
            if (isForcedToTerminate(DRUM_REQUEST))
                return Request.TERMINATED

            if (!(ALWAYS_TRY_PREDICT_SORTING && curRequestId == NOTHING)
                && !isNowPerfectlySorted && TRY_ADDITIONAl_PREDICT_SORTING_WHILE_SHOOTING)
                isNowPerfectlySorted = storageCells.tryInitiatePredictSort(requested)

            if (isNowPerfectlySorted)
            {
                lazyStreamDrumRequest(storageCells.anyBallCount())
//                lazyStreamDrumRequest(2)
                return Request.SUCCESS_NOW_EMPTY
            }


            val requestResult = storageCells.handleRequest(requested[curRequestId])

            shootingResult = shootRequestFinalPhase(
                requestResult, DRUM_REQUEST,
                autoUpdatePatternWhenSucceed)


            if (Request.wasTerminated(shootingResult)) return Request.TERMINATED
            if (Request.didFail(      shootingResult)) return defaultError

            curRequestId++
        }
        return shootingResult
    }



    suspend fun shootDrumRequestEntireIsValid(
        requested: Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed: Boolean = true
    ): RequestResult.Name
    {
        val curStoragePG = storageCells.ballCountPGA()
        val requestPGA   = countPGA(requested)

        if (canCompleteEntireRequest(curStoragePG, requestPGA))
            return shootDrumUntilBreaksLogic(
                requested,
                min(requested.size, MAX_BALL_COUNT),
                Request.FAIL_UNKNOWN,
                storageCells
                    .tryInitiatePredictSort(requested),
                autoUpdatePatternWhenSucceed)

        return Request.NOT_ENOUGH_COLORS
    }
    suspend fun shootDrumRequestEntireIsValid(
        requested: Array<BallRequest.Name>,
        failsafe:  Array<BallRequest.Name>,
        autoUpdatePatternWhenSucceed:     Boolean = true,
        autoUpdateUnfinishedWithFailsafe: Boolean = true
    ): RequestResult.Name
    {
        val curStoragePG = storageCells.ballCountPGA()
        var requestPGA   = countPGA(requested)

        if (canCompleteEntireRequest(curStoragePG, requestPGA))  //  All good
            return shootDrumUntilBreaksLogic(
                requested,
                min(requested.size, MAX_BALL_COUNT),
                Request.FAIL_UNKNOWN,
                storageCells
                    .tryInitiatePredictSort(requested),
                autoUpdatePatternWhenSucceed)

        if (autoUpdateUnfinishedWithFailsafe)
            dynamicMemoryPattern.setTemporary(failsafe)

        requestPGA = countPGA(failsafe)

        if (canCompleteEntireRequest(curStoragePG, requestPGA))  //  Failsafe good
            return shootDrumUntilBreaksLogic(
                failsafe,
                min(failsafe.size, MAX_BALL_COUNT),
                Request.FAIL_UNKNOWN,
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





    suspend fun resumeLogicAfterRequest(
        processId: Int,
        doAutoCalibration: Boolean = true)
    {
        logM.logMd("RESUME AFTER REQUEST, process: $processId", PROCESS_NAME)

        if (doAutoCalibration)
        {
            logM.logMd("Reversing belts for calibration", PROCESS_STARTING)
            storageCells.hwSortingM.reverseBeltsTime(Delay.PART_PUSH)
            logM.logMd("Finished reversing", PROCESS_ENDING)

            logM.logMd("Starting calibration", PROCESS_STARTING)
            storageCells.hwSortingM.forwardBeltsTime(Delay.HALF_PUSH)
            storageCells.hwSortingM.fullCalibrate()
        }
        else storageCells.hwSortingM.fullCalibrate()


        logM.logMd("Phase 2 - RESUME AFTER REQUEST, process: $processId", LOGIC_STEPS)

        runStatus.safeRemoveThisProcessIdFromQueue(processId)
        runStatus.safeRemoveThisProcessFromTerminationList(processId)
        runStatus.clearCurrentActiveProcess()
        storageCells.hwSortingM.resumeAwaitingEating()

        logM.logMd("FINISHED resume logic", PROCESS_ENDING)
    }
    @Synchronized
    fun resumeLogicAfterIntake(processId: Int)
    {
        runStatus.safeRemoveThisProcessFromTerminationList(processId)

        if (processId != INTAKE)
             runStatus.safeRemoveThisProcessIdFromQueue(processId)
        else runStatus.safeRemoveOnlyOneInstanceOfThisProcessFromQueue(processId)

        runStatus.clearCurrentActiveProcess()

        if (runStatus.countOfThisProcess(
                RUNNING_INTAKE_INSTANCE) == 0)
        {
            EventBusLI.invoke(FullFinishedIntakeEvent(
                storageCells.anyBallCount()))
            EventBusLI.invoke(SetLightColorEvent(LightColor.BLUE))
        }
    }



    suspend fun fullWaitForShotFired(
        processId: Int,
        doWaitBeforeNextShot: Boolean = false,
        autoUpdatePatternWhenSucceed : Boolean = true
    ): Boolean
    {
        storageCells.hwSortingM.openTurretGate()
        logM.logMd("waiting for shot - event send", EVENTS_FEEDBACK)
        shotWasFired.set(false)
        canShoot.set(false)
        EventBusLI.invoke(Request.IsReadyEvent)

        var timePassedWaiting: Long = NOTHING.toLong()
        while (!canShoot.get() && timePassedWaiting < DELAY.SSL_MAX_ODOMETRY_REALIGNMENT_AWAITING_MS)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            timePassedWaiting += DELAY.EVENT_AWAITING_MS
            if (isForcedToTerminate(processId)) return false
        }

        if (!canShoot.get()) return false

        timePassedWaiting = 0
        storageCells.hwSortingM.smartPushNextBall()

        while (!shotWasFired.get() &&
            timePassedWaiting < DELAY.SSL_MAX_SHOT_AWAITING_MS)
        {
            delay(DELAY.EVENT_AWAITING_MS)
            timePassedWaiting += DELAY.EVENT_AWAITING_MS
            if (isForcedToTerminate(processId)) return false
        }

        logM.logMd("DONE waiting for shot", PROCESS_ENDING)
        logM.logMd("fired? ${shotWasFired.get()}," +
                " delta time: $timePassedWaiting", GENERIC_INFO)

        storageCells.updateAfterRequest()

        if (doWaitBeforeNextShot) delay(BETWEEN_SHOTS_MS)

        if (autoUpdatePatternWhenSucceed)
            dynamicMemoryPattern.removeFromTemporary()

        return true
    }
}