package org.woen.modules.scoringSystem.storage.sorting


import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult

import org.woen.enumerators.StorageSlot

import org.woen.modules.scoringSystem.storage.Alias.Delay
import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.ShortScale
import org.woen.modules.scoringSystem.storage.Alias.NOTHING
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT
import org.woen.modules.scoringSystem.storage.Alias.STORAGE_SLOT_COUNT

import org.woen.modules.scoringSystem.storage.Alias.TelemetryLI

import org.woen.telemetry.Configs.SORTING_SETTINGS.TRUE_MATCH_WEIGHT
import org.woen.telemetry.Configs.SORTING_SETTINGS.PSEUDO_MATCH_WEIGHT
import org.woen.telemetry.Configs.SORTING_SETTINGS.START_WEIGHT_FOR_PREDICT_SORT

import org.woen.telemetry.Configs.SORTING_SETTINGS.ALWAYS_TRY_PREDICT_SORTING
import org.woen.telemetry.Configs.SORTING_SETTINGS.MIN_SEQUENCE_SCORE_FOR_PREDICT_SORTING

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



class CountPGA(var purple: Int, var green: Int, var any: Int = NOTHING)
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
        val result = Intake.F_IS_FULL
        if (alreadyFull()) return result

        var curSlotId = StorageSlot.BOTTOM
        while (curSlotId < MAX_BALL_COUNT)
        {
            if (_storageCells[Intake.SEARCH_ORDER[curSlotId]].isEmpty())
            {
                result.set(Intake.SEARCH_ORDER[curSlotId])
                curSlotId += STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }

    suspend fun handleRequest(name: BallRequest.Name): RequestResult
    {
        val request = BallRequest(name)
        if (request.isNone()) return Request.F_ILLEGAL_ARGUMENT

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
        TelemetryLI.log("starting request search: ${requested}, storage:")
        logAllStorageData()

        if (isEmpty()) return Request.F_IS_EMPTY
        val result = Request.F_COLOR_NOT_PRESENT

        var curSlotId = StorageSlot.BOTTOM
        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            if (_storageCells[Request.SEARCH_ORDER[curSlotId]].name() == requested)
            {
                result.set(Request.SEARCH_ORDER[curSlotId])
                curSlotId += STORAGE_SLOT_COUNT  //  Fast break, preferring chosen slot order
            }
            curSlotId++
        }

        return result
    }
    private fun anyBallRequestSearch(): RequestResult
    {
        TelemetryLI.log("starting request search: ANY_CLOSEST, storage:")
        logAllStorageData()

        val result = Request.F_IS_EMPTY
        if (isEmpty()) return result

        var curSlotId = StorageSlot.BOTTOM
        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            if (_storageCells[Request.SEARCH_ORDER[curSlotId]].isFilled())
            {
                result.set(Request.SEARCH_ORDER[curSlotId])
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

            TelemetryLI.log("search round: $startRequestId")

            while (requestId < MAX_BALL_COUNT + startRequestId)
            {
                val curRequest  = requested[(requestId - startRequestId) % MAX_BALL_COUNT]
                val storageBall = _storageCells[Request.SEARCH_ORDER[requestId % MAX_BALL_COUNT]]

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

                TelemetryLI.log(
                    "> requestId: $requestId, direct match: $canMatchRequest\n"
                            + "request ball: ${curRequest.name()}, storage ball: ${storageBall.name()}")

                requestId++
            }

            if (localMaximum > globalMaximum)
            {
                doRotations = startRequestId
                globalMaximum = localMaximum

                TelemetryLI.log("Found new global maximum: $globalMaximum")
            }

            startRequestId++
        }

        TelemetryLI.log("CELLS: Done searching, max: $globalMaximum, rotations: $doRotations")
        return PredictSortResult(doRotations, globalMaximum)
    }
    suspend fun initiatePredictSort(requested: Array<BallRequest.Name>,
                                    minValidInSequence: Double = 0.75): Boolean
    {
        val requestedFullData = Array(requested.size) { BallRequest(requested[it]) }

        TelemetryLI.log("CELLS: Start predict sort search")
        val searchResult = predictSortSearch(requestedFullData)

        TelemetryLI.log("Best score: ${searchResult.maxSequenceScore}" +
                ", required min score: $MIN_SEQUENCE_SCORE_FOR_PREDICT_SORTING")

        if (searchResult.maxSequenceScore >= minValidInSequence)
            repeat (searchResult.totalRotations)
                { fullRotate() }

        return searchResult.maxSequenceScore >= MAX_BALL_COUNT * PSEUDO_MATCH_WEIGHT
                && searchResult.maxSequenceScore > (MAX_BALL_COUNT - 1) * TRUE_MATCH_WEIGHT
    }
    suspend fun tryInitiatePredictSort(requested: Array<BallRequest.Name>): Boolean
    {
        return if (ALWAYS_TRY_PREDICT_SORTING)
            initiatePredictSort(requested,
                MIN_SEQUENCE_SCORE_FOR_PREDICT_SORTING)
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

        TelemetryLI.log("before intake:")
        logAllStorageData()

        _storageCells[StorageSlot.BOTTOM].set(inputBall)
        hwReAdjustStorage()

        TelemetryLI.log("finished cells intake, new storage:")
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
        TelemetryLI.log("storage before full rotation:")
        logAllStorageData()

        hwSortingM.stopAwaitingEating(true)
        hwReAdjustStorage()

        hwSortingM.hwRotateMobileSlot()

        _storageCells[StorageSlot.MOBILE].set(_storageCells[StorageSlot.TURRET])
        _storageCells[StorageSlot.TURRET].empty()

        hwReAdjustStorage()

        TelemetryLI.log("finished full rotation, new storage:")
        logAllStorageData()
    }


    private fun swReAdjustStorage(): Boolean
    {
        TelemetryLI.log("SwReadjust start")
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
            TelemetryLI.log("finished readjusting")
            return false
        }
    }
    suspend fun hwReAdjustStorage()
    {
        hwSortingM.stopAwaitingEating(true)

        while (swReAdjustStorage())
            hwSortingM.hwForwardBeltsTime(Delay.FULL_PUSH)
    }



    fun pauseAnyIntake() = hwSortingM.stopAwaitingEating(true)
    fun resumeIntakes()  = hwSortingM.resumeAwaitingEating()


    private fun logAllStorageData()
    {
        TelemetryLI.log("" +
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

        while (curSlotId < STORAGE_SLOT_COUNT)
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
    fun ballCountPG(): CountPGA
    {
        val intPG     = intArrayOf(NOTHING, NOTHING, NOTHING)
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < StorageSlot.MOBILE)
        {
            intPG[_storageCells[curSlotId].id()]++
            curSlotId++
        }

        return toCountPGA(intPG, false)
    }
    fun toCountPGA(array: IntArray,
        includeAbstractAny: Boolean = true) : CountPGA
    {
        val abstractAny = if (includeAbstractAny)
            array[ShortScale.ABSTRACT_ANY] else NOTHING

        return CountPGA(
            array[ShortScale.PURPLE_ID],
            array[ShortScale.GREEN_ID],
                    abstractAny)
    }


    fun handleIdenticalColorRequest(): StorageHandleIdenticalColorsEvent
    {
        val curStorage  = ballCountPG()
        val purpleCount = curStorage.purple
        val greenCount  = curStorage.green

        return if (greenCount > purpleCount)
             StorageHandleIdenticalColorsEvent(
                 greenCount,
                 Ball.Name.GREEN)

        else StorageHandleIdenticalColorsEvent(
                 purpleCount,
                 Ball.Name.PURPLE)
    }
}