package org.woen.modules.storage;

import barrel.enumerators.Ball;
import barrel.enumerators.StorageSlot;



public class MobileSlot
{
    private StorageSlot _slotState = new StorageSlot();
    private Ball _ball = new Ball();



    public void SetSlot(StorageSlot.Name newSlot)
    {
        _slotState.Set(newSlot);
    }
    public void MoveSlot()
    {
        if  (_slotState.Is_MOBILE_OUT())
             _slotState.Set(StorageSlot.Name.MOBILE_IN, StorageSlot.MOBILE_IN);
        else _slotState.Set(StorageSlot.Name.MOBILE_OUT, StorageSlot.MOBILE_OUT);
    }

    public void FillSlot(Ball.Name ball)
    {
        _ball.Set(ball);
    }
    public void EmptySlot()
    {
        _ball.Empty();
    }




    public StorageSlot SlotState() {
        return _slotState;
    }
    public int SlotStateId()
    {
        return _slotState.Id();
    }
    public StorageSlot.Name SlotStateName()
    {
        return _slotState.Name();
    }

    public boolean IsSlotOut()
    {
        return _slotState.Is_MOBILE_OUT();
    }
    public boolean IsSlotIn()
    {
        return _slotState.Is_MOBILE_IN();
    }



    public Ball Ball()
    {
        return _ball;
    }
    public int  BallId()
    {
        return _ball.Id();
    }
    public Ball.Name BallName()
    {
        return _ball.Name();
    }
    public boolean IsEmpty()
    {
        return _ball.IsEmpty();
    }
    public boolean IsFilled()
    {
        return _ball.IsFilled();
    }



    public int Count()
    {
        return _ball.Id() == 0 ? 0 : 1;
    }
}