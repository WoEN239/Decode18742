package barrel;

import barrel.enumerators.Ball;
import barrel.enumerators.BallRequest;

import barrel.enumerators.StorageSlot;
import barrel.enumerators.StorageOffset;

import barrel.enumerators.IntakeResult;
import barrel.enumerators.RequestResult;


public class BarrelStorage
{
    private Ball[] _storage;
    private StorageOffset _storageOffset;


    public BarrelStorage(Ball[] balls)
    {
        if (balls.length == 3)
            for (int i = 0; i < 3; i++)
                _storage[i].Set(balls[i].GetId(), balls[i].GetName());
    }
    public BarrelStorage()
    {
        _storage = new Ball[3];
    }


    public Ball[] Storage()
    {
        return _storage;
    }
    public StorageOffset CurrentOffset()
    {
        return _storageOffset;
    }
    public StorageOffset.Name CurrentOffsetName()
    {
        return _storageOffset.GetName();
    }
    public void ChangeOffset(StorageOffset.Name newOffset)
    {
        _storageOffset.Set(newOffset);
    }


    public IntakeResult HandleInput()
    {
        IntakeResult result = new IntakeResult();
        result.Set(IntakeResult.Name.FAIL_STORAGE_IS_FULL);

        for (int i = 0; i < 3; i++)
        {
            if (_storage[i].GetName() == Ball.Name.NONE)
            {
                result.Set(i);
                if (i == 1) i += 2;  //  Fast break
                //  Preferring the center slot = closest to input chamber
            }
        }

        return result;
    }
    public RequestResult HandleRequest(BallRequest.Name request)
    {
        int requestBuffer = BallRequest.ToInt(request);
        if (requestBuffer == 3)  //  RequestResult.Name. ANY (int 3)
        {   //  Optimised comparing by id without extra unnecessary conversions

            RequestResult requestResult =  RequestSearch(Ball.Name.PURPLE);
            if (requestResult.GetName() == RequestResult.Name.FAIL_COLOR_NOT_PRESENT)
                return RequestSearch(Ball.Name.GREEN);

            return requestResult;
        }
        else return RequestSearch(Ball.ToName(requestBuffer));
    }
    private RequestResult RequestSearch(Ball.Name requested)
    {
        RequestResult result = new RequestResult();
        result.Set(RequestResult.Name.FAIL_COLOR_NOT_PRESENT);

        for (int i = 0; i < 3; i++)
        {
            if (_storage[i].GetName() == requested)
            {
                result.Set(i);
                if (i != 1) i += 3;  //  Fast break
                //  Preferring slots that are closer to the shooting chamber
            }
        }

        return result;
    }



    public boolean IntakeToCenter(Ball.Name inputBall)
    {
        if (_storage[StorageSlot.CENTER()].GetName() == Ball.Name.NONE)
        {
            _storage[StorageSlot.CENTER()].Set(inputBall);
            return true;
        }
        else return false;
    }




    public void RotateCW()
    {
        RotateLeft();
    }
    public void RotateCCW()
    {
        RotateRight();
    }
    public void RotateLeft()
    {
        Ball buffer = _storage[StorageSlot.LEFT()];
        _storage[StorageSlot.LEFT  ()] = _storage[StorageSlot.CENTER()];
        _storage[StorageSlot.CENTER()] = _storage[StorageSlot.RIGHT ()];
        _storage[StorageSlot.RIGHT ()] = buffer;
    }
    public void RotateRight()
    {
        Ball buffer = _storage[StorageSlot.RIGHT()];
        _storage[StorageSlot.RIGHT ()] = _storage[StorageSlot.CENTER()];
        _storage[StorageSlot.CENTER()] = _storage[StorageSlot.LEFT  ()];
        _storage[StorageSlot.LEFT  ()] = buffer;
    }
}
