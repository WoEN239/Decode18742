package org.woen.modules.storage.sorting


import barrel.enumerators.Ball
import barrel.enumerators.BallRequest

import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import barrel.enumerators.StorageSlot

import org.woen.telemetry.Configs.STORAGE.SLOTS_COUNT
import org.woen.telemetry.Configs.STORAGE.REAL_SLOT_COUNT
import org.woen.telemetry.Configs.STORAGE.PREFERRED_INTAKE_SLOT_ORDER
import org.woen.telemetry.Configs.STORAGE.PREFERRED_REQUEST_SLOT_ORDER


/*   IMPORTANT NOTE ON HOW THE STORAGE IS CONFIGURED:
 *
 *   //  The MobileSlot is a combined unit
 *   //  It behaves and is treated as one slot,
 *   //  => there CAN NOT be balls in both position
 *
 *   //  Every slot can only move the balls in one direction (CW)
 *
 *                     __--__
 *                    /      \
 *                    |      |    Storage RotateCW
 *                           V
 *    OUTPUT
 *   |  ^^  |
 *   |  ||  \_________________________________________
 *   |  ||                                            \
 *   |  [MOBILE_OUT slot]   ---->   [MOBILE_IN slot]  |
 *   |      ^^^                           |||         |
 *   |      |||                           |||         |
 *   |      |||                           vvv         \---------
 *   |  [CENTER slot]       <---     [BOTTOM slot]  <---- INTAKE
 *   \________________________________________________/---------
 *
 *
 */



class StorageCells
{
    private val _storageCells = Array(SLOTS_COUNT) { Ball() }
    private val _mobileSlot = MobileSlot()



    fun handleIntake(): IntakeResult
    {
        val result = IntakeResult(
            IntakeResult.FAIL_STORAGE_IS_FULL,
            IntakeResult.Name.FAIL_STORAGE_IS_FULL
        )

        var curSlotId = 0
        while (curSlotId < REAL_SLOT_COUNT)
        {
            if (_storageCells[PREFERRED_INTAKE_SLOT_ORDER[curSlotId]].IsEmpty())
            {
                result.Set(curSlotId)
                curSlotId += REAL_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }
        if (result.SolutionIsMobileOut() && _mobileSlot.isFilled())
            return IntakeResult(
                IntakeResult.FAIL_STORAGE_IS_FULL,
                IntakeResult.Name.FAIL_STORAGE_IS_FULL
            )

        return result
    }

    fun handleRequest(request: BallRequest.Name): RequestResult
    {
        val requestBuffer = BallRequest(request)

        if (requestBuffer.IsNone())
            return RequestResult(
                RequestResult.FAIL_ILLEGAL_ARGUMENT,
                RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
            )


        if (requestBuffer.IsPreferred())
        {   //  Optimised comparing by id without extra unnecessary conversions

            val requestResult = requestSearch(requestBuffer.ToBall())

            if (requestResult.DidFail())
                return requestSearch(requestBuffer.ToInverseBall())

            return requestResult
        }
        else if (requestBuffer.IsAny()) return anyBallRequestSearch()
        else return requestSearch(requestBuffer.ToBall())
    }
    private fun requestSearch(requested: Ball.Name): RequestResult
    {
        val result = RequestResult(
            RequestResult.FAIL_COLOR_NOT_PRESENT,
            RequestResult.Name.FAIL_COLOR_NOT_PRESENT
        )

        var curSlotId = 0
        while (curSlotId < SLOTS_COUNT)
        {
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].Name() == requested)
            {
                result.Set(curSlotId)
                curSlotId += SLOTS_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }
    private fun anyBallRequestSearch(): RequestResult
    {
        val result = RequestResult(
            RequestResult.FAIL_IS_EMPTY,
            RequestResult.Name.FAIL_IS_EMPTY
        )

        var curSlotId = 0
        while (curSlotId < SLOTS_COUNT)
        {
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].HasBall())
            {
                result.Set(curSlotId)
                curSlotId += SLOTS_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }



    fun updateAfterIntake(inputBall: Ball.Name): Boolean
    {
        val intakeCondition = _storageCells[StorageSlot.BOTTOM].IsEmpty()

        if (intakeCondition)  _storageCells[StorageSlot.BOTTOM].Set(inputBall)
        return intakeCondition
    }
    fun updateAfterRequest(): Boolean
    {
        val requestCondition = _storageCells[StorageSlot.MOBILE_OUT].IsFilled()

        if (requestCondition)  _storageCells[StorageSlot.MOBILE_OUT].Empty()
        return requestCondition
    }


    fun fixStorageDesync()
    {
        TODO("Add storage recalibrations using in robot color sensors")
    }



    fun fullRotateCW(): Boolean
    {
        val buffer = _storageCells[StorageSlot.MOBILE_IN]

        _storageCells[StorageSlot.MOBILE_IN]  = _storageCells[StorageSlot.MOBILE_OUT]
        _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.CENTER]
        _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]
        _storageCells[StorageSlot.BOTTOM] = buffer

        //!  TODO(Rotate the hardware storage);
        return true //! TODO(Replace with hardware rotation succession)
    }
    fun partial1RotateCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.CENTER].IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }
    fun partial2RotateCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.MOBILE_OUT].IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.CENTER]
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }
    fun partial3RotateCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.MOBILE_IN].IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.MOBILE_IN]  = _storageCells[StorageSlot.MOBILE_OUT]
            _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.CENTER]
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }
    fun partialAutoRotateCW(): Boolean
    {
        return if (partial2RotateCW()) true
        else partial3RotateCW()
    }



    fun storageRaw(): Array<Ball>
    {
        return _storageCells
    }
    fun storageFiltered(): Array<Ball>
    {
        return arrayOf(
            _storageCells[StorageSlot.BOTTOM],
            _storageCells[StorageSlot.CENTER],
            _mobileSlot.ball()
        )
    }

    fun anyBallCount(): Int
    {
        var count = 0

        if (_mobileSlot.isFilled()) count++
        for (slotId in StorageSlot.BOTTOM..<StorageSlot.MOBILE_OUT)
            if (_storageCells[slotId].HasBall()) count++

        return count
    }
    fun selectedBallCount(ball: Ball.Name): Int
    {
        var count = 0

        if (_mobileSlot.ballName() == ball) count++
        for (slotId in StorageSlot.BOTTOM..<StorageSlot.MOBILE_OUT)
            if (_storageCells[slotId].HasBall(ball)) count++

        return count
    }
    fun ballColorCountPG(): IntArray
    {
        val countPG = intArrayOf(0, 0, 0)

        countPG[_mobileSlot.ballId()]++
        for (i in StorageSlot.BOTTOM..<StorageSlot.MOBILE_OUT)
            countPG[_storageCells[i].Id()]++

        return intArrayOf(countPG[Ball.PURPLE], countPG[Ball.GREEN])
    }


    fun slotBall(): Ball
    {
        return _mobileSlot.ball()
    }
    fun slotBallName(): Ball.Name
    {
        return _mobileSlot.ballName()
    }
    fun slotBallId(): Int
    {
        return _mobileSlot.ballId()
    }


    fun mobileSlot(): MobileSlot
    {
        return _mobileSlot
    }
    fun ballSlotState(): StorageSlot
    {
        return _mobileSlot.ballSlotState()
    }
    fun ballSlotName(): StorageSlot.Name
    {
        return _mobileSlot.ballSlotStateName()
    }
    fun ballSlotStateId(): Int
    {
        return _mobileSlot.ballSlotStateId()
    }
    fun isBallInSlotIn(): Boolean
    {
        return _mobileSlot.isBallInSlotIn()
    }
    fun isBallInSlotOut(): Boolean
    {
        return _mobileSlot.isBallInSlotOut()
    }





    fun linkMobileSlotHardware()
    {
        _mobileSlot.initHardware()
    }
}