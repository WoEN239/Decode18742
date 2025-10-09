package org.woen.modules.scoringSystem.barrel


/*
import android.annotation.SuppressLint
import barrel.BarrelStorage
import barrel.enumerators.Ball
import barrel.enumerators.BallRequest

import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import barrel.enumerators.ShotType
import barrel.enumerators.RunStatus
import barrel.enumerators.StorageOffset

import kotlinx.coroutines.delay
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads.Companion.LAZY_INSTANCE



class Barrel
{
    private var _runStatus: RunStatus
    private var _intakeRunStatus: RunStatus
    private var _requestRunStatus: RunStatus

    private var _storage: BarrelStorage
    private var _storageOffset: StorageOffset
    private val _barrelMotor: HardwareBarrel

    private var _shotWasFired: Boolean


    val CW_60: Double = 60.0; val CW_120: Double = CW_60 * 2;
    val CCW_60 = -60.0; val CCW_120: Double = CCW_60 * 2    //  60 & 120 degrees motor rotation constants constants



    init
    {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateIntakeEvent::class, {
                _intakeRunStatus.SetTermination(
                    RunStatus.DO_TERMINATE(),
                    RunStatus.TerminationStatus.DO_TERMINATE
                )
            }
        )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(TerminateRequestEvent::class, {
                _requestRunStatus.SetTermination(
                    RunStatus.DO_TERMINATE(),
                    RunStatus.TerminationStatus.DO_TERMINATE
                )
            }
        )
        ThreadedEventBus.LAZY_INSTANCE.subscribe(GiveNextRequest::class, {
            _shotWasFired = true; }
        )


        _runStatus = RunStatus()
        _intakeRunStatus = RunStatus()
        _requestRunStatus = RunStatus()

        _shotWasFired = false;

        _storage   = BarrelStorage()
        _storageOffset = StorageOffset()

        _barrelMotor = HardwareBarrel("aboba")
        LAZY_INSTANCE.EXPANSION.addDevices(_barrelMotor)
    }
    fun Start()
    {
        if (_runStatus.GetId() != RunStatus.PAUSE())
            _runStatus.Set(RunStatus.Name.ACTIVE,RunStatus.ACTIVE())

        if (_intakeRunStatus.GetTermination() == RunStatus.TerminationStatus.IS_INACTIVE)
            _intakeRunStatus.SetTermination(RunStatus.IS_ACTIVE(), RunStatus.TerminationStatus.IS_ACTIVE)

        if (_requestRunStatus.GetTermination() == RunStatus.TerminationStatus.IS_INACTIVE)
            _requestRunStatus.SetTermination(RunStatus.IS_ACTIVE(), RunStatus.TerminationStatus.IS_ACTIVE)
    }



    @SuppressLint("SuspiciousIndentation")
    suspend fun HandleInput(inputBall: Ball.Name?): IntakeResult.Name?
    {
        if (DoTerminateIntake()) TerminateIntake()    //  Does not terminate, only resets _intakeRunStatus

        if (!IntakeRaceConditionIsPresent())
        {
                if (DoTerminateIntake()) return TerminateIntake()
            val intakeResult = _storage.HandleInput()  //  Storage search for empty slots

                if (DoTerminateIntake()) return TerminateIntake()
            if (!UpdateAfterInput(intakeResult, inputBall))  //  Safe updating after intake
                intakeResult.Set(IntakeResult.Name.FAIL_UNKNOWN, IntakeResult.FAIL_UNKNOWN())

            return if (intakeResult.DidSucceed()) IntakeResult.Name.SUCCESS else intakeResult.GetName()
        }

        ResumeLogic()
        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    private fun UpdateAfterInput(intakeResult: IntakeResult, inputBall: Ball.Name?): Boolean
    {
        val name = intakeResult.GetName()
        if (IntakeResult.DidFail(name)) return false //  Intake failed

        //  Align center slot to be empty
        when (name)
        {
            IntakeResult.Name.SUCCESS_LEFT ->
            {
                _storage.RotateCCW()
                _barrelMotor.Rotate(CCW_120 + RealignToIntake())
            }
            IntakeResult.Name.SUCCESS_RIGHT ->
            {
                _storage.RotateCW()
                _barrelMotor.Rotate(CW_120 + RealignToIntake())
            }
            else -> _barrelMotor.Rotate(RealignToIntake())
        }


        return _storage.IntakeToCenter(inputBall) //  Safe intake
    }
    private fun RealignToIntake(): Double
    {
        if (_storageOffset.GetId() == StorageOffset.CCW_60()) return CW_60
        else if (_storageOffset.GetId() == StorageOffset.CW_60()) return CCW_60
        return 0.0
    }
    suspend private fun IntakeRaceConditionIsPresent(): Boolean
    {
        if (_runStatus.GetId() == RunStatus.ACTIVE())
        {
            StopAnyLogic()
            _intakeRunStatus.Set(RunStatus.PAUSE(), RunStatus.Name.PAUSE)

            delay(2)  //!  need to calibrate this delay for maximum efficiency
            return _intakeRunStatus.GetId() == RunStatus.PAUSE()
        }
        return true;
    }
    private fun DoTerminateIntake(): Boolean
    {
        return _intakeRunStatus.GetTerminationId() == RunStatus.DO_TERMINATE()
    }
    private fun TerminateIntake(): IntakeResult.Name
    {
        _intakeRunStatus.SetTermination(
            RunStatus.IS_ACTIVE(),
            RunStatus.TerminationStatus.IS_ACTIVE
        )

        return IntakeResult.Name.PROCESS_WAS_TERMINATED;
    }



    @SuppressLint("SuspiciousIndentation")
    suspend fun HandleRequest(request: BallRequest.Name?): RequestResult.Name?
    {
        if (DoTerminateRequest()) TerminateRequest()    //  Does not terminate, only resets _requestRunStatus

        while (RequestRaceConditionIsPresent()) ;

            if (DoTerminateRequest()) return TerminateRequest()
        val requestResult = _storage.HandleRequest(request)

            if (DoTerminateRequest()) return TerminateRequest()
        if (UpdateAfterRequest(requestResult))
        {
            while (!_shotWasFired) ; _shotWasFired = false  //!  Maybe improve this later

            _storage.EmptyWasFired(_storageOffset)
        }


        ResumeLogic()

        return if (requestResult.DidSucceed())
            if (_storage.AnyBallCount() > 0) RequestResult.Name.SUCCESS
            else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        else requestResult.GetName()
    }
    private fun UpdateAfterRequest(requestResult: RequestResult): Boolean
    {
        if (requestResult.DidFail()) return false

        if (requestResult.GetId() == RequestResult.SUCCESS_RIGHT())      //  Goal: CCW_60
        {
            if (_storageOffset.GetId() == StorageOffset.CW_60())
            {
                _barrelMotor.Rotate(CCW_120)
                _storageOffset.Set(StorageOffset.Name.CCW_60, StorageOffset.CCW_60())
            }
            else if (_storageOffset.GetId() == StorageOffset.NONE())
            {
                _barrelMotor.Rotate(CCW_60)
                _storageOffset.Set(StorageOffset.Name.CCW_60, StorageOffset.CCW_60())
            }
        }
        else if (requestResult.GetId() == RequestResult.SUCCESS_LEFT())  //  Goal: CW_60
        {
            if (_storageOffset.GetId() == StorageOffset.CCW_60())
            {
                _barrelMotor.Rotate(CW_120)
                _storageOffset.Set(StorageOffset.Name.CW_60, StorageOffset.CW_60())
            }
            else if (_storageOffset.GetId() == StorageOffset.NONE())
            {
                _barrelMotor.Rotate(CW_60)
                _storageOffset.Set(StorageOffset.Name.CW_60, StorageOffset.CW_60())
            }
        }
        else  //  SUCCESS_CENTER
        {
            if (_storageOffset.GetId() == StorageOffset.CCW_60())
            {
                _barrelMotor.Rotate(CCW_120)
                _storage.RotateCCW()
            }
            else
            {
                var delta: Double = CW_120;
                if (_storageOffset.GetId() == StorageOffset.NONE())
                    delta += CW_60;

                _barrelMotor.Rotate(delta)
                _storage.RotateCW()
                _storageOffset.Set(StorageOffset.Name.CW_60, StorageOffset.CW_60())
            }
        }

        ThreadedEventBus.LAZY_INSTANCE.invoke(BarrelRequestIsReadyEvent())
        return true
    }
    private suspend fun RequestRaceConditionIsPresent(): Boolean
    {
        if (_runStatus.GetId() == RunStatus.ACTIVE())
        {
            StopAnyLogic()
            _intakeRunStatus.Set(RunStatus.PAUSE(), RunStatus.Name.PAUSE)

            delay(2)  //!  need to calibrate this delay for maximum efficiency
            return _requestRunStatus.GetId() == RunStatus.PAUSE()
        }
        return true;
    }
    private fun DoTerminateRequest(): Boolean
    {
        return _requestRunStatus.GetTerminationId() == RunStatus.DO_TERMINATE()
    }
    private fun TerminateRequest(): RequestResult.Name
    {
        _requestRunStatus.SetTermination(
            RunStatus.IS_ACTIVE(),
            RunStatus.TerminationStatus.IS_ACTIVE
        )

        return RequestResult.Name.PROCESS_WAS_TERMINATED;
    }



    suspend fun ShootEntireDrumRequest(): RequestResult.Name
    {
            if (DoTerminateRequest()) TerminateRequest()    //  Does not terminate, only resets _requestRunStatus
        while (RequestRaceConditionIsPresent()) ;
            if (DoTerminateRequest()) return TerminateRequest()

        ShootEverything()

        ResumeLogic()
        return RequestResult.Name.SUCCESS_IS_NOW_EMPTY
    }
    suspend fun ShootEntireDrumRequest(
        requestOrder: Array<BallRequest.Name?>,
        shotType: ShotType?
    ): RequestResult.Name?
    {
        return ShootEntireDrumRequest(requestOrder, requestOrder, shotType)
    }
    @JvmOverloads
    suspend fun ShootEntireDrumRequest(
        requestOrder: Array<BallRequest.Name?>,
        failsafeOrder: Array<BallRequest.Name?> = requestOrder,
        shotType: ShotType? = ShotType.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
    ): RequestResult.Name?
    {
            if (DoTerminateRequest()) TerminateRequest()    //  Does not terminate, only resets _requestRunStatus
        while (RequestRaceConditionIsPresent()) ;
            if (DoTerminateRequest()) return TerminateRequest()

        val requestResult: RequestResult.Name? =
            when (shotType)
            {
                ShotType.FIRE_EVERYTHING_YOU_HAVE -> ShootEverything()
                ShotType.FIRE_PATTERN_CAN_SKIP -> ShootEntireRequestCanSkip(requestOrder, failsafeOrder)
                ShotType.FIRE_UNTIL_PATTERN_IS_BROKEN -> ShootEntireUntilPatternBreaks(requestOrder, failsafeOrder)
                else  //if (shotType == ShotType.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID)
                    -> ShootEntireRequestIsValid(requestOrder, failsafeOrder)
            }

        ResumeLogic()
        return requestResult
    }



    private fun ShootEverything(): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_IS_EMPTY

        var i = 0
        while (i < 3)
        {
                if (DoTerminateRequest()) return TerminateRequest()

            val requestResult = _storage.HandleRequest(BallRequest.Name.ANY)

            if (UpdateAfterRequest(requestResult))
            {
                while (!_shotWasFired) ; _shotWasFired = false  //!  Maybe improve this later
                _storage.EmptyWasFired(_storageOffset)
                shootingResult = RequestResult.Name.SUCCESS_IS_NOW_EMPTY
            }
            else i += 3 //  Fast break if barrel is empty
            i++
        }
        return if (shootingResult == RequestResult.Name.SUCCESS_IS_NOW_EMPTY)
                   shootingResult else RequestResult.Name.FAIL_IS_EMPTY
    }



    private fun ShootEntireRequestCanSkip(
        requestOrder: Array<BallRequest.Name?>,
        failsafeOrder: Array<BallRequest.Name?>
    ): RequestResult.Name? 
    {
        var shootingResult = ShootEntireCanSkipLogic(requestOrder)

        if (RequestResult.DidFail(shootingResult))
        {
                if (DoTerminateRequest()) return TerminateRequest()
            shootingResult = ShootEntireCanSkipLogic(failsafeOrder)
        }

        return if (RequestResult.DidSucceed(shootingResult))
            if (_storage.AnyBallCount() > 0) 
                RequestResult.Name.SUCCESS else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        else shootingResult
    }
    private fun ShootEntireCanSkipLogic(requestOrder: Array<BallRequest.Name?>): RequestResult.Name 
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        for (i in 0..2) 
        {
                if (DoTerminateRequest()) return TerminateRequest()

            val requestResult = _storage.HandleRequest(requestOrder[i])

            if (UpdateAfterRequest(requestResult)) 
            {
                while (!_shotWasFired) ; _shotWasFired = false  //!  Maybe improve this later
                _storage.EmptyWasFired(_storageOffset)
                shootingResult = RequestResult.Name.SUCCESS
            }
        }
        return shootingResult
    }



    private fun ShootEntireUntilPatternBreaks(
        requestOrder: Array<BallRequest.Name?>,
        failsafeOrder: Array<BallRequest.Name?>
    ): RequestResult.Name? 
    {
        var shootingResult = ShootEntireUntilBreaksLogic(requestOrder)

        if (RequestResult.DidFail(shootingResult))
        {
                if (DoTerminateRequest()) return TerminateRequest()
            shootingResult = ShootEntireUntilBreaksLogic(failsafeOrder)
        }

        return if (RequestResult.DidSucceed(shootingResult))
            if (_storage.AnyBallCount() > 0) 
                RequestResult.Name.SUCCESS else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        else shootingResult
    }
    private fun ShootEntireUntilBreaksLogic(requestOrder: Array<BallRequest.Name?>): RequestResult.Name 
    {
        var shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT

        var i = 0
        while (i < 3)
        {
                if (DoTerminateRequest()) return TerminateRequest()

            val requestResult = _storage.HandleRequest(requestOrder[i])

            if (UpdateAfterRequest(requestResult))
            {
                while (!_shotWasFired) ; _shotWasFired = false  //!  Maybe improve this later
                _storage.EmptyWasFired(_storageOffset)
                shootingResult = RequestResult.Name.SUCCESS
            }
            else i += 3 //  Fast break if barrel is empty

            i++
        }
        return shootingResult
    }



    private fun ShootEntireRequestIsValid(
        requestOrder: Array<BallRequest.Name?>,
        failsafeOrder: Array<BallRequest.Name?>
    ): RequestResult.Name?
    {
        val countPG = _storage.BallColorCountPG()
        var requestCountPGA = CountPGA(requestOrder)

        if (ValidateEntireRequest(countPG, requestCountPGA))  //  Normal order failed
        {
            requestCountPGA = CountPGA(failsafeOrder)


            if (ValidateEntireRequest(countPG, requestCountPGA))  //  Failsafe order also failed
                return RequestResult.Name.FAIL_NOT_ENOUGH_COLORS
            if (DoTerminateRequest()) return TerminateRequest()   //  Process termination


            return ShootEntireValidRequestLogic(failsafeOrder)  //  Try failsafe order
        }

        return ShootEntireValidRequestLogic(requestOrder)
    }

    private fun CountPGA(requestOrder: Array<BallRequest.Name?>): IntArray
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
    private fun ValidateEntireRequest(countPG: IntArray, requestCountPGA: IntArray): Boolean
    {
        val storageDeltaAfterRequests = intArrayOf(
            countPG[0] - requestCountPGA[0],
            countPG[1] - requestCountPGA[1]
        )

        return storageDeltaAfterRequests[0] < 0 || storageDeltaAfterRequests[1] < 0 ||
                storageDeltaAfterRequests[0] + storageDeltaAfterRequests[1] < requestCountPGA[2]
    }
    private fun ShootEntireValidRequestLogic(requestOrder: Array<BallRequest.Name?>): RequestResult.Name
    {
        var requestResult: RequestResult
        for (i in 0..2)
        {
                if (DoTerminateRequest()) return TerminateRequest()

            requestResult = _storage.HandleRequest(requestOrder[i])

            if (UpdateAfterRequest(requestResult))
            {
                while (!_shotWasFired) ; _shotWasFired = false  //!  Maybe improve this later
                _storage.EmptyWasFired(_storageOffset)
            }
            else return RequestResult.Name.FAIL_UNKNOWN
        }
        return if (_storage.AnyBallCount() > 0) RequestResult.Name.SUCCESS
        else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
    }





    fun Storage(): Array<Ball?>?
    {
        return _storage.Storage()
    }

    fun BallColorCountPG(): IntArray
    {
        return _storage.BallColorCountPG()
    }

    fun AnyBallCount(): Int
    {
        return _storage.AnyBallCount()
    }

    fun BallCount(): Int
    {
        return _storage.AnyBallCount()
    }

    fun PurpleBallCount(): Int
    {
        return _storage.SelectedBallCount(Ball.Name.PURPLE)
    }

    fun GreenBallCount(): Int
    {
        return _storage.SelectedBallCount(Ball.Name.GREEN)
    }



    fun StopAnyLogic()
    {
        _runStatus.Set(RunStatus.Name.PAUSE)
    }
    fun ResumeLogic()
    {
        _runStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE())

        _intakeRunStatus.Set(RunStatus.ACTIVE(), RunStatus.Name.ACTIVE)
        _requestRunStatus.Set(RunStatus.ACTIVE(), RunStatus.Name.ACTIVE)
    }
}
*/