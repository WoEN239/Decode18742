package org.woen.modules.storage


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
 *                     __--__
 *                    /      \
 *                    |      |    Storage RotateCW
 *                           V
 *    OUTPUT
 *   |  ^^  |
 *   |  ||  \_________________________________________
 *   |  ||                                            \
 *   |  [MOBILE_OUT slot]    <->   [MOBILE_IN slot]   |
 *   |      ^^^                           vvv         |
 *   |      |||                     one-directional   |
 *   |      vvv                           vvv         \---------
 *   |  [CENTER slot]       <->      [BOTTOM slot]  <---- INTAKE
 *   \________________________________________________/---------
 *
 *
 */



class StorageCells
{
    private val _storageCells: Array<Ball> = Array(SLOTS_COUNT) { Ball() }
    private val _mobileSlot: MobileSlot = MobileSlot()



    fun HandleIntake(): IntakeResult
    {
        val result = IntakeResult(IntakeResult.FAIL_STORAGE_IS_FULL, IntakeResult.Name.FAIL_STORAGE_IS_FULL)

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
        if (result.SolutionIsMobileOut() && _mobileSlot.IsFilled())
            return IntakeResult(
                IntakeResult.FAIL_STORAGE_IS_FULL,
                IntakeResult.Name.FAIL_STORAGE_IS_FULL
            )

        return result
    }

    fun HandleRequest(request: BallRequest.Name): RequestResult
    {
        val requestBuffer = BallRequest(request)

        if (requestBuffer.IsNone())
            return RequestResult(
                RequestResult.FAIL_ILLEGAL_ARGUMENT,
                RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
            )


        if (requestBuffer.IsPreferred())
        {   //  Optimised comparing by id without extra unnecessary conversions

            val requestResult = RequestSearch(requestBuffer.ToBall())

            if (requestResult.DidFail())
                return RequestSearch(requestBuffer.ToInverseBall())

            return requestResult
        }
        else if (requestBuffer.IsAny()) return AnyBallRequestSearch()
        else return RequestSearch(requestBuffer.ToBall())
    }

    private fun RequestSearch(requested: Ball.Name): RequestResult
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
    private fun AnyBallRequestSearch(): RequestResult
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



    fun UpdateAfterIntake(inputBall: Ball.Name): Boolean
    {
        val intakeCondition = _storageCells[StorageSlot.BOTTOM].IsEmpty()

        if (intakeCondition)  _storageCells[StorageSlot.BOTTOM].Set(inputBall)
        return intakeCondition
    }
    fun UpdateAfterRequest(): Boolean
    {
        val requestCondition = _storageCells[StorageSlot.MOBILE_OUT].IsFilled()

        if (requestCondition)  _storageCells[StorageSlot.MOBILE_OUT].Empty()
        return requestCondition
    }


    fun FixStorageDesync()
    {
        TODO("Add storage recalibrations using in robot color sensors")
    }



    fun FullRotateCW(): Boolean
    {
        val buffer = _storageCells[StorageSlot.MOBILE_IN]

        _storageCells[StorageSlot.MOBILE_IN]  = _storageCells[StorageSlot.MOBILE_OUT]
        _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.CENTER]
        _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]
        _storageCells[StorageSlot.BOTTOM] = buffer

        //!  TODO(Rotate the hardware storage);
        return true //! TODO(Replace with hardware rotation succession)
    }
    fun Partial1RotateCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.CENTER].IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }
    fun Partial2RotateCW(): Boolean
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
    fun Partial3RotateCW(): Boolean
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
    fun PartialAutoRotateCW(): Boolean
    {
        return if (Partial2RotateCW()) true
        else Partial3RotateCW()
    }


    fun Partial1RotateCCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.BOTTOM].IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.BOTTOM] = _storageCells[StorageSlot.CENTER]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }
    fun Partial2RotateCCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.BOTTOM].IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.BOTTOM] = _storageCells[StorageSlot.CENTER]
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.MOBILE_OUT]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }
    fun Partial3RotateCCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.BOTTOM].IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.BOTTOM] = _storageCells[StorageSlot.CENTER]
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.MOBILE_OUT]
            _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.MOBILE_IN]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }



    fun StorageRaw(): Array<Ball>
    {
        return _storageCells
    }
    fun StorageFiltered(): Array<Ball>
    {
        return arrayOf(
            _storageCells[StorageSlot.BOTTOM],
            _storageCells[StorageSlot.CENTER],
            _mobileSlot.Ball()
        )
    }

    fun AnyBallCount(): Int
    {
        var count = 0

        if (_mobileSlot.IsFilled()) count++
        for (slotId in StorageSlot.BOTTOM..<StorageSlot.MOBILE_OUT)
            if (_storageCells[slotId].HasBall()) count++

        return count
    }
    fun SelectedBallCount(ball: Ball.Name): Int
    {
        var count = 0

        if (_mobileSlot.BallName() == ball) count++
        for (slotId in StorageSlot.BOTTOM..<StorageSlot.MOBILE_OUT)
            if (_storageCells[slotId].HasBall(ball)) count++

        return count
    }
    fun BallColorCountPG(): IntArray
    {
        val countPG = intArrayOf(0, 0, 0)

        countPG[_mobileSlot.BallId()]++
        for (i in StorageSlot.BOTTOM..<StorageSlot.MOBILE_OUT)
            countPG[_storageCells[i].Id()]++

        return intArrayOf(countPG[Ball.PURPLE], countPG[Ball.GREEN])
    }


    fun MobileSlotBall(): Ball
    {
        return _mobileSlot.Ball()
    }
    fun MobileSlotBallName(): Ball.Name
    {
        return _mobileSlot.BallName()
    }
    fun MobileSlotBallId(): Int
    {
        return _mobileSlot.BallId()
    }


    fun MobileSlot(): MobileSlot
    {
        return _mobileSlot
    }
    fun MobileSlotState(): StorageSlot
    {
        return _mobileSlot.SlotState()
    }
    fun MobileSlotName(): StorageSlot.Name
    {
        return _mobileSlot.SlotStateName()
    }
    fun MobileSlotId(): Int
    {
        return _mobileSlot.SlotStateId()
    }
    fun IsMobileSlotIn(): Boolean
    {
        return _mobileSlot.IsSlotIn()
    }





    init
    {
        _mobileSlot.SetSlot(StorageSlot.Name.MOBILE_IN)
        //!  TODO(Hardware calibration of mobile slot)
    }
}