package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.RequestResult
import org.woen.enumerators.StorageSlot

import org.woen.telemetry.LogManager

import org.woen.modules.scoringSystem.storage.Alias.Delay
import org.woen.modules.scoringSystem.storage.Alias.Intake
import org.woen.modules.scoringSystem.storage.Alias.Request
import org.woen.modules.scoringSystem.storage.Alias.NOTHING
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT
import org.woen.modules.scoringSystem.storage.Alias.STORAGE_SLOT_COUNT

import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.LOGIC_STEPS
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.GENERIC_INFO
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.PROCESS_ENDING
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.PROCESS_STARTING
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.CELLS_DEBUG_LEVELS
import org.woen.telemetry.configs.Configs.DEBUG_LEVELS.CELLS_DEBUG_SETTING

import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.TRUE_MATCH_WEIGHT
import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.PSEUDO_MATCH_WEIGHT
import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.START_WEIGHT_FOR_PREDICT_SORT

import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.ALWAYS_TRY_PREDICT_SORTING
import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.MIN_SEQUENCE_SCORE_FOR_PREDICT_SORTING

import org.woen.modules.scoringSystem.storage.sorting.hardware.HwSortingManager
import org.woen.modules.scoringSystem.storage.StorageHandleIdenticalColorsEvent
import org.woen.telemetry.configs.Configs.SORTING_SETTINGS.INITIAL_LOAD_FROM_TURRET_TO_BOTTOM



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
    private val _storageCells = INITIAL_LOAD_FROM_TURRET_TO_BOTTOM
    val hwSortingM = HwSortingManager()
    val logM = LogManager(CELLS_DEBUG_SETTING, CELLS_DEBUG_LEVELS, "CELLS")



    fun resetParametersToDefault()
    {
        if (notFullYet()) hwSortingM.resumeAwaitingEating()
    }

    @Synchronized
    fun fullEmptyStorageCells()
    {
        _storageCells[StorageSlot.BOTTOM].empty()
        _storageCells[StorageSlot.CENTER].empty()
        _storageCells[StorageSlot.TURRET].empty()
        _storageCells[StorageSlot.MOBILE].empty()
    }




    suspend fun handleRequest(requested: BallRequest.Name): RequestResult
    {
        if (BallRequest.isNone(requested))
            return Request.F_ILLEGAL_ARGUMENT

        hwReAdjustStorage()

        return if (isEmpty()) Request.F_IS_EMPTY
        else requestSearch(requested)
    }
    private fun requestSearch(requested: BallRequest.Name): RequestResult
    {
        logM.logMd("starting request search: ${requested}, storage:", PROCESS_STARTING)
        logAllStorageData()

        val result = Request.F_COLOR_NOT_PRESENT

        var curSlotId    = StorageSlot.BOTTOM
        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            val chosenSlotId = Request.SEARCH_ORDER[curSlotId]

            if (_storageCells[chosenSlotId].doesMatch(requested))
            {
                result.set(chosenSlotId)
                curSlotId += STORAGE_SLOT_COUNT
            }
            curSlotId++
        }

        return result
    }

    private fun predictSortSearch(
        requested: Array<BallRequest>,
        trimmedRequestSize: Int): PredictSortResult
    {
        var globalMaximum  = START_WEIGHT_FOR_PREDICT_SORT
        var doRotations    = NOTHING
        var startRequestId = NOTHING

        while (startRequestId < trimmedRequestSize)
        {
            var localMaximum  = START_WEIGHT_FOR_PREDICT_SORT
            var requestId     = startRequestId

            logM.logMd("search round: $startRequestId", GENERIC_INFO)

            while  (requestId   <  trimmedRequestSize + startRequestId)
            {
                val curRequest  = requested[requestId - startRequestId]
                val storageBall = _storageCells[Request.SEARCH_ORDER[requestId % MAX_BALL_COUNT]]
                //  Taking the module for the storage ball by 3 prevents counting empty mobile slot

                val canMatchRequest = storageBall.name() == curRequest.toBall()

                if (curRequest.isAbstractAny())
                {
                    if (canMatchRequest) localMaximum += TRUE_MATCH_WEIGHT
                    else if (storageBall.isFilled())
                        localMaximum += if (curRequest.isAny()) TRUE_MATCH_WEIGHT
                                                         else PSEUDO_MATCH_WEIGHT
                    else requestId += trimmedRequestSize
                }
                else if (canMatchRequest) localMaximum += TRUE_MATCH_WEIGHT
                else requestId += trimmedRequestSize

                logM.logMd("requestId: ${requestId % MAX_BALL_COUNT}, direct match: $canMatchRequest"
                      + "\n\t\t> request ball: ${curRequest.name()}, storage ball: ${storageBall.name()}",
                    GENERIC_INFO)

                requestId++
            }

            if (localMaximum > globalMaximum)
            {
                doRotations = startRequestId
                globalMaximum = localMaximum

                logM.logMd("Found new global maximum: $globalMaximum", GENERIC_INFO)

                if (globalMaximum == requested.size * TRUE_MATCH_WEIGHT)
                {
                    logM.logMd("Found optimal state, ending search in advance", LOGIC_STEPS)
                    return PredictSortResult(doRotations, globalMaximum)
                }
            }

            startRequestId++
        }

        logM.logMd("Done searching, max: $globalMaximum, rotations: $doRotations", PROCESS_ENDING)
        return PredictSortResult(doRotations, globalMaximum)
    }
    suspend fun initiatePredictSort(requested: Array<BallRequest.Name>,
                                    minValidInSequence: Double = 0.75): Boolean
    {
        val trimmedRequestSize = min(requested.size, MAX_BALL_COUNT)
        if (trimmedRequestSize == NOTHING) return false

        hwReAdjustStorage()

        logM.logMd("Start predict sort search", PROCESS_STARTING)
        val requestedFullData  = Array(requested.size) { BallRequest(requested[it]) }
        val searchResult = predictSortSearch(requestedFullData, trimmedRequestSize)

        logM.logMd("Best score: ${searchResult.maxSequenceScore}" +
                       ", required >= $minValidInSequence", GENERIC_INFO)

        if (searchResult.maxSequenceScore >= minValidInSequence)
            repeat (searchResult.totalRotations)
                { fullRotate() }

        return searchResult.maxSequenceScore >= trimmedRequestSize      * PSEUDO_MATCH_WEIGHT
            && searchResult.maxSequenceScore > (trimmedRequestSize - 1) *   TRUE_MATCH_WEIGHT
    }
    suspend fun tryInitiatePredictSort(requested: Array<BallRequest.Name>): Boolean
    {
        return if (ALWAYS_TRY_PREDICT_SORTING)
            initiatePredictSort(requested,
                MIN_SEQUENCE_SCORE_FOR_PREDICT_SORTING)
        else false
    }



    suspend fun safeFillWithUnknown()
    {
        hwReAdjustStorage()

        var curSlot = StorageSlot.BOTTOM
        while (_storageCells[curSlot].isEmpty())
        {
            _storageCells[curSlot].setUnknown()
            curSlot++
        }
    }
    @Synchronized
    fun updateAfterLazyIntake(inputFromTurretSlotToBottom: Array<Ball.Name>)
    {
        if (inputFromTurretSlotToBottom.size > MAX_BALL_COUNT) return

        var    curSlot  = StorageSlot.BOTTOM
        while (curSlot <= StorageSlot.TURRET)
        {
            _storageCells[Intake.INPUT_ORDER[curSlot]].set(
                 inputFromTurretSlotToBottom[curSlot])
            curSlot++
        }

        logAllStorageData()
    }
    suspend fun updateAfterIntake(inputBall: Ball.Name)
    {
        logM.logMd("before intake:", GENERIC_INFO)
        logAllStorageData()

        var curSlot = StorageSlot.BOTTOM
        var rotationTime = 2 * Delay.PART_PUSH
        while (curSlot < StorageSlot.MOBILE && _storageCells[curSlot].isEmpty())
        {
            curSlot++
            rotationTime += Delay.PART_PUSH
        }
        curSlot--

        if (curSlot >= StorageSlot.BOTTOM) _storageCells[curSlot].set(inputBall)

        logM.logMd("software storage processing finished", PROCESS_ENDING)
        logM.logMd("new storage: ", GENERIC_INFO)
        logAllStorageData()

        hwSortingM.reinstantiableForwardBeltsTime(
            rotationTime,
            curSlot == StorageSlot.TURRET)
    }
    @Synchronized
    fun updateAfterRequest()
    {
        hwSortingM.stopAwaitingEating(true)

        _storageCells[StorageSlot.TURRET].set(_storageCells[StorageSlot.CENTER])
        _storageCells[StorageSlot.CENTER].set(_storageCells[StorageSlot.BOTTOM])
        _storageCells[StorageSlot.BOTTOM].empty()

        logAllStorageData()
    }



//    fun recalibrateAfterStorageDesync()
//    {
//        TODO("Add full storage recalibration using in robot color sensors")
//    }



    suspend fun fullRotate()
    {
        logM.logMd("storage before full rotation:", GENERIC_INFO)
        logAllStorageData()

        hwReAdjustStorage()

        hwSortingM.rotateMobileSlot()

        _storageCells[StorageSlot.MOBILE].set(_storageCells[StorageSlot.TURRET])
        _storageCells[StorageSlot.TURRET].empty()

        hwReAdjustStorage()

        logM.logMd("finished full rotation, new storage:", PROCESS_ENDING)
        logAllStorageData()
    }


    @Synchronized
    private fun swReAdjustStorage(): Boolean
    {
        logM.logMd("SwReadjust start", PROCESS_STARTING)
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
            logM.logMd("finished readjusting", PROCESS_ENDING)
            return false
        }
    }
    suspend fun hwReAdjustStorage()
    {
        hwSortingM.stopAwaitingEating(true)

        while (swReAdjustStorage())
            hwSortingM.forwardBeltsTime(Delay.FULL_PUSH)
    }



    private fun logAllStorageData()
    {
        logM.logMd(""
              + "B:  ${_storageCells[StorageSlot.BOTTOM].name()}; "
              + "C:  ${_storageCells[StorageSlot.CENTER].name()}; "
              + "MO: ${_storageCells[StorageSlot.TURRET].name()}; "
              + "MI: ${_storageCells[StorageSlot.MOBILE].name()}\n",
            GENERIC_INFO)
    }
    fun storageData() = _storageCells

    @Synchronized
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

    @Synchronized
    fun alreadyFull() = anyBallCount() >= MAX_BALL_COUNT
    @Synchronized
    fun notFullYet()  = anyBallCount() < MAX_BALL_COUNT
    @Synchronized
    fun onlyOneBallLeft() = anyBallCount() == 1

    @Synchronized
    fun isEmpty(): Boolean
    {
        return _storageCells[StorageSlot.BOTTOM].isEmpty()
            && _storageCells[StorageSlot.CENTER].isEmpty()
            && _storageCells[StorageSlot.TURRET].isEmpty()
            && _storageCells[StorageSlot.MOBILE].isEmpty()
    }
    @Synchronized
    fun isNotEmpty(): Boolean
    {
        return _storageCells[StorageSlot.BOTTOM].isFilled()
            || _storageCells[StorageSlot.CENTER].isFilled()
            || _storageCells[StorageSlot.TURRET].isFilled()
            || _storageCells[StorageSlot.MOBILE].isFilled()
    }


    @Synchronized
    fun ballCountPGA(): CountPGA
    {
        val intPG     = intArrayOf(NOTHING, NOTHING, NOTHING)
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            intPG[_storageCells[curSlotId].id()]++
            curSlotId++
        }

        return toCountPGA(intPG)
    }
    fun toCountPGA(array: IntArray,
        includeAbstractAny: Boolean = true) : CountPGA
    {
        val abstractAny = if (includeAbstractAny)
            array[Ball.UNKNOWN_COLOR] else NOTHING

        return CountPGA(
            array[Ball.PURPLE],
            array[Ball.GREEN],
                    abstractAny)
    }


    fun handleIdenticalColorRequest(): StorageHandleIdenticalColorsEvent
    {
        val curStorage  = ballCountPGA()
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