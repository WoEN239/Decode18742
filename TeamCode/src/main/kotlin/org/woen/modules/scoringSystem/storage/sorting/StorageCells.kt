package org.woen.modules.scoringSystem.storage.sorting


import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult

import org.woen.enumerators.StorageSlot

import org.woen.telemetry.ThreadedTelemetry

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.GENERIC
import org.woen.telemetry.Configs.STORAGE.PREFERRED_INTAKE_SLOT_ORDER
import org.woen.telemetry.Configs.STORAGE.PREFERRED_REQUEST_SLOT_ORDER

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
    private val _storageCells = Array(GENERIC.STORAGE_SLOT_COUNT) { Ball() }
    private val _hwSortingM = HwSortingManager()



    fun resetParametersToDefault()
    {
        fullEmptyStorageCells()
        _hwSortingM.resetParametersAndLogicToDefault()
    }
    private fun fullEmptyStorageCells()
    {
        _storageCells[StorageSlot.BOTTOM].empty()
        _storageCells[StorageSlot.CENTER].empty()
        _storageCells[StorageSlot.TURRET].empty()
        _storageCells[StorageSlot.MOBILE].empty()
    }



    fun handleIntake(): IntakeResult
    {
        val result = IntakeResult(
            IntakeResult.FAIL_STORAGE_IS_FULL,
            IntakeResult.Name.FAIL_STORAGE_IS_FULL
        )
        if (alreadyFull()) return result

        var curSlotId = StorageSlot.BOTTOM
        while (curSlotId < GENERIC.MAX_BALL_COUNT)
        {
            if (_storageCells[PREFERRED_INTAKE_SLOT_ORDER[curSlotId]].isEmpty())
            {
                result.set(PREFERRED_INTAKE_SLOT_ORDER[curSlotId])
                curSlotId += GENERIC.STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }

    suspend fun handleRequest(requestName: BallRequest.Name): RequestResult
    {
        val request = BallRequest(requestName)

        if (request.isNone())
            return RequestResult(
                RequestResult.FAIL_ILLEGAL_ARGUMENT,
                RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
            )

        hwReAdjustStorage()

        if (request.isPreferred())
        {
            val requestResult = requestSearch(request.toBall())

            if (requestResult.didFail())
                return requestSearch(request.toInverseBall())

            return requestResult
        }
        else if (request.isAny()) return anyBallRequestSearch()
        else return requestSearch(request.toBall())
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
        while (curSlotId < GENERIC.STORAGE_SLOT_COUNT)
        {
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].name() == requested)
            {
                result.set(PREFERRED_REQUEST_SLOT_ORDER[curSlotId])
                curSlotId += GENERIC.STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
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
        while (curSlotId < GENERIC.STORAGE_SLOT_COUNT)
        {
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].isFilled())
            {
                result.set(PREFERRED_REQUEST_SLOT_ORDER[curSlotId])
                curSlotId += GENERIC.STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }



    fun updateAfterLazyIntake(inputBalls: Array<Ball.Name>)
    {
        if (inputBalls.size > GENERIC.MAX_BALL_COUNT) return

        var curSlot = StorageSlot.BOTTOM
        while (curSlot <= StorageSlot.TURRET)
        {
            _storageCells[curSlot].set(inputBalls[curSlot])
            curSlot++
        }
    }
    suspend fun updateAfterIntake(inputBall: Ball.Name)
    {
        _hwSortingM.stopAwaitingEating(true)

        ThreadedTelemetry.LAZY_INSTANCE.log("before intake:")
        logAllStorageData()

        _storageCells[StorageSlot.BOTTOM].set(inputBall)
        hwReAdjustStorage()

        ThreadedTelemetry.LAZY_INSTANCE.log("finished cells intake, new storage:")
        logAllStorageData()

        _hwSortingM.resumeAwaitingEating()
    }
    fun updateAfterRequest()
    {
        _hwSortingM.stopAwaitingEating(true)

        _storageCells[StorageSlot.TURRET].set(_storageCells[StorageSlot.CENTER])
        _storageCells[StorageSlot.CENTER].set(_storageCells[StorageSlot.BOTTOM])
        _storageCells[StorageSlot.BOTTOM].empty()

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
    fun hwSlowStartBelt() = _hwSortingM.slowStartBelts()
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

        _storageCells[StorageSlot.MOBILE].set(_storageCells[StorageSlot.TURRET])
        _storageCells[StorageSlot.TURRET].empty()

        hwReAdjustStorage()

        ThreadedTelemetry.LAZY_INSTANCE.log("finished full rotation, new storage:")
        logAllStorageData()
    }


    private fun swReAdjustStorage(): Boolean
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("SwReadjust start")
        if (_storageCells[StorageSlot.TURRET].isEmpty()
            && isNotEmpty())
        {
            _storageCells[StorageSlot.TURRET].set(_storageCells[StorageSlot.CENTER])
            _storageCells[StorageSlot.CENTER].set(_storageCells[StorageSlot.BOTTOM])
            _storageCells[StorageSlot.BOTTOM].set(_storageCells[StorageSlot.MOBILE])
            _storageCells[StorageSlot.MOBILE].empty()

            return true
        }
        else if (_storageCells[StorageSlot.CENTER].isEmpty()
            && anyBallCount() > 1)
        {
            _storageCells[StorageSlot.CENTER].set(_storageCells[StorageSlot.BOTTOM])
            _storageCells[StorageSlot.BOTTOM].set(_storageCells[StorageSlot.MOBILE])
            _storageCells[StorageSlot.MOBILE].empty()

            return true
        }
        else if (_storageCells[StorageSlot.BOTTOM].isEmpty()
              && _storageCells[StorageSlot.MOBILE].isFilled())
        {
            _storageCells[StorageSlot.BOTTOM].set(_storageCells[StorageSlot.MOBILE])
            _storageCells[StorageSlot.MOBILE].empty()

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
            _hwSortingM.hwForwardBeltsTime(DELAY.ONE_BALL_PUSHING_MS)
    }



    fun pauseAnyIntake() = _hwSortingM.stopAwaitingEating(true)
    fun resumeIntakes() = _hwSortingM.resumeAwaitingEating()


    fun logAllStorageData()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("" +
                "B:  ${_storageCells[StorageSlot.BOTTOM].name()}; "
              + "C:  ${_storageCells[StorageSlot.CENTER].name()}; "
              + "MO: ${_storageCells[StorageSlot.TURRET].name()}; "
              + "MI: ${_storageCells[StorageSlot.MOBILE].name()}\n"
        )
    }
    fun storageData() = _storageCells

    fun anyBallCount(): Int
    {
        var count = 0
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE)
        {
            if (_storageCells[curSlotId].isFilled()) count++
            curSlotId++
        }

        return count
    }
    fun alreadyFull() = anyBallCount() >= GENERIC.MAX_BALL_COUNT
    fun isEmpty(): Boolean
    {
        return _storageCells[StorageSlot.BOTTOM].isEmpty()
            && _storageCells[StorageSlot.CENTER].isEmpty()
            && _storageCells[StorageSlot.TURRET].isEmpty()
            && _storageCells[StorageSlot.MOBILE].isEmpty()
    }
    fun isNotEmpty(): Boolean
    {
        return _storageCells[StorageSlot.BOTTOM].isFilled()
            || _storageCells[StorageSlot.CENTER].isFilled()
            || _storageCells[StorageSlot.TURRET].isFilled()
            || _storageCells[StorageSlot.MOBILE].isFilled()
    }


    fun selectedBallCount(ball: Ball.Name): Int
    {
        var count = 0
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE)
        {
            if (_storageCells[curSlotId].hasBall(ball)) count++
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
            countPG[_storageCells[curSlotId].id()]++
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