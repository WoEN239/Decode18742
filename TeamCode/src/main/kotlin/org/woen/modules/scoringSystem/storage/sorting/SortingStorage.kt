package org.woen.modules.scoringSystem.storage.sorting


import woen239.enumerators.Ball
import woen239.enumerators.BallRequest

import woen239.enumerators.ShotType
import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import woen239.enumerators.RunStatus

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference

import android.annotation.SuppressLint

import org.woen.threading.ThreadedEventBus
import org.woen.modules.scoringSystem.storage.StorageFinishedIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent
import org.woen.modules.scoringSystem.storage.StorageFinishedEveryRequestEvent

import org.woen.modules.scoringSystem.storage.sorting.hardware.HwSortingManager

import org.woen.telemetry.Configs.STORAGE.REAL_SLOT_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING
import org.woen.telemetry.Configs.STORAGE.INTAKE_RACE_CONDITION_DELAY
import org.woen.telemetry.Configs.STORAGE.REQUEST_RACE_CONDITION_DELAY



class SortingStorage
{
    private val _intakeRunStatus  = RunStatus()
    private val _requestRunStatus = RunStatus()

    private val _storageCells = StorageCells()
    private lateinit var _hwSortingM: HwSortingManager  //  DO NOT JOIN ASSIGNMENT

    private var _shotWasFired = AtomicReference(false)




    @SuppressLint("SuspiciousIndentation")
    suspend fun handleIntake(inputBall: Ball.Name): IntakeResult.Name
    {
        if (_storageCells.anyBallCount() >= 3) return IntakeResult.Name.FAIL_STORAGE_IS_FULL

        if (noIntakeRaceConditionProblems())
        {
                if (doTerminateIntake()) return terminateIntake()
            val storageCanHandle = _storageCells.handleIntake()

                if (doTerminateIntake()) return terminateIntake()
            val intakeResult = updateAfterInput(storageCanHandle, inputBall)
            //  Safe updating storage after intake  - wont update if an error occurs

            fullResumeRequestLogic(intakeResult)
            return intakeResult
        }

        val intakeFail = IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        fullResumeRequestLogic(intakeFail)
        return intakeFail
    }
    private suspend fun intakeRaceConditionIsPresent(): Boolean
    {
        if (_intakeRunStatus.IsActive())
        {
            forceStopRequest()

            delay(INTAKE_RACE_CONDITION_DELAY)
            return _intakeRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private suspend fun noIntakeRaceConditionProblems(): Boolean
    {
        _intakeRunStatus.SafeResetTermination()

        return !intakeRaceConditionIsPresent()
    }
    private suspend fun updateAfterInput(intakeResult: IntakeResult, inputBall: Ball.Name): IntakeResult.Name
    {
        if (intakeResult.DidFail())  return intakeResult.Name()   //  Intake failed
        _hwSortingM.forceSafePause()


        //!  Align center slot to be empty
        TODO("Handle motor rotation to correct slot")


       _hwSortingM.forceSafeResume()

        return if (_storageCells.updateAfterIntake(inputBall))
             IntakeResult.Name.SUCCESS
        else IntakeResult.Name.FAIL_UNKNOWN
    }
    private fun doTerminateIntake(): Boolean
    {
        return _intakeRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    }
    private suspend fun terminateIntake(): IntakeResult.Name
    {
        _intakeRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        val intakeFail = IntakeResult.Name.FAIL_PROCESS_WAS_TERMINATED
        fullResumeRequestLogic(intakeFail)

        return intakeFail
    }
    fun switchTerminateIntake()
    {
        _intakeRunStatus.DoTerminate()
    }



    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
    {
        if (_storageCells.anyBallCount() <= 0) return RequestResult.Name.FAIL_IS_EMPTY

        handleRequestRaceCondition()
        if (doTerminateRequest()) return terminateRequest()

        var requestResult = _storageCells.handleRequest(request)
        if (doTerminateRequest()) return terminateRequest()

        requestResult = shootRequestFinalPhase(requestResult)

        fullResumeIntakeLogic(requestResult.Name())
        return requestResult.Name()
    }
    private suspend fun updateAfterRequest(requestResult: RequestResult): Boolean
    {
        _hwSortingM.forceSafePause()


        TODO("Rotate motor to target slot")  //!


        return true
    }
    private suspend fun shootRequestFinalPhase(requestResult: RequestResult) : RequestResult
    {
        if (requestResult.DidFail()) return requestResult
        else if (updateAfterRequest(requestResult))
        {
            fullWaitForShotFired()

            if (_storageCells.updateAfterRequest())
            {
                return if (_storageCells.anyBallCount() > 0)
                     RequestResult(
                        RequestResult.SUCCESS,
                        RequestResult.Name.SUCCESS
                    )
                else RequestResult(
                        RequestResult.SUCCESS_IS_NOW_EMPTY,
                        RequestResult.Name.SUCCESS_IS_NOW_EMPTY
                    )
            }

            else
            {
                _storageCells.fixStorageDesync()
                return RequestResult(
                        RequestResult.FAIL_SOFTWARE_STORAGE_DESYNC,
                        RequestResult.Name.FAIL_SOFTWARE_STORAGE_DESYNC
                    )
            }
        }

        return RequestResult(
            RequestResult.FAIL_HARDWARE_PROBLEM,
            RequestResult.Name.FAIL_HARDWARE_PROBLEM
        )
    }
    private suspend fun requestRaceConditionIsPresent(): Boolean
    {
        if (_requestRunStatus.IsActive())
        {
            forceStopIntake()

            delay(REQUEST_RACE_CONDITION_DELAY)
            return _requestRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private suspend fun handleRequestRaceCondition()
    {
        _requestRunStatus.SafeResetTermination()
        while (requestRaceConditionIsPresent())
            delay(DELAY_FOR_EVENT_AWAITING)
    }
    private fun doTerminateRequest(): Boolean
    {
        return _requestRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    }
    private suspend fun terminateRequest(): RequestResult.Name
    {
        _requestRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        val requestFail = RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
        fullResumeIntakeLogic(requestFail)

        return requestFail
    }
    fun switchTerminateRequest()
    {
        _requestRunStatus.DoTerminate()
    }




    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        if (_storageCells.anyBallCount() <= 0) return RequestResult.Name.FAIL_IS_EMPTY

        handleRequestRaceCondition()
        if (doTerminateRequest()) return terminateRequest()

        val requestResult = shootEverything()

        fullResumeIntakeLogic(requestResult)
        return requestResult
    }
    suspend fun shootEntireDrumRequest(
        requestOrder: Array<BallRequest.Name>,
        shotType: ShotType
    ): RequestResult.Name
    {
        return shootEntireDrumRequest(requestOrder, requestOrder, shotType)
    }
    @JvmOverloads
    suspend fun shootEntireDrumRequest(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name> = requestOrder,
        shotType: ShotType = ShotType.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
    ): RequestResult.Name
    {
        if (_storageCells.anyBallCount() <= 0) return RequestResult.Name.FAIL_IS_EMPTY

        handleRequestRaceCondition()
        if (doTerminateRequest()) return terminateRequest()

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
        var shootingResult = RequestResult(RequestResult.FAIL_IS_EMPTY, RequestResult.Name.FAIL_IS_EMPTY)

        var i = 0
        while (i < REAL_SLOT_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(BallRequest.Name.ANY_CLOSEST)

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult)

            i++
        }
        return shootingResult.Name()
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
        while (i < REAL_SLOT_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            val requestResult = _storageCells.handleRequest(requestOrder[i])

                if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootRequestFinalPhase(requestResult).Name()

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
        val shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = 0
        while (i < REAL_SLOT_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            var requestResult = _storageCells.handleRequest(requestOrder[i])

                if (doTerminateRequest()) return terminateRequest()
            requestResult = shootRequestFinalPhase(requestResult)
            if (requestResult.DidFail()) i += REAL_SLOT_COUNT //  Fast break if storage is empty

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

        var i = 0
        while (i < REAL_SLOT_COUNT)
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
        var requestResult = RequestResult()

        var i = 0
        while (i < REAL_SLOT_COUNT)
        {
                if (doTerminateRequest()) return terminateRequest()
            requestResult = _storageCells.handleRequest(requestOrder[i])

                if (doTerminateRequest()) return terminateRequest()
            requestResult = shootRequestFinalPhase(requestResult)

            if (requestResult.DidFail())  return requestResult.Name()  //  Fast break if something went wrong

            i++
        }
        return requestResult.Name()
    }





    fun forceStopIntake()
    {
        _intakeRunStatus.Set(
            RunStatus.USED_BY_ANOTHER_PROCESS,
            RunStatus.Name.USED_BY_ANOTHER_PROCESS
        )
    }
    fun safeResumeIntakeLogic()
    {
        if (_intakeRunStatus.IsUsedByAnotherProcess())
            _intakeRunStatus.SetActive()
    }
    private suspend fun fullResumeIntakeLogic(requestResult: RequestResult.Name)
    {
        safeResumeIntakeLogic()
        _hwSortingM.forceSafeResume()

        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageFinishedEveryRequestEvent(requestResult))
    }

    fun forceStopRequest()
    {
        _requestRunStatus.Set(
            RunStatus.USED_BY_ANOTHER_PROCESS,
            RunStatus.Name.USED_BY_ANOTHER_PROCESS
        )
    }
    private suspend fun fullResumeRequestLogic(intakeResult: IntakeResult.Name)
    {
        if (_requestRunStatus.IsUsedByAnotherProcess())
            _requestRunStatus.SetActive()

        _hwSortingM.forceSafeResume()

        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageFinishedIntakeEvent(intakeResult))
    }



    fun shotWasFired()
    {
        _shotWasFired.set(true)
    }
    private suspend fun fullWaitForShotFired()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageRequestIsReadyEvent())

        while (!_shotWasFired.get()) delay(DELAY_FOR_EVENT_AWAITING)
        _shotWasFired.set(false)
    }



    fun storageRaw(): Array<Ball>
    {
        return _storageCells.storageRaw()
    }
    fun storageFiltered(): Array<Ball>
    {
        return _storageCells.storageFiltered()
    }

    fun ballColorCountPG(): IntArray
    {
        return _storageCells.ballColorCountPG()
    }

    fun purpleBallCount(): Int
    {
        return _storageCells.selectedBallCount(Ball.Name.PURPLE)
    }
    fun greenBallCount(): Int
    {
        return _storageCells.selectedBallCount(Ball.Name.GREEN)
    }
    fun selectedBallCount(ball: Ball.Name): Int
    {
        return _storageCells.selectedBallCount(ball)
    }

    fun anyBallCount(): Int
    {
        return _storageCells.anyBallCount()
    }





    fun trySafeStart()
    {
        if (_intakeRunStatus.IsInactive())
            _intakeRunStatus.SetActive()
        if (_requestRunStatus.IsInactive())
            _requestRunStatus.SetActive()

        _hwSortingM.safeStart()
    }
    suspend fun safeStart(): Boolean
    {
        while (!_intakeRunStatus.IsInactive())
            delay(DELAY_FOR_EVENT_AWAITING)
        _intakeRunStatus.SetActive()

        while (!_requestRunStatus.IsInactive())
            delay(DELAY_FOR_EVENT_AWAITING)
        _requestRunStatus.SetActive()

        while (!_hwSortingM.safeStart())
            delay(DELAY_FOR_EVENT_AWAITING)

        return true
    }

    suspend fun safeStop(): Boolean
    {
        _intakeRunStatus.DoTerminate()
        while (!_intakeRunStatus.IsTerminated())
            delay(DELAY_FOR_EVENT_AWAITING)
        _intakeRunStatus.SetInactive()

        _requestRunStatus.DoTerminate()
        while (!_intakeRunStatus.IsTerminated())
            delay(DELAY_FOR_EVENT_AWAITING)
        _intakeRunStatus.SetInactive()

        while (!_hwSortingM.safeStop())
            delay(DELAY_FOR_EVENT_AWAITING)

        return true
    }
    fun forceStop()
    {
        _intakeRunStatus.SetInactive()
        _intakeRunStatus.DoTerminate()

        _requestRunStatus.SetInactive()
        _requestRunStatus.DoTerminate()

        _hwSortingM.forceStop()
    }



    fun linkHardware()
    {
        _hwSortingM = HwSortingManager("")
        _hwSortingM.addDevice()

        _storageCells.linkMobileSlotHardware()
    }
}