package org.woen.modules.storage

import barrel.enumerators.Ball
import barrel.enumerators.BallRequest

import barrel.enumerators.ShotType
import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import barrel.enumerators.RunStatus

import kotlinx.coroutines.delay
import org.woen.threading.ThreadedEventBus

import android.annotation.SuppressLint



class Storage
{
    private var _runStatus = RunStatus()
    private var _intakeRunStatus  = RunStatus()
    private var _requestRunStatus = RunStatus()

    private var _storageCells: StorageCells = StorageCells()
    private var _shotWasFired: Boolean = false
    //!  private HardwareStorage hwStorage



    @SuppressLint("SuspiciousIndentation")
    suspend fun HandleIntake(inputBall: Ball.Name): IntakeResult.Name
    {
        _intakeRunStatus.SafeResetTermination()

        if (!IntakeRaceConditionIsPresent())
        {
                if (DoTerminateIntake()) return TerminateIntake()
            val intakeResult = _storageCells.HandleIntake()

                if (DoTerminateIntake()) return TerminateIntake()
            if (!UpdateAfterInput(intakeResult, inputBall))  //  Safe updating after intake
                intakeResult.Set(IntakeResult.FAIL_UNKNOWN, IntakeResult.Name.FAIL_UNKNOWN)

            SafeResumeRequestLogic()
            return if (intakeResult.DidSucceed()) IntakeResult.Name.SUCCESS else intakeResult.Name()
        }
        SafeResumeRequestLogic()
        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    suspend fun IntakeRaceConditionIsPresent(): Boolean
    {
        if (_intakeRunStatus.IsActive())
        {
            ForceStopRequest()

            delay(4)
            return _intakeRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private fun UpdateAfterInput(intakeResult: IntakeResult, inputBall: Ball.Name): Boolean
    {
        if (intakeResult.DidFail()) return false  //  Intake failed

        //!  Align center slot to be empty
        TODO("Handle motor rotation to correct slot")


        return _storageCells.UpdateAfterIntake(inputBall)  //  Safe intake
    }
    private fun DoTerminateIntake(): Boolean
    {
        return _intakeRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    }
    private fun TerminateIntake(): IntakeResult.Name
    {
        _intakeRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        SafeResumeRequestLogic()
        return IntakeResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }



    @SuppressLint("SuspiciousIndentation")
    suspend fun HandleRequest(request: BallRequest.Name): RequestResult.Name
    {
        _requestRunStatus.SafeResetTermination()
        while (RequestRaceConditionIsPresent()) ;

            if (DoTerminateRequest()) return TerminateRequest()
        var requestResult = _storageCells.HandleRequest(request)

            if (DoTerminateRequest()) return TerminateRequest()
        requestResult = ShootRequestFinalPhase(requestResult)

        SafeResumeIntakeLogic()
        return requestResult.Name()
    }
    private fun UpdateAfterRequest(requestResult: RequestResult): Boolean
    {
        TODO("Rotate motor to target slot")


        //!  ThreadedEventBus.LAZY_INSTANCE.invoke(storageRequestIsReadyEvent())

        return true
    }
    private fun ShootRequestFinalPhase(requestResult: RequestResult) : RequestResult
    {
        if (requestResult.DidFail()) return requestResult
        else if (UpdateAfterRequest(requestResult))
        {
            while (!_shotWasFired) ;
            _shotWasFired = false  //!  Maybe improve this later

            if (_storageCells.UpdateAfterRequest())
            {
                return if (_storageCells.AnyBallCount() > 0)
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
                _storageCells.FixStorageDesync()
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
    private suspend fun RequestRaceConditionIsPresent(): Boolean
    {
        if (_requestRunStatus.IsActive())
        {
            ForceStopIntake()

            delay(2)
            return _requestRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private fun DoTerminateRequest(): Boolean
    {
        return _requestRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    }
    private fun TerminateRequest(): RequestResult.Name
    {
        _requestRunStatus.SetTermination(
            RunStatus.IS_ACTIVE,
            RunStatus.TerminationStatus.IS_ACTIVE
        )

        SafeResumeIntakeLogic()
        return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }




    suspend fun ShootEntireDrumRequest(): RequestResult.Name
    {
        _requestRunStatus.SafeResetTermination()
        while (RequestRaceConditionIsPresent()) ;

        if (DoTerminateRequest()) return TerminateRequest()
        ShootEverything()

        SafeResumeIntakeLogic()
        return RequestResult.Name.SUCCESS_IS_NOW_EMPTY
    }
    suspend fun ShootEntireDrumRequest(
        requestOrder: Array<BallRequest.Name>,
        shotType: ShotType
    ): RequestResult.Name
    {
        return ShootEntireDrumRequest(requestOrder, requestOrder, shotType)
    }
    @JvmOverloads
    suspend fun ShootEntireDrumRequest(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name> = requestOrder,
        shotType: ShotType = ShotType.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
    ): RequestResult.Name
    {
        _requestRunStatus.SafeResetTermination()
        while (RequestRaceConditionIsPresent()) ;

        if (DoTerminateRequest()) return TerminateRequest()

        val requestResult: RequestResult.Name =
            when (shotType)
            {
                ShotType.FIRE_EVERYTHING_YOU_HAVE -> ShootEverything()
                ShotType.FIRE_PATTERN_CAN_SKIP -> ShootEntireRequestCanSkip(requestOrder, failsafeOrder)
                ShotType.FIRE_UNTIL_PATTERN_IS_BROKEN -> ShootEntireUntilPatternBreaks(requestOrder, failsafeOrder)
                else  -> ShootEntireRequestIsValid(requestOrder, failsafeOrder)
            }

        SafeResumeIntakeLogic()
        return requestResult
    }



    @SuppressLint("SuspiciousIndentation")
    private fun ShootEverything(): RequestResult.Name
    {
        var shootingResult = RequestResult(RequestResult.FAIL_IS_EMPTY, RequestResult.Name.FAIL_IS_EMPTY)

        var i = 0
        while (i < 3)
        {
                if (DoTerminateRequest()) return TerminateRequest()
            val requestResult = _storageCells.HandleRequest(BallRequest.Name.ANY)

                if (DoTerminateRequest()) return TerminateRequest()
            shootingResult = ShootRequestFinalPhase(requestResult)

            i++
        }
        return shootingResult.Name()
    }



    private fun ShootEntireRequestCanSkip(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val shootingResult = ShootEntireCanSkipLogic(requestOrder)

        if (RequestResult.DidSucceed(shootingResult))
            return shootingResult
        return ShootEntireCanSkipLogic(failsafeOrder)
    }
    @SuppressLint("SuspiciousIndentation")
    private fun ShootEntireCanSkipLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        for (i in 0..2)
        {
                if (DoTerminateRequest()) return TerminateRequest()
            val requestResult = _storageCells.HandleRequest(requestOrder[i])

                if (DoTerminateRequest()) return TerminateRequest()
            shootingResult = ShootRequestFinalPhase(requestResult).Name()
        }
        return shootingResult
    }



    private fun ShootEntireUntilPatternBreaks(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val shootingResult = ShootEntireUntilBreaksLogic(requestOrder)

        if (RequestResult.DidSucceed(shootingResult))
            return shootingResult
        return ShootEntireUntilBreaksLogic(failsafeOrder)
    }
    @SuppressLint("SuspiciousIndentation")
    private fun ShootEntireUntilBreaksLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        val shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = 0
        while (i < 3)
        {
                if (DoTerminateRequest()) return TerminateRequest()
            var requestResult = _storageCells.HandleRequest(requestOrder[i])

                if (DoTerminateRequest()) return TerminateRequest()
            requestResult = ShootRequestFinalPhase(requestResult)
            if (requestResult.DidFail()) i += 2 //  Fast break if storage is empty

            i++
        }
        return shootingResult
    }



    @SuppressLint("SuspiciousIndentation")
    private fun ShootEntireRequestIsValid(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val countPG = _storageCells.BallColorCountPG()
        var requestCountPGA = CountPGA(requestOrder)

            if (DoTerminateRequest()) return TerminateRequest()
        if (ValidateEntireRequestDidSucceed(countPG, requestCountPGA))  //  All good
            return ShootEntireValidRequestLogic(requestOrder)


        requestCountPGA = CountPGA(failsafeOrder)

            if (DoTerminateRequest()) return TerminateRequest()
        if (ValidateEntireRequestDidSucceed(countPG, requestCountPGA))  //  Failsafe good
            return ShootEntireValidRequestLogic(failsafeOrder)


        return RequestResult.Name.FAIL_NOT_ENOUGH_COLORS  //  All bad
    }

    private fun CountPGA(requestOrder: Array<BallRequest.Name>): IntArray
    {
        val countPGA = intArrayOf(0, 0, 0)

        for (i in 0..2)
        {
            if      (requestOrder[i] == BallRequest.Name.PURPLE) countPGA[0]++
            else if (requestOrder[i] == BallRequest.Name.GREEN)  countPGA[1]++
            else if (requestOrder[i] == BallRequest.Name.ANY)    countPGA[2]++
        }
        return countPGA
    }
    private fun ValidateEntireRequestDidSucceed(countPG: IntArray, requestCountPGA: IntArray): Boolean
    {
        val storageDeltaAfterRequests = intArrayOf(
            countPG[0] - requestCountPGA[0],
            countPG[1] - requestCountPGA[1]
        )

        return storageDeltaAfterRequests[0] >= 0 && storageDeltaAfterRequests[1] >= 0 &&
                storageDeltaAfterRequests[0] + storageDeltaAfterRequests[1] >= requestCountPGA[2]
    }
    @SuppressLint("SuspiciousIndentation")
    private fun ShootEntireValidRequestLogic(requestOrder: Array<BallRequest.Name>): RequestResult.Name
    {
        var requestResult = RequestResult()

        for (i in 0..2)
        {
                if (DoTerminateRequest()) return TerminateRequest()
            requestResult = _storageCells.HandleRequest(requestOrder[i])

                if (DoTerminateRequest()) return TerminateRequest()
            requestResult = ShootRequestFinalPhase(requestResult)

            if (requestResult.DidFail())  return requestResult.Name()  //  Fast break if something went wrong
        }
        return requestResult.Name()
    }






    fun ForceStopIntake()
    {
        _intakeRunStatus.Set(
            RunStatus.USED_BY_ANOTHER_PROCESS,
            RunStatus.Name.USED_BY_ANOTHER_PROCESS
        )
    }
    private fun SafeResumeIntakeLogic()
    {
        if (_intakeRunStatus.IsUsedByAnotherProcess())
            _intakeRunStatus.Set(
                RunStatus.ACTIVE,
                RunStatus.Name.ACTIVE
            )
    }
    private fun UnsafeForceResumeIntakeLogic()
    {
        _intakeRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
    }

    fun ForceStopRequest()
    {
        _requestRunStatus.Set(
            RunStatus.USED_BY_ANOTHER_PROCESS,
            RunStatus.Name.USED_BY_ANOTHER_PROCESS
        )
    }
    private fun SafeResumeRequestLogic()
    {
        if (_requestRunStatus.IsUsedByAnotherProcess())
            _requestRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
    }
    private fun UnsafeForceResumeRequestLogic()
    {
        _requestRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
    }



    fun StorageRaw(): Array<Ball>
    {
        return _storageCells.StorageRaw()
    }
    fun StorageFiltered(): Array<Ball>
    {
        return _storageCells.StorageFiltered()
    }

    fun BallColorCountPG(): IntArray
    {
        return _storageCells.BallColorCountPG()
    }

    fun PurpleBallCount(): Int
    {
        return _storageCells.SelectedBallCount(Ball.Name.PURPLE)
    }
    fun GreenBallCount(): Int
    {
        return _storageCells.SelectedBallCount(Ball.Name.GREEN)
    }

    fun AnyBallCount(): Int
    {
        return _storageCells.AnyBallCount()
    }
    fun BallCount(): Int
    {
        return _storageCells.AnyBallCount()
    }





    fun start()
    {
        if (_runStatus.Name() != RunStatus.Name.PAUSE)
            _runStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE)
    }
    init
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateIntakeEvent::class, {
            _intakeRunStatus.SetTermination(
                RunStatus.DO_TERMINATE,
                RunStatus.TerminationStatus.DO_TERMINATE
            )
        } )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateRequestEvent::class, {
            _requestRunStatus.SetTermination(
                RunStatus.DO_TERMINATE,
                RunStatus.TerminationStatus.DO_TERMINATE
            )
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