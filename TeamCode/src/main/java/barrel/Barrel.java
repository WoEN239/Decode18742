package barrel;

import barrel.enumerators.Ball;
import barrel.enumerators.BallRequest;

import barrel.enumerators.IntakeResult;
import barrel.enumerators.RequestResult;

import barrel.enumerators.RunStatus;

public class Barrel
{
    private BarrelStorage _storage;
    private RunStatus runStatus;


    public Barrel()
    {
        _storage = new BarrelStorage();
        runStatus = new RunStatus();
    }
    public void Start()
    {
        runStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE());
    }



    public IntakeResult.Name HandleInput(Ball.Name inputBall)
    {
        if (runStatus.GetName() == RunStatus.Name.ACTIVE)
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
            _storage.RotateRight();
            //!  Rotate the motor to the right by 120
        }
        else if (name == IntakeResult.Name.SUCCESS_RIGHT)
        {
            _storage.RotateLeft();
            //!  Rotate the motor to the left  by 120
        }

        return _storage.IntakeToCenter(inputBall);  //  Safe intake
    }



    public RequestResult.Name HandleRequest(BallRequest.Name request)
    {
        if (runStatus.GetName() == RunStatus.Name.ACTIVE)
        {
            StopAnyLogic();

            RequestResult requestResult = _storage.HandleRequest(request);

            UpdateAfterRequest(requestResult);

            //!  Wait for shot fired event

            _storage.EmptyLeftWasFired();

            ResumeLogic();
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
           //!  Rotate motor to the left (CCW 120)

            _storage.RotateCCW();
        }
        else if (requestResult.GetName() == RequestResult.Name.SUCCESS_CENTER)
        {
            //!  Rotate the motor to the right (CW 120)

            _storage.RotateCW();
        }
        return true;
    }


    public void StopAnyLogic()
    {
        runStatus.Set(RunStatus.Name.PAUSE);
    }
    public void ResumeLogic()
    {
        runStatus.Set(RunStatus.Name.ACTIVE);
    }
}
