/*package barrel;

import org.woen.threading.ThreadedEventBus;
import org.woen.threading.hardware.HardwareThreads;

import org.woen.modules.barrel.HardwareBarrel;
import com.qualcomm.robotcore.hardware.HardwareMap;

import barrel.enumerators.Ball;
import barrel.enumerators.BallRequest;

import barrel.enumerators.IntakeResult;
import barrel.enumerators.RequestResult;

import barrel.enumerators.ShotType;
import barrel.enumerators.RunStatus;


public class Barrel
{
    private RunStatus _runStatus;
    private BarrelStorage _storage;
    private HardwareBarrel _barrelMotor;

    final double CW_120 = 120.0, CCW_120 = -120.0;  //  120 degrees motor rotation constants constants



    public Barrel(HardwareMap hwMap, String deviceName, int direction)
    {
        _storage = new BarrelStorage();
        _runStatus = new RunStatus();

        _barrelMotor = new HardwareBarrel(deviceName, direction);
        _barrelMotor.init(hwMap);
        HardwareThreads.getLAZY_INSTANCE().getEXPANSION().addDevices(_barrelMotor);
    }
    public void Start()
    {
        if (_runStatus.Name() != RunStatus.Name.PAUSE)
            _runStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE());
    }



    public IntakeResult.Name HandleInput(Ball.Name inputBall)
    {
        if (_runStatus.Name() == RunStatus.Name.ACTIVE)
        {
            //  Storage search for empty slots
            IntakeResult intakeResult = _storage.HandleInput();

            //  Safe updating after intake
            if (!UpdateAfterInput(intakeResult, inputBall))
                intakeResult.Set(IntakeResult.Name.FAIL_UNKNOWN, IntakeResult.FAIL_UNKNOWN());

            return intakeResult.DidSucceed() ? IntakeResult.Name.SUCCESS : intakeResult.Name();
        }

        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY;
    }
    public IntakeResult.Name HandleInput(Ball inputBall)
    {
        return HandleInput(inputBall.Name());
    }
    private boolean UpdateAfterInput(IntakeResult intakeResult, Ball.Name inputBall)
    {
        IntakeResult.Name name = intakeResult.Name();

        if (name == IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY ||
            name == IntakeResult.Name.FAIL_STORAGE_IS_FULL ||
            name == IntakeResult.Name.FAIL_UNKNOWN)
            return false;  //  Intake failed

        //  Align center slot to be empty
        if (name == IntakeResult.Name.SUCCESS_LEFT)
        {
            _storage.RotateCCW();
            _barrelMotor.Rotate(CCW_120);
        }
        else if (name == IntakeResult.Name.SUCCESS_RIGHT)
        {
            _storage.RotateCW();
            _barrelMotor.Rotate(CW_120);
        }

        return _storage.IntakeToCenter(inputBall);  //  Safe intake
    }



    public RequestResult.Name HandleRequest(BallRequest.Name request)
    {
        if (_runStatus.Name() == RunStatus.Name.ACTIVE)
        {
            StopAnyLogic();

            RequestResult requestResult = _storage.HandleRequest(request);
            if (UpdateAfterRequest(requestResult))
            {
                //!  Wait for shot fired event
                _storage.EmptyLeftWasFired();
            }
            //  Unknown error

            ResumeLogic();
            return requestResult.DidSucceed() ? _storage.AnyBallCount() > 0 ?
                            RequestResult.Name.SUCCESS : RequestResult.Name.SUCCESS_IS_NOW_EMPTY
                    : requestResult.Name();
        }

        return RequestResult.Name.FAIL_IS_CURRENTLY_BUSY;
    }
    public RequestResult.Name HandleRequest(BallRequest request)
    {
        return HandleRequest(request.Name());
    }
    private boolean UpdateAfterRequest(RequestResult requestResult)
    {
        if (requestResult.DidFail()) return false;

        if (requestResult.Name() == RequestResult.Name.SUCCESS_RIGHT)
        {
            _barrelMotor.Rotate(CCW_120);
            _storage.RotateCCW();
        }
        else if (requestResult.Name() == RequestResult.Name.SUCCESS_CENTER)
        {
            _barrelMotor.Rotate(CW_120);
            _storage.RotateCW();
        }

        //!  Send ball is ready request

        return true;
    }



    public RequestResult.Name ShootEntireDrumRequest()
    {
        if (_runStatus.Name() == RunStatus.Name.ACTIVE)
        {
            StopAnyLogic();
            ShootEverything();
            return RequestResult.Name.SUCCESS_IS_NOW_EMPTY;
        }
        return RequestResult.Name.FAIL_IS_CURRENTLY_BUSY;
    }
    public RequestResult.Name ShootEntireDrumRequest(BallRequest.Name[] requestOrder)
    {
        return ShootEntireDrumRequest(requestOrder, requestOrder, ShotType.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID);
    }
    public RequestResult.Name ShootEntireDrumRequest(BallRequest.Name[] requestOrder, ShotType shotType)
    {
        return ShootEntireDrumRequest(requestOrder, requestOrder, shotType);
    }
    public RequestResult.Name ShootEntireDrumRequest(BallRequest.Name[] requestOrder, BallRequest.Name[] failsafeOrder, ShotType shotType)
    {
        if (_runStatus.Name() == RunStatus.Name.ACTIVE)
        {
            StopAnyLogic();

            RequestResult.Name requestResult;
            if (shotType == ShotType.FIRE_EVERYTHING_YOU_HAVE)
                requestResult = ShootEverything();

            else if (shotType == ShotType.FIRE_PATTERN_CAN_SKIP)
                requestResult = ShootEntireRequestCanSkip(requestOrder, failsafeOrder);

            else if (shotType == ShotType.FIRE_UNTIL_PATTERN_IS_BROKEN)
                requestResult = ShootEntireUntilPatternBreaks(requestOrder, failsafeOrder);

            else //if (shotType == ShotType.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID)
                requestResult = ShootEntireRequestIsValid(requestOrder, failsafeOrder);

            ResumeLogic();
            return requestResult;
        }

        return RequestResult.Name.FAIL_IS_CURRENTLY_BUSY;
    }



    private RequestResult.Name ShootEverything()
    {
        RequestResult.Name shootingResult = RequestResult.Name.FAIL_IS_EMPTY;

        for (int i = 0; i < 3; i++)
        {
            RequestResult requestResult = _storage.HandleRequest(BallRequest.Name.ANY);

            if (UpdateAfterRequest(requestResult))
            {
                //!  Wait for shot fired event
                _storage.EmptyLeftWasFired();
                shootingResult = RequestResult.Name.SUCCESS_IS_NOW_EMPTY;
            }
            else i += 3;  //  Fast break if barrel is empty
        }
        return shootingResult == RequestResult.Name.SUCCESS_IS_NOW_EMPTY ?
                shootingResult : RequestResult.Name.FAIL_IS_EMPTY;
    }


    private RequestResult.Name ShootEntireRequestCanSkip(BallRequest.Name[] requestOrder, BallRequest.Name[] failsafeOrder)
    {
        RequestResult.Name shootingResult = ShootEntireCanSkipLogic(requestOrder);

        if (RequestResult.DidFail(shootingResult))
            shootingResult = ShootEntireCanSkipLogic(failsafeOrder);

        return RequestResult.DidSucceed(shootingResult) ? _storage.AnyBallCount() > 0 ?
                    RequestResult.Name.SUCCESS : RequestResult.Name.SUCCESS_IS_NOW_EMPTY
               : shootingResult;
    }
    private RequestResult.Name ShootEntireCanSkipLogic(BallRequest.Name[] requestOrder)
    {
        RequestResult.Name shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT;

        for (int i = 0; i < 3; i++)
        {
            RequestResult requestResult = _storage.HandleRequest(requestOrder[i]);

            if (UpdateAfterRequest(requestResult))
            {
                //!  Wait for shot fired event
                _storage.EmptyLeftWasFired();
                shootingResult = RequestResult.Name.SUCCESS;
            }
        }
        return shootingResult;
    }


    private RequestResult.Name ShootEntireUntilPatternBreaks(BallRequest.Name[] requestOrder, BallRequest.Name[] failsafeOrder)
    {
        RequestResult.Name shootingResult = ShootEntireUntilBreaksLogic(requestOrder);

        if (RequestResult.DidFail(shootingResult))
            shootingResult = ShootEntireUntilBreaksLogic(failsafeOrder);

        return RequestResult.DidSucceed(shootingResult) ? _storage.AnyBallCount() > 0 ?
                RequestResult.Name.SUCCESS : RequestResult.Name.SUCCESS_IS_NOW_EMPTY
                : shootingResult;
    }
    private RequestResult.Name ShootEntireUntilBreaksLogic(BallRequest.Name[] requestOrder)
    {
        RequestResult.Name shootingResult = RequestResult.Name.FAIL_COLOR_NOT_PRESENT;

        for (int i = 0; i < 3; i++)
        {
            RequestResult requestResult = _storage.HandleRequest(requestOrder[i]);

            if (UpdateAfterRequest(requestResult))
            {
                //!  Wait for shot fired event
                _storage.EmptyLeftWasFired();
                shootingResult = RequestResult.Name.SUCCESS;
            }
            else i += 3;  //  Fast break if barrel is empty
        }
        return shootingResult;
    }


    private RequestResult.Name ShootEntireRequestIsValid(BallRequest.Name[] requestOrder, BallRequest.Name[] failsafeOrder)
    {
        int[] countPG = _storage.BallColorCountPG();
        int[] requestCountPGA = CountPGA(requestOrder);

        if (ValidateEntireRequest(countPG, requestCountPGA))  //  Normal order failed
        {
            requestCountPGA = CountPGA(failsafeOrder);

            if (ValidateEntireRequest(countPG, requestCountPGA))  //  Failsafe order also failed
                return RequestResult.Name.FAIL_NOT_ENOUGH_COLORS;

            return ShootEntireValidRequestLogic(failsafeOrder);
        }

        return ShootEntireValidRequestLogic(requestOrder);
    }
    private int[] CountPGA (BallRequest.Name[] requestOrder)
    {
        int[] countPGA = new int[] { 0, 0, 0 };

        for (int i = 0; i < 3; i++)
        {
            if      (requestOrder[i] == BallRequest.Name.PURPLE) countPGA[0]++;
            else if (requestOrder[i] == BallRequest.Name.GREEN)  countPGA[1]++;
            else if (requestOrder[i] == BallRequest.Name.ANY)    countPGA[2]++;
        }
        return countPGA;
    }
    private boolean ValidateEntireRequest(int[] countPG, int[] requestCountPGA)
    {
        int[] storageDeltaAfterRequests = new int[]
        {
            countPG[0] - requestCountPGA[0],
            countPG[1] - requestCountPGA[1]
        };

        return storageDeltaAfterRequests[0] < 0 || storageDeltaAfterRequests[1] < 0 ||
            storageDeltaAfterRequests[0] + storageDeltaAfterRequests[1] < requestCountPGA[2];
    }
    private RequestResult.Name ShootEntireValidRequestLogic(BallRequest.Name[] requestOrder)
    {
        RequestResult requestResult;
        for (int i = 0; i < 3; i++)
        {
            requestResult = _storage.HandleRequest(requestOrder[i]);

            if (UpdateAfterRequest(requestResult))
            {
                //!  Wait for successfully gunshot event
                _storage.EmptyLeftWasFired();
            }
            else return RequestResult.Name.FAIL_UNKNOWN;
        }
        return _storage.AnyBallCount() > 0 ? RequestResult.Name.SUCCESS : RequestResult.Name.SUCCESS_IS_NOW_EMPTY;
    }



    public Ball[] Storage()
    {
        return _storage.Storage();
    }
    public int[] BallColorCountPG()
    {
        return _storage.BallColorCountPG();
    }
    public int AnyBallCount()
    {
        return _storage.AnyBallCount();
    }
    public int BallCount()
    {
        return _storage.AnyBallCount();
    }
    public int PurpleBallCount()
    {
        return _storage.SelectedBallCount(Ball.Name.PURPLE);
    }
    public int GreenBallCount()
    {
        return _storage.SelectedBallCount(Ball.Name.GREEN);
    }



    public void StopAnyLogic()
    {
        _runStatus.Set(RunStatus.Name.PAUSE);
    }
    public void ResumeLogic()
    {
        _runStatus.Set(RunStatus.Name.ACTIVE);
    }
}
*/