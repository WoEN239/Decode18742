package org.woen.modules.storage;

import barrel.enumerators.Ball;
import barrel.enumerators.StorageSlot;

public class MobileSlot
{
    private StorageSlot _slot = new StorageSlot();
    private Ball _ball = new Ball();



    public StorageSlot Slot()
    {
        return _slot;
    }
    public int SlotId()
    {
        return _slot.GetId();
    }
    public StorageSlot.Name SlotName()
    {
        return _slot.GetName();
    }



    public Ball Ball()
    {
        return _ball;
    }
    public int  BallId()
    {
        return _ball.GetId();
    }
    public Ball.Name BallName()
    {
        return _ball.GetName();
    }


    public int Count()
    {
        return _ball.GetId() == 0 ? 0 : 1;
    }
}