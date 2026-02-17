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

import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.NOTHING
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT

import org.woen.utils.process.RunStatus
import org.woen.telemetry.LogManager

import org.woen.telemetry.configs.Debug
import org.woen.telemetry.configs.Delay
import org.woen.telemetry.configs.ProcessId
import org.woen.telemetry.configs.RobotSettings.SORTING
import org.woen.telemetry.configs.RobotSettings.SHOOTING



class SortingStorageLogic
{
    val storageCells           = StorageCells()
    val dynamicMemoryPattern   = DynamicPattern()

    val canShoot     = AtomicBoolean(false)
    val shotWasFired = AtomicBoolean(false)

    val runStatus = RunStatus(ProcessId.PRIORITY_SETTING_FOR_SSM)
    val logM = LogManager(Debug.SSL)



    fun resetParametersToDefault()
    {
        dynamicMemoryPattern.fullReset()
        runStatus.fullResetToActiveState()
        logM.reset(Debug.SSL)

        shotWasFired.set(false)
    }



    suspend fun canInitiatePredictSort(): Boolean
    {
        logM.logMd("Try initiating predict sort", Debug.TRYING)

        if (runStatus.isUsedByAnyProcess()) return false

        runStatus.addProcessToQueue(   ProcessId.PREDICT_SORT)
        delay(Delay.MS.RACE_CONDITION.PREDICT_SORT)

        return runStatus.isThisProcessHighestPriority(ProcessId.PREDICT_SORT)
    }
    suspend fun safeInitiatePredictSort(requested: Array<BallRequest.Name>)
    {
        runStatus.setActiveProcess(ProcessId.PREDICT_SORT)

        storageCells.initiatePredictSort(requested)

        runStatus.clearActiveProcess()
        runStatus.removeProcessFromQueue(ProcessId.PREDICT_SORT)
    }


    suspend fun canStartStorageCalibrationWithCurrent(): Boolean
    {
        logM.logMd("Try starting storage calibration with current", Debug.TRYING)

        if (runStatus.isUsedByAnyProcess()) return false

        runStatus.addProcessToQueue(   ProcessId.STORAGE_CALIBRATION)
        delay(Delay.MS.RACE_CONDITION.STORAGE_CALIBRATION)

        return runStatus.isThisProcessHighestPriority(ProcessId.STORAGE_CALIBRATION)
    }
    suspend fun safeStartStorageCalibrationWithCurrent()
    {
        runStatus.setActiveProcess(ProcessId.STORAGE_CALIBRATION)

        storageCells.safeFillWithUnknown()

        runStatus.clearActiveProcess()
        runStatus.removeProcessFromQueue(ProcessId.STORAGE_CALIBRATION)
    }


    suspend fun canStartUpdateAfterLazyIntake(): Boolean
    {
        logM.logMd("Try starting updating after lazy intake", Debug.TRYING)

        if (runStatus.isUsedByAnyProcess()) return false

        runStatus.addProcessToQueue(  ProcessId.UPDATE_AFTER_LAZY_INTAKE)
        delay(Delay.MS.RACE_CONDITION.UPDATE_AFTER_LAZY_INTAKE)

        return runStatus.isThisProcessHighestPriority(ProcessId.UPDATE_AFTER_LAZY_INTAKE)
    }
    fun safeUpdateAfterLazyIntake(inputFromTurretSlotToBottom: Array<Ball.Name>)
    {
        runStatus.setActiveProcess(ProcessId.UPDATE_AFTER_LAZY_INTAKE)

        storageCells.updateAfterLazyIntake(inputFromTurretSlotToBottom)

        runStatus.clearActiveProcess()
        runStatus.removeProcessFromQueue(ProcessId.UPDATE_AFTER_LAZY_INTAKE)
    }



    suspend fun safeSortIntake(inputBall: Ball.Name): IntakeResult.Name
    {
        if (storageCells.alreadyFull()) return Intake.FAIL_IS_FULL

        runStatus.addProcessToQueue(ProcessId.RUNNING_INTAKE_INSTANCE)

        logM.logMd("Sorting intake", Debug.LOGIC)
        storageCells.updateAfterIntake(inputBall)

        runStatus.removeOneInstanceOfProcessFromQueue(
            ProcessId.RUNNING_INTAKE_INSTANCE)

        return Intake.SUCCESS
    }

    private suspend fun intakeRaceConditionIsPresent(
        processId: Int,
        vararg exceptionProcessesId: Int):  Boolean
    {
        logM.logMd("CHECKING RACE CONDITION", Debug.RACE_CONDITION)
        if (runStatus.isUsedByAnotherProcess(
            processId, *exceptionProcessesId)) return true

        logM.logMd("Currently not busy", Debug.RACE_CONDITION)
        runStatus.addProcessToQueue(processId)

        delay(when (processId)
        {
                ProcessId.INTAKE -> Delay.MS.RACE_CONDITION.INTAKE
                else        -> Delay.MS.RACE_CONDITION.LAZY_INTAKE
        }   )

        logM.logMd("Highest processId: " +
                "${runStatus.getHighestPriorityProcess(
                *exceptionProcessesId)}",
                Debug.RACE_CONDITION)

        return !runStatus.isThisProcessHighestPriority(
            processId, *exceptionProcessesId)
    }
    suspend fun noIntakeRaceConditionProblems(
        processId: Int,
        vararg exceptionProcessesId: Int): Boolean
    {
        runStatus.removeProcessFromTermination(processId)

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
            storageCells.hwSortingM.reverseBeltsTime(Delay.MS.PUSH.PART)
            storageCells.hwSortingM.closeTurretGate()
            storageCells.hwSortingM.slowStartBelts()

            delay(Delay.MS.PUSH.HALF)
            storageCells.hwSortingM.stopBelts()
        }

        val fullRotations = when (requestResult.name())
        {
            Request.MOBILE_SLOT -> 2
            Request.BOTTOM_SLOT -> 2
            Request.CENTER_SLOT -> 1
            else -> -1
        }

        logM.logMd("rotating cur slot times: $fullRotations", Debug.LOGIC)
        repeat(fullRotations)
            { storageCells.fullRotate() }

        logM.logMd("sorting finished - success", Debug.END)
        logM.logMd("Getting ready to shoot", Debug.START)
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

        logM.logMd("Finished updating", Debug.END)

        return if (updateResult.didSucceed())
        {
            val isLastBall = storageCells.onlyOneBallLeft()
            storageCells.hwSortingM.helpPushLastBall.set(isLastBall)

            val doWaitBeforeNextShot = !isLastBall &&
                    SHOOTING.DO_WAIT_BEFORE_NEXT_SHOT

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
        else if (runStatus.isUsedByAnotherProcess(
                processId)) return true


        runStatus.addProcessToQueue(processId)
        storageCells.hwSortingM.stopAwaitingEating(true)

        delay(when (processId)
        {
                ProcessId.SINGLE_REQUEST
                     -> Delay.MS.RACE_CONDITION.SINGLE_REQUEST
                else -> Delay.MS.RACE_CONDITION.DRUM_REQUEST
        }   )

        return !runStatus.isThisProcessHighestPriority(processId)
    }
    suspend fun cantHandleRequestRaceCondition(processId: Int): Boolean
    {
        runStatus.removeProcessFromQueue(processId)
        runStatus.removeProcessFromTermination(processId)

        while (requestRaceConditionIsPresent(processId))
            delay(Delay.MS.AWAIT.EVENTS)

        return isForcedToTerminate(processId)
    }

    suspend fun terminateRequest(processId: Int): RequestResult.Name
    {
        logM.logMd("Request is being terminated", Debug.TERMINATION)

        runStatus.removeProcessFromQueue(processId)
        runStatus.removeProcessFromTermination(processId)

        resumeLogicAfterRequest(processId)
        return Request.TERMINATED
    }
    @Synchronized
    fun isForcedToTerminate(processId: Int)
        = runStatus.isForcedToTerminateThisProcess(processId)






    suspend fun streamDrumRequest(ballCount: Int)
    {
        logM.logMd("Starting stream shooting, count: $ballCount", Debug.START)

        val beltPushTime = when (ballCount)
        {
            3    -> Delay.MS.SHOOTING.FIRE_3
            2    -> Delay.MS.SHOOTING.FIRE_2
            else -> Delay.MS.SHOOTING.FIRE_1
        }

        logM.logMd("Firing time: $beltPushTime", Debug.GENERIC)

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
        logM.logMd("Expected shot count: $ballCount", Debug.GENERIC)

        streamDrumRequest(ballCount)
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

        storageCells.hwReAdjustStorage()

        while (curRequestId < trimmedRequestSize)
        {
            if (isForcedToTerminate(ProcessId.DRUM_REQUEST))
                return Request.TERMINATED

            if (furtherDoPredictSort(curRequestId, isNowPerfectlySorted))
                isNowPerfectlySorted = storageCells.tryInitiatePredictSort(requested)

            if (isNowPerfectlySorted)
            {
                streamDrumRequest(storageCells.anyBallCount())
//                streamDrumRequest(3)
                return Request.SUCCESS_NOW_EMPTY
            }


            val requestResult = storageCells.handleRequest(requested[curRequestId])

            shootingResult = shootRequestFinalPhase(
                if (requestResult.didFail())
                     Request.TURRET_SLOT else requestResult,
                ProcessId.DRUM_REQUEST,
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

        storageCells.hwReAdjustStorage()

        while (curRequestId < trimmedRequestSize)
        {
            if (isForcedToTerminate(ProcessId.DRUM_REQUEST))
                return Request.TERMINATED

            if (furtherDoPredictSort(curRequestId, isNowPerfectlySorted))
                isNowPerfectlySorted = storageCells.tryInitiatePredictSort(requested)

            if (isNowPerfectlySorted)
            {
                streamDrumRequest(storageCells.anyBallCount())
//                streamDrumRequest(3)
                return Request.SUCCESS_NOW_EMPTY
            }


            val requestResult = storageCells.handleRequest(requested[curRequestId])

            shootingResult = shootRequestFinalPhase(
                requestResult, ProcessId.DRUM_REQUEST,
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



    private fun furtherDoPredictSort(
        curRequestId: Int,
        isNowPerfectlySorted: Boolean)
        = !(SORTING.PREDICT.ALWAYS_TRY_IN_ADVANCE && curRequestId == NOTHING)
            && !isNowPerfectlySorted && SORTING.PREDICT.FURTHER_TRY_IN_ACTION
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
        logM.logMd("RESUME AFTER REQUEST, process: $processId", Debug.PROCESS_NAME)

        if (doAutoCalibration)
        {
            logM.logMd("Reversing belts for calibration", Debug.START)
            storageCells.hwSortingM.reverseBeltsTime(Delay.MS.PUSH.HALF)
            logM.logMd("Finished reversing", Debug.END)

            logM.logMd("Starting calibration", Debug.START)
            storageCells.hwSortingM.forwardBeltsTime(Delay.MS.PUSH.HALF)
            storageCells.hwSortingM.fullCalibrate()
        }
        else storageCells.hwSortingM.fullCalibrate()


        logM.logMd("Phase 2 - RESUME AFTER REQUEST, process: $processId", Debug.LOGIC)

        runStatus.removeProcessFromQueue(processId)
        runStatus.removeProcessFromTermination(processId)
        runStatus.clearActiveProcess()
        storageCells.hwSortingM.resumeAwaitingEating()

        logM.logMd("FINISHED resume logic", Debug.END)
    }
    @Synchronized
    fun resumeLogicAfterIntake(processId: Int)
    {
        runStatus.removeProcessFromTermination(processId)

        if (processId != ProcessId.INTAKE)
             runStatus.removeProcessFromQueue(processId)
        else runStatus.removeOneInstanceOfProcessFromQueue(processId)

        runStatus.clearActiveProcess()

        if (runStatus.countOfThisProcess(
                ProcessId.RUNNING_INTAKE_INSTANCE) == 0)
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
        logM.logMd("waiting for shot - event send", Debug.EVENTS)
        shotWasFired.set(false)
        canShoot.set(false)
        EventBusLI.invoke(Request.IsReadyEvent)

        var timePassedWaiting: Long = NOTHING.toLong()
        while (!canShoot.get() && timePassedWaiting <
            Delay.MS.AWAIT.ODOMETRY_TURNING)
        {
            delay(Delay.MS.AWAIT.EVENTS)
            timePassedWaiting += Delay.MS.AWAIT.EVENTS
            if (isForcedToTerminate(processId)) return false
        }

        if (!canShoot.get()) return false

        timePassedWaiting = 0
        storageCells.hwSortingM.smartPushNextBall()

        while (!shotWasFired.get() &&
            timePassedWaiting < Delay.MS.AWAIT.SSL_SHOT)
        {
            delay(Delay.MS.AWAIT.EVENTS)
            timePassedWaiting += Delay.MS.AWAIT.EVENTS
            if (isForcedToTerminate(processId)) return false
        }

        logM.logMd("DONE waiting for shot", Debug.END)
        logM.logMd("fired? ${shotWasFired.get()}," +
                " delta time: $timePassedWaiting", Debug.GENERIC)

        storageCells.updateAfterRequest()

        if (doWaitBeforeNextShot) delay(Delay.MS.AWAIT.EVENTS)

        if (autoUpdatePatternWhenSucceed)
            dynamicMemoryPattern.removeFromTemporary()

        return true
    }
}