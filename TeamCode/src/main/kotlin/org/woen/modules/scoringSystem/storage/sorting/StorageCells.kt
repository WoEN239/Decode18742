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
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent



/*   IMPORTANT NOTE ON HOW THE STORAGE IS CONFIGURED:
 *
 *   //  Only possible to move the storage in one direction
 *   //  First ball in sequence becomes last once every rotation
 *   //  The balls are always aligned to the Turret slot
 *
 *
 *                                               OUTPUT
 *                          SORTING      |        ^^^         |
 *                          SERVO GATE   |        |||         |
 *       ____________________________/___/   =TURRET GATE=    |
 *      /                           /             |||         |
 *      |    [MOBILE slot]      <--/---      [TURRET slot]  \=== PUSHER SERVO
 *      |         |||                             ^^^         |
 *      |         |||                             |||         |
 *      |         vvv                             |||         |
 *      |    [BOTTOM slot]      ------>      [CENTER slot]    |
 *      \_____________________________________________________/
 *
 */



class StorageCells
{
    private val _storageCells = Array(STORAGE_SLOT_COUNT) { Ball() }
    private val _hwSortingM = HwSortingManager()



    fun resetParametersToDefault()
    {
        fullEmptyStorageCells()
        _hwSortingM.resetParametersAndLogicToDefault()
    }
    private fun fullEmptyStorageCells()
    {
        _storageCells[StorageSlot.BOTTOM].Empty()
        _storageCells[StorageSlot.CENTER].Empty()
        _storageCells[StorageSlot.TURRET].Empty()
        _storageCells[StorageSlot.MOBILE].Empty()
    }



    fun handleIntake(): IntakeResult
    {
        val result = IntakeResult(
            IntakeResult.FAIL_STORAGE_IS_FULL,
            IntakeResult.Name.FAIL_STORAGE_IS_FULL
        )
        if (alreadyFull()) return result

        var curSlotId = StorageSlot.BOTTOM
        while (curSlotId < MAX_BALL_COUNT)
        {
            if (_storageCells[PREFERRED_INTAKE_SLOT_ORDER[curSlotId]].IsEmpty())
            {
                result.Set(PREFERRED_INTAKE_SLOT_ORDER[curSlotId])
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

        ThreadedTelemetry.LAZY_INSTANCE.log("starting request search: ${requested}, storage:")
        logAllStorageData()

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
        val result = RequestResult(
            RequestResult.FAIL_IS_EMPTY,
            RequestResult.Name.FAIL_IS_EMPTY
        )

        ThreadedTelemetry.LAZY_INSTANCE.log("starting request search: ANY_CLOSEST, storage:")
        logAllStorageData()


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



    fun updateAfterLazyIntake(inputBalls: Array<Ball.Name>)
    {
        if (inputBalls.size > MAX_BALL_COUNT) return

        var curSlot = StorageSlot.BOTTOM
        while (curSlot <= StorageSlot.TURRET)
        {
            _storageCells[curSlot].Set(inputBalls[curSlot])
            curSlot++
        }
    }
    suspend fun updateAfterIntake(inputBall: Ball.Name)
    {
        _hwSortingM.stopAwaitingEating(true)

        ThreadedTelemetry.LAZY_INSTANCE.log("before intake:")
        logAllStorageData()

        _storageCells[StorageSlot.BOTTOM].Set(inputBall)
        hwReAdjustStorage()

        ThreadedTelemetry.LAZY_INSTANCE.log("finished cells intake, new storage:")
        logAllStorageData()

        _hwSortingM.resumeAwaitingEating()
    }
    fun updateAfterRequest()
    {
        _hwSortingM.stopAwaitingEating(true)

        _storageCells[StorageSlot.TURRET].Set(_storageCells[StorageSlot.CENTER])
        _storageCells[StorageSlot.CENTER].Set(_storageCells[StorageSlot.BOTTOM])
        _storageCells[StorageSlot.BOTTOM].Empty()

        logAllStorageData()
    }



    fun fastFixStorageDesync()
    {
        TODO("Add storage recalibrations using in robot optic-pares")
    }
    fun fullFixStorageDesync()
    {
        TODO("Add full storage recalibration using in robot color sensors")
    }



    suspend fun hwForwardBeltsTime(timeMs: Long) = _hwSortingM.hwForwardBeltsTime(timeMs)
    suspend fun hwReverseBeltsTime(timeMs: Long) = _hwSortingM.hwReverseBeltsTime(timeMs)
    fun hwStartBelts() = _hwSortingM.startBelts()
    fun hwStopBelts() = _hwSortingM.stopBelts()
    suspend fun hwOpenTurretGate() = _hwSortingM.openTurretGate()
    suspend fun hwCloseTurretGate() = _hwSortingM.closeTurretGate()


    suspend fun fullCalibrate() = _hwSortingM.fullCalibrate()
    suspend fun fullRotate()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("storage before full rotation:")
        logAllStorageData()

        _hwSortingM.stopAwaitingEating(true)
        hwReAdjustStorage()

        _hwSortingM.hwRotateMobileSlot()

        _storageCells[StorageSlot.MOBILE].Set(_storageCells[StorageSlot.TURRET])
        _storageCells[StorageSlot.TURRET].Empty()

        hwReAdjustStorage()

        ThreadedTelemetry.LAZY_INSTANCE.log("finished full rotation, new storage:")
        logAllStorageData()
    }


    private fun swReAdjustStorage(): Boolean
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("SwReadjust start")
        if (_storageCells[StorageSlot.TURRET].IsEmpty()
            && isNotEmpty())
        {
            _storageCells[StorageSlot.TURRET].Set(_storageCells[StorageSlot.CENTER])
            _storageCells[StorageSlot.CENTER].Set(_storageCells[StorageSlot.BOTTOM])
            _storageCells[StorageSlot.BOTTOM].Set(_storageCells[StorageSlot.MOBILE])
            _storageCells[StorageSlot.MOBILE].Empty()

            return true
        }
        else if (_storageCells[StorageSlot.CENTER].IsEmpty()
            && anyBallCount() > 1)
        {
            _storageCells[StorageSlot.CENTER].Set(_storageCells[StorageSlot.BOTTOM])
            _storageCells[StorageSlot.BOTTOM].Set(_storageCells[StorageSlot.MOBILE])
            _storageCells[StorageSlot.MOBILE].Empty()

            return true
        }
        else if (_storageCells[StorageSlot.BOTTOM].IsEmpty()
              && _storageCells[StorageSlot.MOBILE].IsFilled())
        {
            _storageCells[StorageSlot.BOTTOM].Set(_storageCells[StorageSlot.MOBILE])
            _storageCells[StorageSlot.MOBILE].Empty()

            return true
        }
        else
        {
            ThreadedTelemetry.LAZY_INSTANCE.log("finished readjusting")
            return false
        }
    }
    suspend fun hwReAdjustStorage()
    {
        _hwSortingM.stopAwaitingEating(true)
        while (swReAdjustStorage())
            _hwSortingM.hwForwardBeltsTime(DELAY_FOR_ONE_BALL_PUSHING_MS)
    }



    fun pauseAnyIntake() = _hwSortingM.stopAwaitingEating(true)
    fun resumeIntakes() = _hwSortingM.resumeAwaitingEating()


    fun logAllStorageData()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("" +
                "B:  ${_storageCells[StorageSlot.BOTTOM].Name()}; "
              + "C:  ${_storageCells[StorageSlot.CENTER].Name()}; "
              + "MO: ${_storageCells[StorageSlot.TURRET].Name()}; "
              + "MI: ${_storageCells[StorageSlot.MOBILE].Name()}\n"
        )
    }
    fun storageData() = _storageCells

    fun anyBallCount(): Int
    {
        var count = 0
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE)
        {
            if (_storageCells[curSlotId].HasBall()) count++
            curSlotId++
        }

        return count
    }
    fun alreadyFull() = anyBallCount() >= MAX_BALL_COUNT;
    fun isEmpty(): Boolean
    {
        return _storageCells[StorageSlot.BOTTOM].IsEmpty()
            && _storageCells[StorageSlot.CENTER].IsEmpty()
            && _storageCells[StorageSlot.TURRET].IsEmpty()
            && _storageCells[StorageSlot.MOBILE].IsEmpty()
    }
    fun isNotEmpty(): Boolean
    {
        return _storageCells[StorageSlot.BOTTOM].IsFilled()
            || _storageCells[StorageSlot.CENTER].IsFilled()
            || _storageCells[StorageSlot.TURRET].IsFilled()
            || _storageCells[StorageSlot.MOBILE].IsFilled()
    }


    fun selectedBallCount(ball: Ball.Name): Int
    {
        var count = 0
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE)
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

        while (curSlotId < StorageSlot.MOBILE)
        {
            countPG[_storageCells[curSlotId].Id()]++
            curSlotId++
        }

        return intArrayOf(countPG[Ball.PURPLE], countPG[Ball.GREEN])
    }


    fun handleIdenticalColorRequest(): StorageHandleIdenticalColorsEvent
    {
        //  -1 used because were are not using the "empty" count
        val currentStorage = ballColorCountPG()
        val purpleCount = currentStorage[Ball.PURPLE - 1]
        val greenCount  = currentStorage[Ball.GREEN  - 1]

        return if (greenCount > purpleCount)
             StorageHandleIdenticalColorsEvent(
                 greenCount,
                 Ball.Name.GREEN)
        else StorageHandleIdenticalColorsEvent(
                 purpleCount,
                 Ball.Name.PURPLE)
    }
}