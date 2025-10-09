package barrel;


/*
import barrel.enumerators.Ball;
import barrel.enumerators.BallRequest;

import barrel.enumerators.StorageOffset;
import barrel.enumerators.StorageSlot;

import barrel.enumerators.IntakeResult;
import barrel.enumerators.RequestResult;


public class BarrelStorage
{
    private Ball[] _storage;


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
    public int AnyBallCount()
    {
        int count = 0;

        for (int slotId = 0; slotId < 3; slotId++)
            if (_storage[slotId].GetId() != Ball.NONE())
                count++;

        return count;
    }
    public int SelectedBallCount(Ball.Name ball)
    {
        int count = 0;

        for (int slotId = 0; slotId < 3; slotId++)
            if (_storage[slotId].GetId() == Ball.PURPLE())
                count++;

        return count;
    }
    public int[] BallColorCountPG()
    {
        int[] countPG = new int[] { 0, 0 };

        for (int i = 0; i < 3; i++)
        {
            if      (_storage[i].GetId() == Ball.PURPLE()) countPG[0]++;
            else if (_storage[i].GetId() == Ball.GREEN())  countPG[1]++;
        }
        return countPG;
    }



    public IntakeResult HandleInput()
    {
        IntakeResult result = new IntakeResult();
        result.Set(IntakeResult.Name.FAIL_STORAGE_IS_FULL);

        for (int i = 0; i < 3; i++)
        {
            if (_storage[i].GetId() == Ball.NONE())
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
        if (requestBuffer == BallRequest.ANY())
        {   //  Optimised comparing by id without extra unnecessary conversions

            RequestResult requestResult =  RequestSearch(Ball.Name.PURPLE);
            if (requestResult.GetId() == RequestResult.FAIL_COLOR_NOT_PRESENT())
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
                if (i == 0) i += 3;  //  Fast break
                //  Preferring the Left (closest) slot to shooting
            }
        }

        return result;
    }



    public boolean IntakeToCenter(Ball.Name inputBall)
    {
        if (_storage[StorageSlot.CENTER()].GetId() == Ball.NONE())
        {
            _storage[StorageSlot.CENTER()].Set(inputBall);
            return true;
        }
        else return false;
    }
    public void EmptyWasFired(StorageOffset storageOffset)
    {
        int slot;
        if (storageOffset.GetId() == StorageOffset.CCW_60()) slot = StorageSlot.LEFT();
        else slot = StorageSlot.RIGHT();

        _storage[slot].Set(Ball.Name.NONE, Ball.NONE());
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
*/