package org.woen.modules.storage

import barrel.enumerators.Ball
import barrel.enumerators.BallRequest
import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult
import barrel.enumerators.StorageSlot

/*   IMPORTANT NOTE TO HOW THE STORAGE IS CONFIGURED:
 *
 *   //  The mobile slot is a combined unit
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
    private val _storageCells: Array<Ball?> = //!  TODO (fix this dumb kotlin thing)
        arrayOfNulls<Ball?>(4) //  0-Bottom, 1-center, 2-mobile_out, 3-mobile_in
    private val _mobileSlot: MobileSlot = MobileSlot()



    fun HandleIntake(): IntakeResult
    {
        val result = IntakeResult(IntakeResult.Name.FAIL_STORAGE_IS_FULL, IntakeResult.FAIL_STORAGE_IS_FULL)

        var i = StorageSlot.BOTTOM
        while (i < StorageSlot.MOBILE_IN)
        {
            if (_storageCells[i]!!.IsEmpty())
            {
                result.Set(i)
                i += 2 //  Fast break, preferring closest slot to intake
            }
            i++
        }
        if (result.SolutionIsMobileOut() && _mobileSlot.IsFilled())
            return IntakeResult(
                IntakeResult.Name.FAIL_STORAGE_IS_FULL,
                IntakeResult.FAIL_STORAGE_IS_FULL
            )

        return result
    }

    fun HandleRequest(request: BallRequest.Name): RequestResult
    {
        val requestBuffer = BallRequest.ToInt(request)
        if (requestBuffer == BallRequest.ANY)
        {   //  Optimised comparing by id without extra unnecessary conversions

            val requestResult = RequestSearch(Ball.Name.PURPLE)
            if (requestResult.Id() == RequestResult.FAIL_COLOR_NOT_PRESENT)
                return RequestSearch(
                    Ball.Name.GREEN
                )

            return requestResult
        }
        else return RequestSearch(Ball.ToName(requestBuffer))
    }
    private fun RequestSearch(requested: Ball.Name): RequestResult
    {
        val result = RequestResult(
            RequestResult.Name.FAIL_COLOR_NOT_PRESENT,
            RequestResult.FAIL_COLOR_NOT_PRESENT
        )

        var i = StorageSlot.BOTTOM
        while (i < StorageSlot.MOBILE)
        {
            if (_storageCells[i]!!.Name() == requested)
            {
                result.Set(i)
                if (i != StorageSlot.BOTTOM) i += 3 //  Fast break
                //  Preferring the closest slot to shooting
            }
            i++
        }

        return result
    }



    fun UpdateAfterIntake(inputBall: Ball.Name): Boolean
    {
        val intakeCondition = _storageCells[StorageSlot.BOTTOM]!!.IsEmpty()

        if (intakeCondition) _storageCells[StorageSlot.BOTTOM]!!.Set(inputBall)
        return intakeCondition
    }
    fun UpdateAfterRequest(): Boolean
    {
        val requestCondition = _storageCells[StorageSlot.MOBILE_OUT]!!.IsFilled()

        if (requestCondition) _storageCells[StorageSlot.MOBILE_OUT]!!.Empty()
        return requestCondition
    }



    fun FullRotateCW(): Boolean
    {
        val buffer = _storageCells[StorageSlot.MOBILE_IN]

        _storageCells[StorageSlot.MOBILE_IN] = _storageCells[StorageSlot.MOBILE_OUT]
        _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.CENTER]
        _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]
        _storageCells[StorageSlot.BOTTOM] = buffer

        //!  TODO(Rotate the hardware storage);
        return true //! TODO(Replace with hardware rotation succession)
    }
    fun Partial1RotateCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.CENTER]!!.IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }
    fun Partial2RotateCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.MOBILE_OUT]!!.IsEmpty()

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
        val rotationCondition = _storageCells[StorageSlot.MOBILE_IN]!!.IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.MOBILE_IN] = _storageCells[StorageSlot.MOBILE_OUT]
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
        val rotationCondition = _storageCells[StorageSlot.BOTTOM]!!.IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.BOTTOM] = _storageCells[StorageSlot.CENTER]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }
    fun Partial2RotateCCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.BOTTOM]!!.IsEmpty()

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
        val rotationCondition = _storageCells[StorageSlot.BOTTOM]!!.IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.BOTTOM] = _storageCells[StorageSlot.CENTER]
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.MOBILE_OUT]
            _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.MOBILE_IN]

            //!  TODO(Rotate the hardware storage);
        }
        return rotationCondition
    }



    fun StorageRaw(): Array<Ball?>
    {
        return _storageCells
    }
    fun StorageFiltered(): Array<Ball?>
    {
        return arrayOf<Ball?>(
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
            if (_storageCells[slotId]!!.IsFilled()) count++

        return count
    }
    fun SelectedBallCount(ball: Ball.Name): Int
    {
        var count = 0

        if (_mobileSlot.BallName() == ball) count++
        for (slotId in StorageSlot.BOTTOM..<StorageSlot.MOBILE_OUT)
            if (_storageCells[slotId]!!.Name() == ball) count++

        return count
    }
    fun BallColorCountPG(): IntArray?
    {
        val countPG = intArrayOf(0, 0, 0)

        countPG[_mobileSlot.BallId()]++
        for (i in StorageSlot.BOTTOM..<StorageSlot.MOBILE_OUT)
            countPG[_storageCells[i]!!.Id()]++

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
        return _mobileSlot.IsSlotIn();
    }





    init
    {
        _mobileSlot.SetSlot(StorageSlot.Name.MOBILE_IN)
        //!  TODO(Hardware calibration of mobile slot)
    }
}