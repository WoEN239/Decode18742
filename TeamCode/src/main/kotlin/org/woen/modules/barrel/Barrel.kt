package org.woen.modules.barrel

import barrel.BarrelStorage
import barrel.enumerators.Ball
import barrel.enumerators.BallRequest

import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import barrel.enumerators.ShotType
import barrel.enumerators.RunStatus

import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.threading.hardware.HardwareThreads.Companion.LAZY_INSTANCE


class Barrel (hwMap: HardwareMap, deviceName: String, direction: Int)
{
    private val _runStatus: RunStatus
    private val _storage: BarrelStorage
    private val _barrelMotor: HardwareBarrel

    val CW_120: Double = 120.0; val CCW_120: Double = -120.0 //  120 degrees motor rotation constants constants



    init
    {
        _storage   = BarrelStorage()
        _runStatus = RunStatus()

        _barrelMotor = HardwareBarrel(deviceName, direction)
        _barrelMotor.init(hwMap)
        LAZY_INSTANCE.EXPANSION.addDevices(_barrelMotor)
    }
    fun Start()
    {
        if (_runStatus.GetName() != RunStatus.Name.PAUSE)
            _runStatus.Set(RunStatus.Name.ACTIVE,RunStatus.ACTIVE())
    }



    fun HandleInput(inputBall: Ball.Name?): IntakeResult.Name?
    {
        if (_runStatus.GetName() == RunStatus.Name.ACTIVE)
        {
            //  Storage search for empty slots
            val intakeResult = _storage.HandleInput()

            //  Safe updating after intake
            if (!UpdateAfterInput(intakeResult, inputBall))
                intakeResult.Set(IntakeResult.Name.FAIL_UNKNOWN, IntakeResult.FAIL_UNKNOWN())

            return if (intakeResult.DidSucceed()) IntakeResult.Name.SUCCESS else intakeResult.GetName()
        }

        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    fun HandleInput(inputBall: Ball): IntakeResult.Name?
    {
        return HandleInput(inputBall.GetName())
    }
    private fun UpdateAfterInput(intakeResult: IntakeResult, inputBall: Ball.Name?): Boolean
    {
        val name = intakeResult.GetName()

        if (IntakeResult.DidFail(name)) return false //  Intake failed


        //  Align center slot to be empty
        if (name == IntakeResult.Name.SUCCESS_LEFT)
        {
            _storage.RotateCCW()
            _barrelMotor.Rotate(CCW_120)
        }
        else if (name == IntakeResult.Name.SUCCESS_RIGHT)
        {
            _storage.RotateCW()
            _barrelMotor.Rotate(CW_120)
        }

        return _storage.IntakeToCenter(inputBall) //  Safe intake
    }



    fun HandleRequest(request: BallRequest.Name?): RequestResult.Name?
    {
        if (_runStatus.GetName() == RunStatus.Name.ACTIVE)
        {
            StopAnyLogic()

            val requestResult = _storage.HandleRequest(request)
            if (UpdateAfterRequest(requestResult))
            {
                //!  Wait for shot fired event
                _storage.EmptyLeftWasFired()
            }
            
            ResumeLogic()
            return if (requestResult.DidSucceed())
                if (_storage.AnyBallCount() > 0) RequestResult.Name.SUCCESS
                else RequestResult.Name.SUCCESS_IS_NOW_EMPTY
            else requestResult.GetName()
        }

        return RequestResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    fun HandleRequest(request: BallRequest): RequestResult.Name?
    {
        return HandleRequest(request.GetName())
    }
    private fun UpdateAfterRequest(requestResult: RequestResult): Boolean
    {
        if (requestResult.DidFail()) return false

        if (requestResult.GetName() == RequestResult.Name.SUCCESS_RIGHT)
        {
            _barrelMotor.Rotate(CCW_120)
            _storage.RotateCCW()
        }
        else if (requestResult.GetName() == RequestResult.Name.SUCCESS_CENTER)
        {
            _barrelMotor.Rotate(CW_120)
            _storage.RotateCW()
        }

        //!  Send ball is ready request
        return true
    }



    fun ShootEntireDrumRequest(): RequestResult.Name
    {
        if (_runStatus.GetName() == RunStatus.Name.ACTIVE)
        {
            StopAnyLogic()

            ShootEverything()

            return RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        }
        return RequestResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    fun ShootEntireDrumRequest(
        requestOrder: Array<BallRequest.Name?>,
        shotType: ShotType?
    ): RequestResult.Name?
    {
        return ShootEntireDrumRequest(requestOrder, requestOrder, shotType)
    }
    @JvmOverloads
    fun ShootEntireDrumRequest(
        requestOrder: Array<BallRequest.Name?>,
        failsafeOrder: Array<BallRequest.Name?> = requestOrder,
        shotType: ShotType? = ShotType.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
    ): RequestResult.Name?
    {
        if (_runStatus.GetName() == RunStatus.Name.ACTIVE)
        {
            StopAnyLogic()

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

        return RequestResult.Name.FAIL_IS_CURRENTLY_BUSY
    }



    private fun ShootEverything(): RequestResult.Name
    {
        var shootingResult = RequestResult.Name.FAIL_IS_EMPTY

        var i = 0
        while (i < 3)
        {
            val requestResult = _storage.HandleRequest(BallRequest.Name.ANY)

            if (UpdateAfterRequest(requestResult))
            {
                //!  Wait for shot fired event
                _storage.EmptyLeftWasFired()
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
            shootingResult = ShootEntireCanSkipLogic(failsafeOrder)

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
            val requestResult = _storage.HandleRequest(requestOrder[i])

            if (UpdateAfterRequest(requestResult)) 
            {
                //!  Wait for shot fired event
                _storage.EmptyLeftWasFired()
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
            shootingResult = ShootEntireUntilBreaksLogic(failsafeOrder)

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
            val requestResult = _storage.HandleRequest(requestOrder[i])

            if (UpdateAfterRequest(requestResult))
            {
                //!  Wait for shot fired event
                _storage.EmptyLeftWasFired()
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

            return ShootEntireValidRequestLogic(failsafeOrder)
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
            requestResult = _storage.HandleRequest(requestOrder[i])

            if (UpdateAfterRequest(requestResult))
            {
                //!  Wait for successfully gunshot event
                _storage.EmptyLeftWasFired()
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
        _runStatus.Set(RunStatus.Name.ACTIVE)
    }
}
