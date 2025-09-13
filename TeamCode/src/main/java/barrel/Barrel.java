package barrel;

import barrel.enumerators.Ball;
import barrel.enumerators.BallRequest;

import barrel.enumerators.IntakeResult;
import barrel.enumerators.RequestResult;

import barrel.enumerators.RunStatus;
import barrel.enumerators.StorageOffset;

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
            while (RealignToIntakePosition()) ;

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
        if (intakeResult.GetName() == IntakeResult.Name.FAIL_STORAGE_IS_FULL)
            return false;  //  Intake failed

        //  Align center slot to be empty
        if (intakeResult.GetName() == IntakeResult.Name.SUCCESS_LEFT)
        {
            _storage.RotateRight();
            //  Rotate the motor to the right by 120
        }
        else if (intakeResult.GetName() == IntakeResult.Name.SUCCESS_RIGHT)
        {
            _storage.RotateLeft();
            //  Rotate the motor to the left  by 120
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

            //  wait for shot fired event

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
        if (requestResult.GetName() == RequestResult.Name.FAIL_COLOR_NOT_PRESENT)
            return false;


        StorageOffset.Name currentOffset = _storage.CurrentOffset().GetName();
        if (requestResult.GetName() == RequestResult.Name.SUCCESS_LEFT)
        {
            if (currentOffset == StorageOffset.Name.ALIGNED_TO_INTAKE)
            {
                //  Rotate the motor by 60  CW
            }
            else if (currentOffset == StorageOffset.Name.OFFSET_60_CCW)
            {
                //  Rotate the motor by 120 CW
            }

            _storage.ChangeOffset(StorageOffset.Name.OFFSET_60_CW);
        }
        else if (requestResult.GetName() == RequestResult.Name.SUCCESS_RIGHT)
        {
            if (currentOffset == StorageOffset.Name.ALIGNED_TO_INTAKE)
            {
                //  Rotate the motor by 60  CCW
            }
            else if (currentOffset == StorageOffset.Name.OFFSET_60_CW)
            {
                //  Rotate the motor by 120 CCW
            }

            _storage.ChangeOffset(StorageOffset.Name.OFFSET_60_CCW);
        }
        else
        {
            _storage.RotateCCW();
            _storage.ChangeOffset(StorageOffset.Name.OFFSET_60_CCW);
            //  Rotate the motor by 180 CW
        }
        return true;
    }
    private boolean RealignToIntakePosition()
    {
        if (runStatus.GetName() == RunStatus.Name.PAUSE) return false;

        StorageOffset.Name storageOffset = _storage.CurrentOffsetName();
        if (storageOffset == StorageOffset.Name.OFFSET_60_CW)
        {
            //  Rotate the motor by 60 CCW
        }
        else if (storageOffset == StorageOffset.Name.OFFSET_60_CCW)
        {
            //  Rotate the motor by 60 CW
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
