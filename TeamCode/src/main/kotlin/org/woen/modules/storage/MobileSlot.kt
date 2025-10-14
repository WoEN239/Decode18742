package org.woen.modules.storage

import barrel.enumerators.Ball
import barrel.enumerators.StorageSlot


class MobileSlot
{
    private val _slotState = StorageSlot()
    private val _ball: Ball = Ball()



    fun SetSlot(newSlot: StorageSlot.Name)
    {
        _slotState.Set(newSlot)
    }
    fun MoveSlot()
    {
        if (_slotState.Is_MOBILE_OUT()) _slotState.Set(
            StorageSlot.Name.MOBILE_IN,
            StorageSlot.MOBILE_IN
        )
        else _slotState.Set(StorageSlot.Name.MOBILE_OUT, StorageSlot.MOBILE_OUT)
    }

    fun FillSlot(ball: Ball.Name)
    {
        _ball.Set(ball)
    }
    fun EmptySlot()
    {
        _ball.Empty()
    }



    fun SlotState(): StorageSlot
    {
        return _slotState
    }
    fun SlotStateId(): Int
    {
        return _slotState.Id()
    }
    fun SlotStateName(): StorageSlot.Name
    {
        return _slotState.Name()
    }

    fun IsSlotOut(): Boolean
    {
        return _slotState.Is_MOBILE_OUT()
    }
    fun IsSlotIn(): Boolean
    {
        return _slotState.Is_MOBILE_IN()
    }



    fun Ball(): Ball
    {
        return _ball
    }
    fun BallId(): Int
    {
        return _ball.Id()
    }
    fun BallName(): Ball.Name
    {
        return _ball.Name()
    }


    fun IsEmpty(): Boolean
    {
        return _ball.IsEmpty()
    }
    fun IsFilled(): Boolean
    {
        return _ball.IsFilled()
    }



    fun Count(): Int
    {
        return if (_ball.Id() == 0) 0 else 1
    }
}