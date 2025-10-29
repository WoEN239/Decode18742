package org.woen.modules.scoringSystem.storage.sorting


import woen239.enumerators.Ball

import woen239.enumerators.MobileGate
import woen239.enumerators.StorageSlot
import woen239.enumerators.MobileRotationResult

import org.woen.threading.hardware.HardwareThreads

import org.woen.modules.scoringSystem.storage.sorting.hardware.HwMobileSlot



class MobileSlot
{
    private val _ball = Ball()
    private val _ballSlot = StorageSlot()

    private val _gate = MobileGate()

    private lateinit var _hwMobileSlot: HwMobileSlot  //  DO NOT JOIN ASSIGNMENT



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
        _ballSlot.Set(result.ToEndStorageSlot())

        when (result.Id())
        {
            MobileRotationResult.SUCCESS_IN ->
            {
                TODO("Add hardware moving: OUTSIDE -> MOBILE_OUT")
            }

            MobileRotationResult.SUCCESS_IN_DOUBLE ->
            {
                TODO("Add hardware moving: OUTSIDE -> MOBILE_IN")
            }

            MobileRotationResult.SUCCESS ->
            {
                TODO("Add hardware moving: MOBILE_OUT -> MOBILE_IN")
            }

            MobileRotationResult.SUCCESS_OUT ->
            {
                _ball.Empty()
                TODO("Add hardware moving: MOBILE_IN -> OUTSIDE")
            }

            MobileRotationResult.SUCCESS_OUT_DOUBLE ->
            {
                _ball.Empty()
                TODO("Add hardware moving: MOBILE_OUT -> OUTSIDE")
            }

            else -> return MobileRotationResult()
        }
    }

    fun tryFillSlot(ball: Ball.Name): Boolean
    {
        val fillResult = tryRotateSlot(StorageSlot.Name.MOBILE_OUT)
        val fillSucceeded = fillResult.DidSucceed()

        if (fillSucceeded)
        {
            _ball.Set(ball)
            _ballSlot.Set(fillResult.ToEndStorageSlot())
        }
        return fillSucceeded
    }
    fun emptySlot(): Boolean
    {
        val emptyingSucceed = tryRotateSlot(StorageSlot.Name.OUTSIDE_MOBILE).DidSucceed()

        if (emptyingSucceed)
        {
            _ball.Empty()
            _ballSlot.SetOutsideMobile()
        }
        return emptyingSucceed
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
        return if (_ball.Id() == Ball.NONE) 0 else 1
    }





    fun linkHardware()
    {
        _hwMobileSlot = HwMobileSlot("", "")
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hwMobileSlot)
    }
    fun calibrateHardware()
    {
        _hwMobileSlot.calibrateGate()
        _hwMobileSlot.calibratePush()
    }

    fun initHardware()
    {

        linkHardware()
        calibrateHardware()
    }



    init
    {
        _ballSlot.SetOutsideMobile()
        _gate.Set(MobileGate.CLOSED, MobileGate.Name.CLOSED)
    }
}