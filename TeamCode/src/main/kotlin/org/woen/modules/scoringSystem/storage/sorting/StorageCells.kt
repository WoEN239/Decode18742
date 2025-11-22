package org.woen.modules.scoringSystem.storage.sorting


import woen239.enumerators.Ball
import woen239.enumerators.BallRequest

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import woen239.enumerators.StorageSlot

import org.woen.telemetry.ThreadedTelemetry

import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.STORAGE_SLOT_COUNT

import org.woen.telemetry.Configs.STORAGE.PREFERRED_INTAKE_SLOT_ORDER
import org.woen.telemetry.Configs.STORAGE.PREFERRED_REQUEST_SLOT_ORDER

import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_ONE_BALL_PUSHING_MS

import org.woen.modules.scoringSystem.storage.sorting.hardware.HwSortingManager



/*   IMPORTANT NOTE ON HOW THE STORAGE IS CONFIGURED:
 *
 *   //  The MobileSlot is a legacy name for 2 slots unit
 *   //  It behaves and is treated as two slots
 *       there CAN be balls in both position
 *   //  But the total ball count in all robot slots
 *       must be less or equal to MAX_BALL_COUNT (3)
 *
 *   //  Every slot can only move the balls in one direction (Forward - CW)
 *
 *                                          __--__
 *                                         /      \
 *                                         |      |    Storage RotateForward (CW)
 *                                                V
 *                    OUTPUT
 *   SWITCH  |          ^^          |   SORTING
 *    SERVO  |          ||          |   SERVO
 *     GATE -|-----==SERVO GATE==   \___GATE______________________
 *           |          ||             /                          \
 *   PUSHER ===/   [MOBILE_OUT slot]  / --->   [MOBILE_IN slot]   |
 *    SERVO  |          ^^^                           |||         |
 *           |          |||                      ==FALL SERVO==   |
 *           |          |||                           vvv         \---------
 *           |      [CENTER slot]       <---     [BOTTOM slot]  <---- INTAKE
 *           \____________________________________________________/---------
 *
 *
 */



class StorageCells
{
    private val _storageCells = Array(STORAGE_SLOT_COUNT) { Ball() }
    private val _hwSortingM = HwSortingManager()



    fun handleIntake(): IntakeResult
    {
        val result = IntakeResult(
            IntakeResult.FAIL_STORAGE_IS_FULL,
            IntakeResult.Name.FAIL_STORAGE_IS_FULL
        )
        if (anyBallCount() >= MAX_BALL_COUNT) return result

        var curSlotId = StorageSlot.BOTTOM
        while (curSlotId < MAX_BALL_COUNT)
        {
            if (_storageCells[PREFERRED_INTAKE_SLOT_ORDER[curSlotId]].IsEmpty())
            {
                result.Set(curSlotId)
                curSlotId += STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }

    suspend fun handleRequest(request: BallRequest.Name): RequestResult
    {
        val requestBuffer = BallRequest(request)

        if (requestBuffer.IsNone())
            return RequestResult(
                RequestResult.FAIL_ILLEGAL_ARGUMENT,
                RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
            )

        hwReAdjustStorage()

        if (requestBuffer.IsPreferred())
        {
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
        if (isEmpty())
            return RequestResult(
                RequestResult.FAIL_IS_EMPTY,
                RequestResult.Name.FAIL_IS_EMPTY)

        ThreadedTelemetry.LAZY_INSTANCE.log("Cur storage before request search")
        ThreadedTelemetry.LAZY_INSTANCE.log("request: $requested")
        ThreadedTelemetry.LAZY_INSTANCE.log("BOTTOM :${_storageCells[StorageSlot.BOTTOM].Name()}")
        ThreadedTelemetry.LAZY_INSTANCE.log("CENTER :${_storageCells[StorageSlot.CENTER].Name()}")
        ThreadedTelemetry.LAZY_INSTANCE.log("MB_OUT :${_storageCells[StorageSlot.MOBILE_OUT].Name()}")
        ThreadedTelemetry.LAZY_INSTANCE.log("MOB_IN :${_storageCells[StorageSlot.MOBILE_IN].Name()}")

        val result = RequestResult(
            RequestResult.FAIL_COLOR_NOT_PRESENT,
            RequestResult.Name.FAIL_COLOR_NOT_PRESENT
        )

        var curSlotId = StorageSlot.BOTTOM
        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].Name() == requested)
            {
                result.Set(PREFERRED_REQUEST_SLOT_ORDER[curSlotId])
                curSlotId += STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }
    private fun anyBallRequestSearch(): RequestResult
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("START: ANY REQUEST SEARCH")

        val result = RequestResult(
            RequestResult.FAIL_IS_EMPTY,
            RequestResult.Name.FAIL_IS_EMPTY
        )

        ThreadedTelemetry.LAZY_INSTANCE.log("Cur storage before request search")
        ThreadedTelemetry.LAZY_INSTANCE.log("request: ANY")
        ThreadedTelemetry.LAZY_INSTANCE.log("BOTTOM :${_storageCells[StorageSlot.BOTTOM].Name()}")
        ThreadedTelemetry.LAZY_INSTANCE.log("CENTER :${_storageCells[StorageSlot.CENTER].Name()}")
        ThreadedTelemetry.LAZY_INSTANCE.log("MB_OUT :${_storageCells[StorageSlot.MOBILE_OUT].Name()}")
        ThreadedTelemetry.LAZY_INSTANCE.log("MOB_IN :${_storageCells[StorageSlot.MOBILE_IN].Name()}")
        if (isEmpty()) return result


        var curSlotId = StorageSlot.BOTTOM
        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].HasBall())
            {
                result.Set(PREFERRED_REQUEST_SLOT_ORDER[curSlotId])
                curSlotId += STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }



    suspend fun updateAfterIntake(inputBall: Ball.Name): Boolean
    {
        _hwSortingM.stopAwaitingEating(true)
        val intakeCondition = _storageCells[StorageSlot.BOTTOM].IsEmpty()
        ThreadedTelemetry.LAZY_INSTANCE.log("Cur storage before intake updating")
        ThreadedTelemetry.LAZY_INSTANCE.log("BOTTOM :${_storageCells[StorageSlot.BOTTOM].Name()}")
        ThreadedTelemetry.LAZY_INSTANCE.log("CENTER :${_storageCells[StorageSlot.CENTER].Name()}")
        ThreadedTelemetry.LAZY_INSTANCE.log("MB_OUT :${_storageCells[StorageSlot.MOBILE_OUT].Name()}")
        ThreadedTelemetry.LAZY_INSTANCE.log("MOB_IN :${_storageCells[StorageSlot.MOBILE_IN].Name()}")

        if (intakeCondition)
        {
            _storageCells[StorageSlot.BOTTOM].Set(inputBall)

            ThreadedTelemetry.LAZY_INSTANCE.log("SW STORAGE UPDATED, auto adjusting..")
            hwReAdjustStorage()
        }
        else ThreadedTelemetry.LAZY_INSTANCE.log("INTAKE: UNKNOWN ERROR whilst cells intake")
        //!  else fastFixStorageDesync()

        _hwSortingM.resumeAwaitingEating()
        return intakeCondition
    }
    suspend fun updateAfterRequest(): Boolean
    {
        _hwSortingM.stopAwaitingEating(true)
        val requestCondition = _storageCells[StorageSlot.MOBILE_OUT].IsFilled()

        if (requestCondition)  _storageCells[StorageSlot.MOBILE_OUT].Empty()
        hwReAdjustStorage()

        return requestCondition
    }


    fun fastFixStorageDesync()
    {
        TODO("Add storage recalibrations using in robot optic-pares")
    }
    fun fullFixStorageDesync()
    {
        TODO("Add full storage recalibration using in robot color sensors")
    }



    suspend fun hwRotateBeltsForward(timeMs: Long) = _hwSortingM.hwRotateBeltForward(timeMs)
    suspend fun hwReverseBelts (timeMs: Long) = _hwSortingM.hwReverseBelt(timeMs)
    suspend fun hwForceResumeBelts() = _hwSortingM.forceSafeResumeBelts()
    suspend fun hwForcePauseBelts()  = _hwSortingM.forceSafePauseBelts()
    suspend fun hwOpenTurretGate()   = _hwSortingM.openTurretGate()



    suspend fun fullCalibrate() = _hwSortingM.fullCalibrate()
    suspend fun fullRotate()
    {
        _hwSortingM.stopAwaitingEating(true)
        ThreadedTelemetry.LAZY_INSTANCE.log("FR - hw readjusting")
        hwReAdjustStorage()


        ThreadedTelemetry.LAZY_INSTANCE.log("FR - MOVING MOBILE")
        _hwSortingM.hwRotateMobileSlots()

        _storageCells[StorageSlot.MOBILE_IN].Set(
            _storageCells[StorageSlot.MOBILE_OUT].Id(),
            _storageCells[StorageSlot.MOBILE_OUT].Name())
        _storageCells[StorageSlot.MOBILE_OUT].Empty()


        ThreadedTelemetry.LAZY_INSTANCE.log("FR - hw readjusting")
        hwReAdjustStorage()
    }
    private fun swReAdjustStorage(): Boolean
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("SW READJUST START - " + anyBallCount())
        if (_storageCells[StorageSlot.MOBILE_OUT].IsEmpty() &&
            isNotEmpty())
        {
            _storageCells[StorageSlot.MOBILE_OUT].Set(
                _storageCells[StorageSlot.CENTER].Id(),
                _storageCells[StorageSlot.CENTER].Name())

            _storageCells[StorageSlot.CENTER].Set(
                _storageCells[StorageSlot.BOTTOM].Id(),
                _storageCells[StorageSlot.BOTTOM].Name())

            _storageCells[StorageSlot.BOTTOM].Set(
                _storageCells[StorageSlot.MOBILE_IN].Id(),
                _storageCells[StorageSlot.MOBILE_IN].Name())

            _storageCells[StorageSlot.MOBILE_IN].Empty()
            return true
        }
        else if (_storageCells[StorageSlot.CENTER].IsEmpty()
            && anyBallCount() > 1)
        {
            _storageCells[StorageSlot.CENTER].Set(
                _storageCells[StorageSlot.BOTTOM].Id(),
                _storageCells[StorageSlot.BOTTOM].Name())

            _storageCells[StorageSlot.BOTTOM].Set(
                _storageCells[StorageSlot.MOBILE_IN].Id(),
                _storageCells[StorageSlot.MOBILE_IN].Name())

            _storageCells[StorageSlot.MOBILE_IN].Empty()

            return true
        }
        else if (_storageCells[StorageSlot.BOTTOM].IsEmpty()
              && _storageCells[StorageSlot.MOBILE_IN].IsFilled())
        {
            _storageCells[StorageSlot.BOTTOM].Set(
                _storageCells[StorageSlot.MOBILE_IN].Id(),
                _storageCells[StorageSlot.MOBILE_IN].Name())

            _storageCells[StorageSlot.MOBILE_IN].Empty()
            return true
        }
        else
        {
            ThreadedTelemetry.LAZY_INSTANCE.log("case 4: already adjusted, ball count: " + anyBallCount())
            return false
        }
    }
    suspend fun hwReAdjustStorage()
    {
        _hwSortingM.stopAwaitingEating(true)
        while (swReAdjustStorage())
            _hwSortingM.hwRotateBeltForward(DELAY_FOR_ONE_BALL_PUSHING_MS)
    }



    suspend fun pauseAnyIntake() = _hwSortingM.stopAwaitingEating(true)
    fun resumeIntakes() = _hwSortingM.resumeAwaitingEating()
    
    

    fun storageData() = _storageCells

    fun anyBallCount(): Int
    {
        var count = 0
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE_IN)
        {
            if (_storageCells[curSlotId].HasBall()) count++
            curSlotId++
        }

        return count
    }
    fun isEmpty(): Boolean
    {
        return _storageCells[StorageSlot.BOTTOM].IsEmpty()
            && _storageCells[StorageSlot.CENTER].IsEmpty()
            && _storageCells[StorageSlot.MOBILE_OUT].IsEmpty()
            && _storageCells[StorageSlot.MOBILE_IN].IsEmpty()
    }
    fun isNotEmpty(): Boolean
    {
        return _storageCells[StorageSlot.BOTTOM].IsFilled()
            || _storageCells[StorageSlot.CENTER].IsFilled()
            || _storageCells[StorageSlot.MOBILE_OUT].IsFilled()
            || _storageCells[StorageSlot.MOBILE_IN].IsFilled()
    }


    fun selectedBallCount(ball: Ball.Name): Int
    {
        var count = 0
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE_IN)
        {
            if (_storageCells[curSlotId].HasBall(ball)) count++
            curSlotId++
        }

        return count
    }
    fun ballColorCountPG(): IntArray
    {
        val countPG = intArrayOf(0, 0, 0)
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE_IN)
        {
            countPG[_storageCells[curSlotId].Id()]++
            curSlotId++
        }

        return intArrayOf(countPG[Ball.PURPLE], countPG[Ball.GREEN])
    }
}