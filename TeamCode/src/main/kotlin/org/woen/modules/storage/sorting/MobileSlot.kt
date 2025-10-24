package org.woen.modules.storage.sorting


import barrel.enumerators.Ball

import barrel.enumerators.StorageSlot
import barrel.enumerators.MobileRotationResult

import barrel.enumerators.MobileGate


class MobileSlot
{
    private val _gate = MobileGate()
    private val _ballSlot = StorageSlot()
    //!  private val _hwMobileSlot = HardwareMobileSlot()

    private val _ball = Ball()



    fun tryRotateSlot(newBallSlot: StorageSlot.Name): MobileRotationResult
    {
        val rotationResult = StorageSlot.MobileCanBeRotated(
            _ballSlot, StorageSlot(newBallSlot)
        )

        return if (rotationResult.DidFail()) rotationResult
        else moveBall(rotationResult)
    }
    private fun moveBall(result: MobileRotationResult): MobileRotationResult
    {
        when (result.Id())
        {
            MobileRotationResult.SUCCESS ->
            {
                _ballSlot.SetMobileIn()
                TODO("Add hardware moving: MOBILE_OUT -> MOBILE_IN")
            }
            MobileRotationResult.SUCCESS_IN ->
            {
                _ballSlot.SetMobileOut()
                TODO("Add hardware moving: OUTSIDE -> MOBILE_OUT")
            }
            MobileRotationResult.SUCCESS_IN_DOUBLE ->
            {
                _ballSlot.SetMobileIn()
                TODO("Add hardware moving: OUTSIDE -> MOBILE_IN")
            }
            MobileRotationResult.SUCCESS_OUT ->
            {
                _ballSlot.SetOutsideMobile()
                TODO("Add hardware moving: MOBILE_OUT -> OUTSIDE")
            }
            MobileRotationResult.SUCCESS_OUT_DOUBLE ->
            {
                _ballSlot.SetOutsideMobile()
                TODO("Add hardware moving: MOBILE_IN -> OUTSIDE")
            }
            else ->
            {
                return MobileRotationResult()
            }
        }

        return result
    }

    fun tryFillSlot(ball: Ball.Name): Boolean
    {
       return tryRotateSlot(StorageSlot.Name.MOBILE_OUT).DidSucceed()
    }
    fun emptySlot(): Boolean
    {
        return tryRotateSlot(StorageSlot.Name.OUTSIDE_MOBILE).DidSucceed()
    }



    fun ballSlotState(): StorageSlot
    {
        return _ballSlot
    }
    fun ballSlotStateId(): Int
    {
        return _ballSlot.Id()
    }
    fun ballSlotStateName(): StorageSlot.Name
    {
        return _ballSlot.Name()
    }

    fun isBallInSlotOut(): Boolean
    {
        return _ballSlot.Is_MOBILE_OUT()
    }
    fun isBallInSlotIn(): Boolean
    {
        return _ballSlot.Is_MOBILE_IN()
    }
    fun isBallOutside(): Boolean
    {
        return _ballSlot.Is_OUTSIDE_MOBILE()
    }



    fun ball(): Ball
    {
        return _ball
    }
    fun ballId(): Int
    {
        return _ball.Id()
    }
    fun ballName(): Ball.Name
    {
        return _ball.Name()
    }


    fun isEmpty(): Boolean
    {
        return _ball.IsEmpty()
    }
    fun isFilled(): Boolean
    {
        return _ball.IsFilled()
    }



    fun ballCount(): Int
    {
        return if (_ball.Id() == 0) 0 else 1
    }





    init
    {
        _ballSlot.SetOutsideMobile()

        //!  _hwMobileSlot = new HardwareMobileSlot(hwMap)
        TODO("Add hardware mobile slot linking")

        _gate.Set(MobileGate.CLOSED, MobileGate.Name.CLOSED)
        TODO("Add hardware gate calibration")
    }
}