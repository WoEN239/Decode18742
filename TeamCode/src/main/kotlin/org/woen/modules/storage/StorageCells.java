package org.woen.modules.storage;

import barrel.enumerators.Ball;
import barrel.enumerators.BallRequest;

import barrel.enumerators.IntakeResult;
import barrel.enumerators.RequestResult;

import barrel.enumerators.StorageSlot;



public class StorageCells
{
    private Ball[] _storageCells;  //  0-Bottom, 1-center, 2-mobile_out, 3-mobile_in
    private MobileSlot _mobileSlot;



    public StorageCells()
    {
        _storageCells = new Ball[4];
    }

    public IntakeResult HandleIntake()
    {
        IntakeResult result = new IntakeResult(IntakeResult.Name.FAIL_STORAGE_IS_FULL, IntakeResult.FAIL_STORAGE_IS_FULL);

        for (int i = StorageSlot.BOTTOM; i < StorageSlot.MOBILE_IN; i++)
        {
            if (_storageCells[i].GetId() == Ball.NONE)
            {
                result.Set(i);
                if (i != StorageSlot.MOBILE_OUT) i += 3;  //  Fast break
                //  Preferring the closest slot possible to intake
            }
        }

        return result;
    }
    public RequestResult HandleRequest(BallRequest.Name request)
    {
        int requestBuffer =  BallRequest.ToInt(request);
        if (requestBuffer == BallRequest.ANY)
        {   //  Optimised comparing by id without extra unnecessary conversions

            RequestResult requestResult = RequestSearch(Ball.Name.PURPLE);
            if (requestResult.GetId() == RequestResult.FAIL_COLOR_NOT_PRESENT)
                return RequestSearch(Ball.Name.GREEN);

            return requestResult;
        }
        else return RequestSearch(Ball.ToName(requestBuffer));
    }
    private RequestResult RequestSearch(Ball.Name requested)
    {
        RequestResult result = new RequestResult(RequestResult.Name.FAIL_COLOR_NOT_PRESENT, RequestResult.FAIL_COLOR_NOT_PRESENT);

        for (int i = StorageSlot.BOTTOM; i < StorageSlot.MOBILE_IN; i++)
        {
            if (_storageCells[i].GetName() == requested)
            {
                result.Set(i);
                if (i != StorageSlot.BOTTOM) i += 3;  //  Fast break
                //  Preferring the closest slot to shooting
            }
        }

        return result;
    }




    public Ball[] StorageRaw()
    {
        return _storageCells;
    }
    public Ball[] StorageFiltered()
    {
        return new Ball[]
        {
            _storageCells[StorageSlot.BOTTOM],
            _storageCells[StorageSlot.CENTER],
            _mobileSlot.Ball()
        };
    }
    public int AnyBallCount()
    {
        int count = 0;

        if (_mobileSlot.BallId() != Ball.NONE) count++;
        for (int slotId = StorageSlot.BOTTOM; slotId < StorageSlot.MOBILE_OUT; slotId++)
            if (_storageCells[slotId].GetId() != Ball.NONE) count++;

        return count;
    }
    public int SelectedBallCount(Ball.Name ball)
    {
        int count = 0;

        if (_mobileSlot.BallName() == ball) count++;
        for (int slotId = StorageSlot.BOTTOM; slotId < StorageSlot.MOBILE_OUT; slotId++)
            if (_storageCells[slotId].GetName() == ball) count++;

        return count;
    }
    public int[] BallColorCountPG()
    {
        int[] countPG = new int[] { 0, 0, 0 };

        countPG[_mobileSlot.BallId()]++;
        for (int i = StorageSlot.BOTTOM; i < StorageSlot.MOBILE_OUT; i++)
            countPG[_storageCells[i].GetId()]++;

        return new int[] { countPG[Ball.PURPLE], countPG[Ball.GREEN] };
    }
}