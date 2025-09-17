package barrel;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EEPROM_232H;
import org.woen.modules.barrel.HardwareBarrel;
import org.woen.threading.hardware.HardwareThreads;

import barrel.enumerators.Ball;
import barrel.enumerators.BallRequest;

import barrel.enumerators.IntakeResult;
import barrel.enumerators.RequestResult;

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
        if (_runStatus.GetName() != RunStatus.Name.PAUSE)
            _runStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE());
    }



    public IntakeResult.Name HandleInput(Ball.Name inputBall)
    {
        if (_runStatus.GetName() == RunStatus.Name.ACTIVE)
        {
            //  Storage search for empty slots
            IntakeResult intakeResult = _storage.HandleInput();

            //  Safe updating after intake
            if (!UpdateAfterInput(intakeResult, inputBall))
                intakeResult.Set(IntakeResult.Name.FAIL_UNKNOWN, IntakeResult.FAIL_UNKNOWN());

            return intakeResult.GetName();
        }

        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY;
    }
    public IntakeResult.Name HandleInput(Ball inputBall)
    {
        return HandleInput(inputBall.GetName());
    }
    private boolean UpdateAfterInput(IntakeResult intakeResult, Ball.Name inputBall)
    {
        IntakeResult.Name name = intakeResult.GetName();

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
        if (_runStatus.GetName() == RunStatus.Name.ACTIVE)
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
            return requestResult.GetName();
        }

        return RequestResult.Name.FAIL_IS_CURRENTLY_BUSY;
    }
    public RequestResult.Name HandleRequest(BallRequest request)
    {
        return HandleRequest(request.GetName());
    }
    private boolean UpdateAfterRequest(RequestResult requestResult)
    {
        RequestResult.Name name = requestResult.GetName();

        if (name == RequestResult.Name.FAIL_IS_CURRENTLY_BUSY ||
            name == RequestResult.Name.FAIL_COLOR_NOT_PRESENT ||
            name == RequestResult.Name.FAIL_UNKNOWN)
            return false;


        if (requestResult.GetName() == RequestResult.Name.SUCCESS_RIGHT)
        {
            _barrelMotor.Rotate(CCW_120);
            _storage.RotateCCW();
        }
        else if (requestResult.GetName() == RequestResult.Name.SUCCESS_CENTER)
        {
            _barrelMotor.Rotate(CW_120);
            _storage.RotateCW();
        }
        return true;
    }



    public RequestResult.Name ShootEntireDrumRequest(BallRequest.Name[] requestOrder)
    {
        if (_runStatus.GetName() == RunStatus.Name.ACTIVE)
        {
            StopAnyLogic();

            int[] countPG  = _storage.BallColorCountPG();
            int[] requestCountPGA = CountPGA(requestOrder);
            int[] storageDeltaAfterRequests = new int[]
                    {
                        countPG[0] - requestCountPGA[0],
                        countPG[1] - requestCountPGA[1]
                    };

            if (storageDeltaAfterRequests[0] < 0 ||
                storageDeltaAfterRequests[1] < 0 ||
                storageDeltaAfterRequests[0] + storageDeltaAfterRequests[1] < requestCountPGA[2])
                    return RequestResult.Name.FAIL_NOT_ENOUGH_COLORS;

            RequestResult requestResult = new RequestResult();

            for (int i = 0; i < 3; i ++)
            {
                requestResult = _storage.HandleRequest(requestOrder[i]);

                if (UpdateAfterRequest(requestResult));
                {
                    //!  Wait for successfully gunshot event
                    _storage.EmptyLeftWasFired();
                }
                //  Unknown error
            }

            ResumeLogic();
            return requestResult.GetName();
        }

        return RequestResult.Name.FAIL_IS_CURRENTLY_BUSY;
    }
    public int[] CountPGA (BallRequest.Name[] requestOrder)
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
