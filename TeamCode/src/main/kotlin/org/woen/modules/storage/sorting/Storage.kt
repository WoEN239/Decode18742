package org.woen.modules.storage.sorting


import barrel.enumerators.Ball
import barrel.enumerators.BallRequest

import barrel.enumerators.ShotType
import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import barrel.enumerators.RunStatus

import kotlinx.coroutines.delay
import org.woen.threading.ThreadedEventBus

import org.woen.modules.storage.GiveNextRequest
import org.woen.modules.storage.TerminateIntakeEvent
import org.woen.modules.storage.TerminateRequestEvent

import android.annotation.SuppressLint
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING

import org.woen.telemetry.Configs.STORAGE.REAL_SLOT_COUNT
import org.woen.telemetry.Configs.STORAGE.INTAKE_RACE_CONDITION_DELAY
import org.woen.telemetry.Configs.STORAGE.REQUEST_RACE_CONDITION_DELAY



class Storage
{
    private var _intakeRunStatus  = RunStatus()
    private var _requestRunStatus = RunStatus()

    private var _storageCells: StorageCells = StorageCells()
    private var _shotWasFired: Boolean = false
    //!  private HardwareStorage hwStorage



    @SuppressLint("SuspiciousIndentation")
    suspend fun handleIntake(inputBall: Ball.Name): IntakeResult.Name
    {
        if (noIntakeRaceConditionProblems())
        {
                if (doTerminateIntake()) return terminateIntake()
            val intakeResult = _storageCells.handleIntake()

                if (doTerminateIntake()) return terminateIntake()
            if (!updateAfterInput(intakeResult, inputBall))  //  Safe updating after intake
                intakeResult.Set(IntakeResult.FAIL_UNKNOWN, IntakeResult.Name.FAIL_UNKNOWN)

            safeResumeRequestLogic()
            return intakeResult.Name()
        }

        safeResumeRequestLogic()
        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
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
    private fun updateAfterInput(intakeResult: IntakeResult, inputBall: Ball.Name): Boolean
    {
        if (intakeResult.DidFail()) return false  //  Intake failed

        //!  Align center slot to be empty
        TODO("Handle motor rotation to correct slot")


        return _storageCells.updateAfterIntake(inputBall)  //  Safe intake
    }
    private fun doTerminateIntake(): Boolean
    {
        return _intakeRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    }
    private fun terminateIntake(): IntakeResult.Name
    {
        _intakeRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        safeResumeRequestLogic()
        return IntakeResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }



    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
    {
        handleRequestRaceCondition()
        if (doTerminateRequest()) return terminateRequest()

        var requestResult = _storageCells.handleRequest(request)
        if (doTerminateRequest()) return terminateRequest()

        requestResult = shootRequestFinalPhase(requestResult)

        safeResumeIntakeLogic()
        return requestResult.Name()
    }
    private fun updateAfterRequest(requestResult: RequestResult): Boolean
    {
        TODO("Rotate motor to target slot")


        //!  ThreadedEventBus.LAZY_INSTANCE.invoke(storageRequestIsReadyEvent())

        return true
    }
    private suspend fun shootRequestFinalPhase(requestResult: RequestResult) : RequestResult
    {
        if (requestResult.DidFail()) return requestResult
        else if (updateAfterRequest(requestResult))
        {
            waitForShotFiredEvent()

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
    private suspend fun waitForShotFiredEvent()
    {
        while (!_shotWasFired) delay(DELAY_FOR_EVENT_AWAITING)
        _shotWasFired = false  //!  Maybe improve this later
    }
    private fun doTerminateRequest(): Boolean
    {
        return _requestRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    }
    private fun terminateRequest(): RequestResult.Name
    {
        _requestRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        safeResumeIntakeLogic()
        return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }




    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        handleRequestRaceCondition()
        if (doTerminateRequest()) return terminateRequest()

        val requestResult = shootEverything()

        safeResumeIntakeLogic()
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

        safeResumeIntakeLogic()
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
            if      (requestOrder[i] == BallRequest.Name.PURPLE)        countPGA[0]++
            else if (requestOrder[i] == BallRequest.Name.GREEN)         countPGA[1]++
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
    private fun safeResumeIntakeLogic()
    {
        if (_intakeRunStatus.IsUsedByAnotherProcess())
            _intakeRunStatus.Set(
                RunStatus.ACTIVE,
                RunStatus.Name.ACTIVE
            )
    }
    private fun unsafeForceResumeIntakeLogic()
    {
        _intakeRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
    }

    fun forceStopRequest()
    {
        _requestRunStatus.Set(
            RunStatus.USED_BY_ANOTHER_PROCESS,
            RunStatus.Name.USED_BY_ANOTHER_PROCESS
        )
    }
    private fun safeResumeRequestLogic()
    {
        if (_requestRunStatus.IsUsedByAnotherProcess())
            _requestRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
    }
    private fun unsafeForceResumeRequestLogic()
    {
        _requestRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
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
    fun ballCount(): Int
    {
        return _storageCells.anyBallCount()
    }





    fun safeStart()
    {
        if (_intakeRunStatus.IsInactive())
            _intakeRunStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE)
        if (_requestRunStatus.IsInactive())
            _requestRunStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE)
    }

    fun safeStop(): Boolean
    {
        _intakeRunStatus.DoTerminate()
        while (_intakeRunStatus.IsTerminated())
            _intakeRunStatus.SetInactive()

        _requestRunStatus.DoTerminate()
        while (_intakeRunStatus.IsTerminated())
            _intakeRunStatus.SetInactive()

        return true
    }
    fun forceStop()
    {
        _intakeRunStatus.SetInactive()
        _intakeRunStatus.DoTerminate()

        _requestRunStatus.SetInactive()
        _requestRunStatus.DoTerminate()
    }



    init
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateIntakeEvent::class, {
            _intakeRunStatus.DoTerminate()
        } )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateRequestEvent::class, {
            _requestRunStatus.DoTerminate()
        } )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(GiveNextRequest::class, {
            _shotWasFired = true
        } )



        TODO("Add hardware initialisation logic")

        //_hwStorage = HardwareStorage(deviceName, direction)
        //_hwStorage.init(hwMap)
        //HardwareThreads.getLAZY_INSTANCE().getEXPANSION().addDevices(_hwStorage)
    }
}