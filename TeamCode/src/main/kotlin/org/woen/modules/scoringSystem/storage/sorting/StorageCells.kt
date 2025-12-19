package org.woen.modules.scoringSystem.storage.sorting


import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult

import org.woen.enumerators.StorageSlot

import org.woen.telemetry.ThreadedTelemetry

import org.woen.telemetry.Configs.DELAY

import org.woen.telemetry.Configs.GENERIC.NOTHING
import org.woen.telemetry.Configs.GENERIC.MAX_BALL_COUNT
import org.woen.telemetry.Configs.GENERIC.STORAGE_SLOT_COUNT

import org.woen.telemetry.Configs.SORTING_SETTINGS.TRUE_MATCH_WEIGHT
import org.woen.telemetry.Configs.SORTING_SETTINGS.PSEUDO_MATCH_WEIGHT
import org.woen.telemetry.Configs.SORTING_SETTINGS.START_WEIGHT_FOR_PREDICT_SORT

import org.woen.telemetry.Configs.SORTING_SETTINGS.PREFERRED_INTAKE_SLOT_ORDER
import org.woen.telemetry.Configs.SORTING_SETTINGS.PREFERRED_REQUEST_SLOT_ORDER

import org.woen.telemetry.Configs.SORTING_SETTINGS.ALWAYS_TRY_PREDICT_SORTING
import org.woen.telemetry.Configs.SORTING_SETTINGS.MINIMAL_VALID_SEQUENCE_FOR_PREDICT_SORTING

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


class PredictSortResult(var totalRotations: Int, var maxSequenceScore: Double)



class StorageCells
{
    private val _storageCells = Array(STORAGE_SLOT_COUNT) { Ball() }
    val hwSortingM = HwSortingManager()



    fun resetParametersToDefault() = fullEmptyStorageCells()

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
        while (curSlotId < MAX_BALL_COUNT)
        {
            if (_storageCells[PREFERRED_INTAKE_SLOT_ORDER[curSlotId]].isEmpty())
            {
                result.set(PREFERRED_INTAKE_SLOT_ORDER[curSlotId])
                curSlotId += STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
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
        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].name() == requested)
            {
                result.set(PREFERRED_REQUEST_SLOT_ORDER[curSlotId])
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
            if (_storageCells[PREFERRED_REQUEST_SLOT_ORDER[curSlotId]].isFilled())
            {
                result.set(PREFERRED_REQUEST_SLOT_ORDER[curSlotId])
                curSlotId += STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }

    private fun predictSortSearch(requested: Array<BallRequest>): PredictSortResult
    {
        var globalMaximum  = START_WEIGHT_FOR_PREDICT_SORT
        var doRotations    = NOTHING
        var startRequestId = NOTHING

        while (startRequestId < MAX_BALL_COUNT)
        {
            var localMaximum = START_WEIGHT_FOR_PREDICT_SORT
            var requestId = startRequestId

            ThreadedTelemetry.LAZY_INSTANCE.log("search round: $startRequestId")

            while (requestId < MAX_BALL_COUNT + startRequestId)
            {
                val curRequest  = requested[(requestId - startRequestId) % MAX_BALL_COUNT]
                val storageBall = _storageCells[PREFERRED_REQUEST_SLOT_ORDER[requestId % MAX_BALL_COUNT]]

                val canMatchRequest = storageBall.name() == curRequest.toBall()

                if (curRequest.isAbstractAny())
                {
                    if (canMatchRequest) localMaximum += TRUE_MATCH_WEIGHT
                    else if (storageBall.isFilled())
                        localMaximum += if (curRequest.isAny()) TRUE_MATCH_WEIGHT
                                                         else PSEUDO_MATCH_WEIGHT
                    else requestId += MAX_BALL_COUNT
                }
                else if (canMatchRequest) localMaximum += TRUE_MATCH_WEIGHT
                else requestId += MAX_BALL_COUNT

                ThreadedTelemetry.LAZY_INSTANCE.log(
                    "> requestId: $requestId, direct match: $canMatchRequest\n"
                            + "request ball: ${curRequest.name()}, storage ball: ${storageBall.name()}")

                requestId++
            }

            if (localMaximum > globalMaximum)
            {
                doRotations = startRequestId
                globalMaximum = localMaximum

                ThreadedTelemetry.LAZY_INSTANCE.log("Found new global maximum: $globalMaximum")
            }

            startRequestId++
        }

        ThreadedTelemetry.LAZY_INSTANCE.log("CELLS: Done searching, max: $globalMaximum, rotations: $doRotations")
        return PredictSortResult(doRotations, globalMaximum)
    }
    suspend fun initiatePredictSort(requested: Array<BallRequest.Name>,
                                    minValidInSequence: Double = 0.75): Boolean
    {
        val requestedFullData = Array(requested.size) { BallRequest(requested[it]) }

        ThreadedTelemetry.LAZY_INSTANCE.log("CELLS: Start predict sort search")
        val searchResult = predictSortSearch(requestedFullData)

        ThreadedTelemetry.LAZY_INSTANCE.log("Best score: ${searchResult.maxSequenceScore}" +
                ", required min score: $MINIMAL_VALID_SEQUENCE_FOR_PREDICT_SORTING")

        if (searchResult.maxSequenceScore >= minValidInSequence)
            repeat (searchResult.totalRotations)
                { fullRotate() }

        return searchResult.maxSequenceScore >= MAX_BALL_COUNT * PSEUDO_MATCH_WEIGHT
                && searchResult.maxSequenceScore > (MAX_BALL_COUNT - 1) * TRUE_MATCH_WEIGHT
    }
    suspend fun tryInitiatePredictSort(requested: Array<BallRequest.Name>): Boolean
    {
        return if (ALWAYS_TRY_PREDICT_SORTING) initiatePredictSort(requested,
                MINIMAL_VALID_SEQUENCE_FOR_PREDICT_SORTING)
        else false
    }



    fun updateAfterLazyIntake(inputBalls: Array<Ball.Name>)
    {
        if (inputBalls.size > MAX_BALL_COUNT) return

        var curSlot = StorageSlot.BOTTOM
        while (curSlot <= StorageSlot.TURRET)
        {
            _storageCells[curSlot].set(inputBalls[curSlot])
            curSlot++
        }
    }
    suspend fun updateAfterIntake(inputBall: Ball.Name)
    {
        hwSortingM.stopAwaitingEating(true)

        ThreadedTelemetry.LAZY_INSTANCE.log("before intake:")
        logAllStorageData()

        _storageCells[StorageSlot.BOTTOM].set(inputBall)
        hwReAdjustStorage()

        ThreadedTelemetry.LAZY_INSTANCE.log("finished cells intake, new storage:")
        logAllStorageData()

        hwSortingM.resumeAwaitingEating()
    }
    fun updateAfterRequest()
    {
        hwSortingM.stopAwaitingEating(false)

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



    suspend fun fullRotate()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("storage before full rotation:")
        logAllStorageData()

        hwSortingM.stopAwaitingEating(true)
        hwReAdjustStorage()

        hwSortingM.hwRotateMobileSlot()

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
        hwSortingM.stopAwaitingEating(true)
        while (swReAdjustStorage())
            hwSortingM.hwForwardBeltsTime(DELAY.ONE_BALL_PUSHING_MS)
    }



    fun pauseAnyIntake() = hwSortingM.stopAwaitingEating(true)
    fun resumeIntakes()  = hwSortingM.resumeAwaitingEating()


    private fun logAllStorageData()
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
        var count = NOTHING
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE)
        {
            if (_storageCells[curSlotId].isFilled()) count++
            curSlotId++
        }

        return count
    }
    fun alreadyFull() = anyBallCount() >= MAX_BALL_COUNT
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
        var count = NOTHING
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
        val countPG = intArrayOf(NOTHING, NOTHING, NOTHING)
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
        val currentStorage = ballColorCountPG()
        val purpleCount = currentStorage[BallRequest.ShortScale.PURPLE]
        val greenCount  = currentStorage[BallRequest.ShortScale.GREEN]

        return if (greenCount > purpleCount)
             StorageHandleIdenticalColorsEvent(
                 greenCount,
                 Ball.Name.GREEN)
        else StorageHandleIdenticalColorsEvent(
                 purpleCount,
                 Ball.Name.PURPLE)
    }
}