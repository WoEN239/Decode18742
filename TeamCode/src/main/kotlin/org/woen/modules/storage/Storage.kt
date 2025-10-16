package org.woen.modules.storage

import barrel.enumerators.Ball
import barrel.enumerators.BallRequest

import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import barrel.enumerators.RunStatus

import kotlinx.coroutines.delay
import org.woen.threading.ThreadedEventBus

import android.annotation.SuppressLint



class Storage
{
    private var _runStatus = RunStatus()
    private var _intakeRunStatus = RunStatus()
    private var _requestRunStatus = RunStatus()

    private var _storageCells: StorageCells = StorageCells()
    private var _shotWasFired: Boolean = false;
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
            _requestRunStatus.Set(
                RunStatus.USED_BY_ANOTHER_PROCESS,
                RunStatus.Name.USED_BY_ANOTHER_PROCESS
            )

            delay(3)
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
        return IntakeResult.Name.FAIL_PROCESS_WAS_TERMINATED;
    }



    @SuppressLint("SuspiciousIndentation")
    suspend fun HandleRequest(request: BallRequest.Name): RequestResult.Name
    {
        _requestRunStatus.SafeResetTermination()

        while (RequestRaceConditionIsPresent()) ;

            if (DoTerminateRequest()) return TerminateRequest()
        val requestResult = _storageCells.HandleRequest(request)

            if (DoTerminateRequest()) return TerminateRequest()
        if (UpdateAfterRequest(requestResult))
        {
            TODO("Improve this")
            while (!_shotWasFired) ;
            _shotWasFired = false  //!  Maybe improve this later

            _storageCells.UpdateAfterRequest();
        }


        SafeResumeIntakeLogic()
        return if (requestResult.DidSucceed())
            if (_storageCells.AnyBallCount() > 0) RequestResult.Name.SUCCESS
            else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        else requestResult.Name()
    }
    private fun UpdateAfterRequest(requestResult: RequestResult): Boolean
    {
        if (requestResult.DidFail()) return false


        TODO("Rotate motor to target slot")
        //!  ThreadedEventBus.LAZY_INSTANCE.invoke(BarrelRequestIsReadyEvent())
        return true
    }
    private suspend fun RequestRaceConditionIsPresent(): Boolean
    {
        if (_requestRunStatus.IsActive())
        {
            _intakeRunStatus.Set(
                RunStatus.USED_BY_ANOTHER_PROCESS,
                RunStatus.Name.USED_BY_ANOTHER_PROCESS
            )

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
        return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED;
    }









    fun ForceStopIntake()
    {
        _intakeRunStatus.Set(RunStatus.INACTIVE, RunStatus.Name.INACTIVE);
    }
    fun SafeResumeIntakeLogic()
    {
        if (_intakeRunStatus.IsInactive())
            _intakeRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
    }
    fun UnsafeForceResumeIntakeLogic()
    {
        _intakeRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE);
    }

    fun ForceStopRequest()
    {
        _requestRunStatus.Set(RunStatus.INACTIVE, RunStatus.Name.INACTIVE);
    }
    fun SafeResumeRequestLogic()
    {
        if (_requestRunStatus.IsInactive())
            _requestRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
    }
    fun UnsafeForceResumeRequestLogic()
    {
        _requestRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE);
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





    fun Start()
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
            _shotWasFired = true;
        } )



        TODO("Add hardware initialisation logic")

        //_hwStorage = HardwareStorage(deviceName, direction)
        //_hwStorage.init(hwMap)
        //HardwareThreads.getLAZY_INSTANCE().getEXPANSION().addDevices(_hwStorage)
    }
}