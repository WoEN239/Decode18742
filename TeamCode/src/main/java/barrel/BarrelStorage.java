package barrel;


import barrel.enumerators.Ball;
import barrel.enumerators.BallRequest;
//import barrel.enumerators.IntakeResult;
import barrel.enumerators.RequestResult;
import barrel.enumerators.StorageSlot;

public class BarrelStorage
{
    private Ball[] _storage;
    private StorageSlot storageSlot;


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



    public void UpdateAfterInput(Ball input)
    {
        //IntakeResult intakeResult;

        //return intakeResult;
    }
    public RequestResult UpdateAfterOutput(BallRequest request)
    {
        int requestBuffer = request.GetId();
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
                if (i != 1) i += 2;  //  Fast break
                // Preferring slots that are closer to the shooting chamber
            }
        }

        return result;
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
        Ball buffer = _storage[storageSlot.LEFT()];
        _storage[storageSlot.LEFT  ()] = _storage[storageSlot.CENTER()];
        _storage[storageSlot.CENTER()] = _storage[storageSlot.RIGHT ()];
        _storage[storageSlot.RIGHT ()] = buffer;
    }
    public void RotateRight()
    {
        Ball buffer = _storage[storageSlot.RIGHT()];
        _storage[storageSlot.RIGHT ()] = _storage[storageSlot.CENTER()];
        _storage[storageSlot.CENTER()] = _storage[storageSlot.LEFT  ()];
        _storage[storageSlot.LEFT  ()] = buffer;
    }
}
