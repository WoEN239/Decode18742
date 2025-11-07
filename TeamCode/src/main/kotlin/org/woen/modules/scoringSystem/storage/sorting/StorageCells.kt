package org.woen.modules.scoringSystem.storage.sorting


import kotlinx.coroutines.delay

import woen239.enumerators.Ball
import woen239.enumerators.BallRequest

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import woen239.enumerators.StorageSlot

import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.STORAGE_SLOT_COUNT

import org.woen.telemetry.Configs.STORAGE.PREFERRED_INTAKE_SLOT_ORDER
import org.woen.telemetry.Configs.STORAGE.PREFERRED_REQUEST_SLOT_ORDER

import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_ONE_BALL_PUSHING_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_MAX_SERVO_POSITION_CHANGE

import org.woen.modules.scoringSystem.storage.sorting.hardware.HwSortingManager
import org.woen.telemetry.ThreadedTelemetry


/*   IMPORTANT NOTE ON HOW THE STORAGE IS CONFIGURED:
 *
 *   //  The MobileSlot is a legacy name for 2 slots unit
 *   //  It behaves and is treated as two slots
 *       there CAN be balls in both position
 *   //  But the total ball count in all robot slots
 *       must be less or equal to MAX_BALL_COUNT (3)
 *
 *   //  Every slot can only move the balls in one direction (CW)
 *
 *                                          __--__
 *                                         /      \
 *                                         |      |    Storage RotateCW
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
    private val _mobileSlot = MobileSlot()

    private lateinit var _hwSortingM: HwSortingManager  //  DO NOT JOIN ASSIGNMENT



    fun handleIntake(): IntakeResult
    {
        val result = IntakeResult(
            IntakeResult.FAIL_STORAGE_IS_FULL,
            IntakeResult.Name.FAIL_STORAGE_IS_FULL
        )
        if (anyBallCount() >= MAX_BALL_COUNT) return result

        var curSlotId = 0
        while (curSlotId < STORAGE_SLOT_COUNT)
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

    fun handleRequest(request: BallRequest.Name): RequestResult
    {
        val requestBuffer = BallRequest(request)

        if (requestBuffer.IsNone())
            return RequestResult(
                RequestResult.FAIL_ILLEGAL_ARGUMENT,
                RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
            )


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
        if (anyBallCount() <= 0)
            return RequestResult(
                RequestResult.FAIL_IS_EMPTY,
                RequestResult.Name.FAIL_IS_EMPTY)


        val result = RequestResult(
            RequestResult.FAIL_COLOR_NOT_PRESENT,
            RequestResult.Name.FAIL_COLOR_NOT_PRESENT
        )

        var curSlotId = 0
        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].Name() == requested)
            {
                result.Set(curSlotId)
                curSlotId += STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
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

        if (anyBallCount() <= 0) return result


        var curSlotId = 0
        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].HasBall())
            {
                result.Set(curSlotId)
                curSlotId += STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }



    suspend fun updateAfterIntake(inputBall: Ball.Name): Boolean
    {
        val intakeCondition = _storageCells[StorageSlot.BOTTOM].IsEmpty()

        if (intakeCondition)
        {
            _storageCells[StorageSlot.BOTTOM].Set(inputBall)

            ThreadedTelemetry.LAZY_INSTANCE.log("AUTO ADJUSTING")
            if (autoPartial2RotateCW()) partial2RotateCW()
        }
        //!  else fixStorageDesync()

        return intakeCondition
    }
    fun updateAfterRequest(): Boolean
    {
        val requestCondition = _storageCells[StorageSlot.MOBILE_OUT].IsFilled()

        if (requestCondition)  _storageCells[StorageSlot.MOBILE_OUT].Empty()
        //!  else fixStorageDesync()
        return requestCondition
    }


    fun fixStorageDesync()
    {
        TODO("Add storage recalibrations using in robot color sensors")
    }



    suspend fun hwRotateBeltCW(time: Long)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HARDWARE IS MOVING")
        _hwSortingM.forceSafeResume()
        delay(time)
        _hwSortingM.forceSafePause()
    }
    suspend fun hwRotateMobileSlotsCW()
    {
        _mobileSlot.openGate()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        _mobileSlot.openPush()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)


        _mobileSlot.closeGate()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        _mobileSlot.closePush()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
    }
    suspend fun hwRotateFallSlotCW()
    {
        _mobileSlot.openFall()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE
                + DELAY_FOR_ONE_BALL_PUSHING_MS)

        _mobileSlot.closeFall()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
    }

    suspend fun fullRotateCW()
    {
        hwRotateBeltCW(DELAY_FOR_ONE_BALL_PUSHING_MS)

        if (_storageCells[StorageSlot.MOBILE_IN].IsFilled())
            hwRotateFallSlotCW()

        if (_storageCells[StorageSlot.MOBILE_OUT].IsFilled())
            hwRotateMobileSlotsCW()

        hwRotateBeltCW(DELAY_FOR_ONE_BALL_PUSHING_MS)



        val buffer = _storageCells[StorageSlot.MOBILE_IN]

        _storageCells[StorageSlot.MOBILE_IN]  = _storageCells[StorageSlot.MOBILE_OUT]
        _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.CENTER]
        _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]
        _storageCells[StorageSlot.BOTTOM] = buffer
    }
    suspend fun partial1RotateCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.CENTER].IsEmpty()

        if (rotationCondition)
        {
            ThreadedTelemetry.LAZY_INSTANCE.log("UPDATE - MOVING 1")
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]
            _storageCells[StorageSlot.BOTTOM] = Ball(Ball.NONE, Ball.Name.NONE)

            hwRotateBeltCW(DELAY_FOR_ONE_BALL_PUSHING_MS)
        }
        return rotationCondition
    }
    suspend fun partial2RotateCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.MOBILE_OUT].IsEmpty()

        if (rotationCondition)
        {
            ThreadedTelemetry.LAZY_INSTANCE.log("UPDATE - MOVING 2")
            _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.CENTER]
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]
            _storageCells[StorageSlot.BOTTOM] = Ball(Ball.NONE, Ball.Name.NONE)

            hwRotateBeltCW(DELAY_FOR_ONE_BALL_PUSHING_MS * 2)
        }
        return rotationCondition
    }
    suspend fun autoPartial2RotateCW()
        = if (partial2RotateCW()) true
        else partial1RotateCW()

    suspend fun partial3RotateCW(): Boolean
    {
        val rotationCondition = _storageCells[StorageSlot.MOBILE_IN].IsEmpty()

        if (rotationCondition)
        {
            _storageCells[StorageSlot.MOBILE_IN]  = _storageCells[StorageSlot.MOBILE_OUT]
            _storageCells[StorageSlot.MOBILE_OUT] = _storageCells[StorageSlot.CENTER]
            _storageCells[StorageSlot.CENTER] = _storageCells[StorageSlot.BOTTOM]
            _storageCells[StorageSlot.BOTTOM] = Ball(Ball.NONE, Ball.Name.NONE)

            _hwSortingM.forceSafePause()
            hwRotateMobileSlotsCW()
            hwRotateBeltCW(DELAY_FOR_ONE_BALL_PUSHING_MS * 2)
        }
        return rotationCondition
    }



    suspend fun forceSafeStartHwBelt() = _hwSortingM.forceSafeStart()

    suspend fun forceSafeStopHwBelt() = _hwSortingM.forceSafeStop()
    fun emergencyStopHwBelt() = _hwSortingM.forceStop()



    fun storageData() = _storageCells

    fun anyBallCount(): Int
    {
        var count = 0; var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE_IN)
        {
            if (_storageCells[curSlotId].HasBall()) count++
            curSlotId++
        }

        return count
    }

    fun selectedBallCount(ball: Ball.Name): Int
    {
        var count = 0; var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE_IN)
        {
            if (_storageCells[curSlotId].HasBall(ball)) count++
            curSlotId++
        }

        return count
    }
    fun ballColorCountPG(): IntArray
    {
        val countPG = intArrayOf(0, 0, 0); var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE_IN)
        {
            countPG[_storageCells[curSlotId].Id()]++
            curSlotId++
        }

        return intArrayOf(countPG[Ball.PURPLE], countPG[Ball.GREEN])
    }



    fun linkBeltHardware()
    {
        _hwSortingM = HwSortingManager()
        _hwSortingM.addDevice()
    }
    fun linkMobileSlotHardware() = _mobileSlot.initHardware()
}