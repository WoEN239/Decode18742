package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference

import android.annotation.SuppressLint

import woen239.enumerators.Ball
import woen239.enumerators.BallRequest

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import woen239.enumerators.ShotType
import woen239.enumerators.RunStatus

import org.woen.threading.ThreadedEventBus
import org.woen.modules.scoringSystem.storage.StorageFinishedIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageIsReadyToEatIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent
import org.woen.modules.scoringSystem.storage.StorageFinishedEveryRequestEvent

import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_ONE_BALL_PUSHING_MS
import org.woen.telemetry.Configs.STORAGE.INTAKE_RACE_CONDITION_DELAY_MS
import org.woen.telemetry.Configs.STORAGE.REQUEST_RACE_CONDITION_DELAY_MS



class SortingStorage
{
    private val _storageCells = StorageCells()

    private val _intakeRunStatus  = RunStatus()
    private val _requestRunStatus = RunStatus()

    private var _shotWasFired = AtomicReference(false)
    private var _ballWasEaten = AtomicReference(false)



    suspend fun handleIntake(inputBall: Ball.Name): IntakeResult.Name
    {
        if (_storageCells.anyBallCount() >= MAX_BALL_COUNT)
            return IntakeResult.Name.FAIL_STORAGE_IS_FULL

        if (noIntakeRaceConditionProblems())
        {
            if (doTerminateIntake()) return terminateIntake()

            val storageCanHandle = _storageCells.handleIntake()

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


        when (intakeResult.Name())
        {
            IntakeResult.Name.SUCCESS_CENTER     -> _storageCells.partial1RotateCW()
            IntakeResult.Name.SUCCESS_MOBILE_OUT -> _storageCells.autoPartial2RotateCW()
            IntakeResult.Name.SUCCESS_MOBILE_IN  -> _storageCells.partial3RotateCW()
            else -> { }
        }


        if (!fullWaitForIntakeIsFinishedEvent())
            return terminateIntake()


        return if (_storageCells.updateAfterIntake(inputBall))
             IntakeResult.Name.SUCCESS
        else IntakeResult.Name.FAIL_UNKNOWN
    }

    private suspend fun intakeRaceConditionIsPresent(): Boolean
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
    fun switchTerminateIntake() = _intakeRunStatus.DoTerminate()



    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
    {
        if (_storageCells.anyBallCount() <= 0)
            return RequestResult.Name.FAIL_IS_EMPTY

        if (cantHandleRequestRaceCondition()) return terminateRequest()


        val requestResult = _storageCells.handleRequest(request)


        if (doTerminateRequest()) return terminateRequest()
        val shootingResult = shootRequestFinalPhase(requestResult)


        fullResumeIntakeLogic(shootingResult)
        return shootingResult
    }
    private suspend fun updateAfterRequest(requestResult: RequestResult): RequestResult
    {
        if (requestResult.DidFail()) return requestResult   //  Intake failed


        val fullRotations = when (requestResult.Name()) {
            RequestResult.Name.SUCCESS_MOBILE_IN -> 2
            RequestResult.Name.SUCCESS_BOTTOM -> 1
            RequestResult.Name.SUCCESS_CENTER -> 0
            else -> -1
        }

        if (fullRotations >= 0)
        {
            repeat(fullRotations) {
                _storageCells.fullRotateCW()
            }

            _storageCells.partial2RotateCW()
        }


        if (!fullWaitForShotFired())
            return RequestResult(terminateRequest())


        return if (_storageCells.updateAfterRequest())
            RequestResult(
                RequestResult.SUCCESS,
                RequestResult.Name.SUCCESS)
        else RequestResult(
            RequestResult.FAIL_HARDWARE_PROBLEM,
            RequestResult.Name.FAIL_HARDWARE_PROBLEM)
    }
    private suspend fun shootRequestFinalPhase(requestResult: RequestResult) : RequestResult.Name
    {
        if (requestResult.DidFail()) return requestResult.Name()

        val updateResult = updateAfterRequest(requestResult)
        if (updateResult.DidSucceed())
        {
            fullWaitForShotFired()

            return if (_storageCells.updateAfterRequest())
            {
                if (_storageCells.anyBallCount() > 0)
                     RequestResult.Name.SUCCESS
                else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
            }
            else
            {
                //!  _storageCells.fixStorageDesync()
                RequestResult.Name.FAIL_SOFTWARE_STORAGE_DESYNC
            }
        }
        else return updateResult.Name()
    }

    private suspend fun requestRaceConditionIsPresent(): Boolean
    {
        if (_requestRunStatus.IsActive())
        {
            forceStopIntake()

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

    private fun terminateRequest(): RequestResult.Name
    {
        _requestRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        val requestFail = RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
        fullResumeIntakeLogic(requestFail)

        return requestFail
    }
    private fun doTerminateRequest() = _requestRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    fun switchTerminateRequest() = _requestRunStatus.DoTerminate()



    suspend fun pushNextWithoutUpdating(time: Long = DELAY_FOR_ONE_BALL_PUSHING_MS)
        = _storageCells.hwRotateBeltCW(time)



    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        if (_storageCells.anyBallCount() <= 0) return RequestResult.Name.FAIL_IS_EMPTY
        if (cantHandleRequestRaceCondition()) return terminateRequest()

        val requestResult = shootEverything()

        fullResumeIntakeLogic(requestResult)
        return requestResult
    }
    suspend fun shootEntireDrumRequest(
        shotType: ShotType,
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name> = requestOrder,
    ): RequestResult.Name
    {
        if (_storageCells.anyBallCount() <= 0) return RequestResult.Name.FAIL_IS_EMPTY
        if (cantHandleRequestRaceCondition()) return terminateRequest()

        val requestResult =
            when (shotType)
            {
                ShotType.FIRE_EVERYTHING_YOU_HAVE -> shootEverything()
                ShotType.FIRE_PATTERN_CAN_SKIP -> shootEntireRequestCanSkip(requestOrder, failsafeOrder)
                ShotType.FIRE_UNTIL_PATTERN_IS_BROKEN -> shootEntireUntilPatternBreaks(requestOrder, failsafeOrder)
                else  -> shootEntireRequestIsValid(requestOrder, failsafeOrder)
            }

        fullResumeIntakeLogic(requestResult)
        return requestResult
    }



    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEverything(): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_IS_EMPTY

        var i = 0
        while (i < MAX_BALL_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(BallRequest.Name.ANY_CLOSEST)

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult)

            i++
        }
        return shootingResult
    }



    private suspend fun shootEntireRequestCanSkip(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val shootingResult = shootEntireCanSkipLogic(requestOrder)

        if (RequestResult.DidSucceed(shootingResult)) return shootingResult
        return shootEntireCanSkipLogic(failsafeOrder)
    }
    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEntireCanSkipLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = 0
        while (i < MAX_BALL_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(requestOrder[i])

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult)

            i++
        }
        return shootingResult
    }



    private suspend fun shootEntireUntilPatternBreaks(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val shootingResult = shootEntireUntilBreaksLogic(requestOrder)

        if (RequestResult.DidSucceed(shootingResult)) return shootingResult
        return shootEntireUntilBreaksLogic(failsafeOrder)
    }
    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEntireUntilBreaksLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = 0
        while (i < MAX_BALL_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(requestOrder[i])

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult)

            if (RequestResult.DidFail(shootingResult))
                i += MAX_BALL_COUNT  //  Fast break if next ball is not present

            i++
        }
        return shootingResult
    }



    @SuppressLint("SuspiciousIndentation")
    private suspend fun shootEntireRequestIsValid(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val countPG = _storageCells.ballColorCountPG()
        var requestCountPGA = countPGA(requestOrder)

            if (doTerminateRequest()) return terminateRequest()
        if (validateEntireRequestDidSucceed(countPG, requestCountPGA))  //  All good
            return shootEntireValidRequestLogic(requestOrder)


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

        var i = 0
        while (i < MAX_BALL_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(requestOrder[i])

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult)

            if (RequestResult.DidFail(shootingResult))  return shootingResult
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
    private fun fullResumeIntakeLogic(requestResult: RequestResult.Name)
    {
        safeResumeIntakeLogic()

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
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageRequestIsReadyEvent())

        while (!_shotWasFired.get())
        {
            delay(DELAY_FOR_EVENT_AWAITING_MS)
            if (doTerminateRequest()) return false
        }

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



    suspend fun forceSafeStart()
    {
        while (!_intakeRunStatus.IsInactive())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        _intakeRunStatus.SetActive()

        while (!_requestRunStatus.IsInactive())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        _requestRunStatus.SetActive()

        _storageCells.forceSafeStartHwBelt()
    }
    suspend fun forceSafeStop()
    {
        _intakeRunStatus.DoTerminate()
        while (!_intakeRunStatus.IsTerminated())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        _intakeRunStatus.SetInactive()

        _requestRunStatus.DoTerminate()
        while (!_intakeRunStatus.IsTerminated())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        _intakeRunStatus.SetInactive()

        _storageCells.forceSafeStopHwBelt()
    }
    fun emergencyForceStop()
    {
        _intakeRunStatus.SetInactive()
        _intakeRunStatus.DoTerminate()

        _requestRunStatus.SetInactive()
        _requestRunStatus.DoTerminate()

        _storageCells.emergencyStopHwBelt()
    }



    fun linkHardware()
    {
        _storageCells.linkBeltHardware()
        _storageCells.linkMobileSlotHardware()
    }
}