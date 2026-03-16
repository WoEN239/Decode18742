package org.woen.scoringSystem.storage


import kotlin.math.min

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.StorageSlot

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.scoringSystem.storage.hardware.HwSortingManager

import org.woen.configs.Alias.Intake
import org.woen.configs.Alias.Request
import org.woen.configs.Alias.MAX_BALL_COUNT
import org.woen.configs.Alias.STORAGE_SLOT_COUNT
import org.woen.configs.DebugSettings

import org.woen.configs.Delay
import org.woen.configs.RobotSettings.ROBOT
import org.woen.configs.RobotSettings.SORTING
import org.woen.configs.RobotSettings.SORTING.PREDICT.TRUE_MATCH_WEIGHT
import org.woen.configs.RobotSettings.SORTING.PREDICT.PSEUDO_MATCH_WEIGHT
import org.woen.scoringSystem.ConnectorModuleStatus
import kotlin.math.floor


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



class CountPGA(var purple: Int, var green: Int, var any: Int = 0)
class PredictSortResult(
    var totalRotations: Int,
    var maxSequenceScore: Double,
    var totalMatches: Int)



class Cells
{
    private val _storageCells = ROBOT.INITIAL_LOAD_FROM_TURRET_TO_BOTTOM
    private val _cms: ConnectorModuleStatus
    val hwSortingM: HwSortingManager

    val logM: LogManager



    constructor(cms: ConnectorModuleStatus)
    {
        _cms = cms

        hwSortingM = HwSortingManager(_cms)

        logM = LogManager(_cms.collector, DebugSettings.CELLS)
    }



    fun relink()
    {
        if (notFullYet()) _cms.canTriggerIntake = true

        logM.relink(DebugSettings.CELLS)
    }

    fun fullEmptyStorageCells()
    {
        _storageCells[StorageSlot.BOTTOM].empty()
        _storageCells[StorageSlot.CENTER].empty()
        _storageCells[StorageSlot.TURRET].empty()
        _storageCells[StorageSlot.MOBILE].empty()
    }

    private fun predictSortSearchLogic(
        requested: Array<BallRequest>,
        trimmedRequestSize: Int): PredictSortResult
    {
        var globalMaximum  = SORTING.PREDICT.START_WEIGHT
        var doRotations    = 0
        var startRequestId = 0

        while (startRequestId < trimmedRequestSize)
        {
            var localMaximum  = SORTING.PREDICT.START_WEIGHT
            var requestId     = startRequestId

            logM.logMd("[i] Predict sort search round: $startRequestId", Debug.GENERIC)

            while  (requestId   <  trimmedRequestSize + startRequestId)
            {
                val curRequest  = requested[requestId - startRequestId]
                val storageBall = _storageCells[Request.SEARCH_ORDER[requestId % MAX_BALL_COUNT]]
                //  Taking the module for the storage ball by 3 prevents counting empty mobile slot


                if (storageBall.isTrueMatch(curRequest.name))
                    localMaximum += TRUE_MATCH_WEIGHT
                else if (storageBall.isPseudoMatch(curRequest.name))
                    localMaximum += PSEUDO_MATCH_WEIGHT
                else   requestId += trimmedRequestSize

                logM.logMd("[=] RequestId: ${requestId % MAX_BALL_COUNT}, " +
                        "did match: ${requestId < trimmedRequestSize + startRequestId}"
                        + "\n    Request ball: ${curRequest.name}, storage ball: ${storageBall.name}",
                    Debug.GENERIC)

                requestId++
            }

            if (localMaximum > globalMaximum)
            {
                doRotations = startRequestId
                globalMaximum = localMaximum

                logM.logMd("Found new global maximum: $globalMaximum", Debug.GENERIC)

                if (globalMaximum == requested.size * TRUE_MATCH_WEIGHT)
                {
                    logM.logMd("Found optimal state, ending search in advance", Debug.LOGIC)
                    return PredictSortResult(
                        doRotations,
                        globalMaximum,
                        MAX_BALL_COUNT)
                }
            }

            startRequestId++
        }

        logM.logMd("Done searching, max: $globalMaximum, rotations: $doRotations", Debug.END)
        return PredictSortResult(
            doRotations,
            globalMaximum,
            floor(globalMaximum / PSEUDO_MATCH_WEIGHT).toInt())
    }
    fun predictSortSearch(requested: Array<BallRequest.Name>): PredictSortResult
    {
        val trimmedRequestSize = min(requested.size, MAX_BALL_COUNT)
        if (trimmedRequestSize == 0)
            return PredictSortResult(0,
                0.0, 0)

        val requestedFullData  = Array(requested.size) { BallRequest(requested[it]) }
        return predictSortSearchLogic(requestedFullData, trimmedRequestSize)
    }
    fun initiatePredictSort(
        requested: Array<BallRequest.Name>): PredictSortResult
    {
        val trimmedRequestSize = min(requested.size, MAX_BALL_COUNT)
        if (trimmedRequestSize == 0)
            return PredictSortResult(0,
                0.0, 0)

        hwReAdjustStorage()

        logM.logMd("Start predict sort search", Debug.START)
        val requestedFullData  = Array(requested.size) { BallRequest(requested[it]) }
        val searchResult = predictSortSearchLogic(requestedFullData, trimmedRequestSize)

        logM.logMd("Best score: ${searchResult.maxSequenceScore}", Debug.GENERIC)

        if (searchResult.maxSequenceScore >= PSEUDO_MATCH_WEIGHT)
            repeat (searchResult.totalRotations)
            { fullRotate() }

        return searchResult
    }



    fun lazySet(inputFromTurretSlotToBottom: Array<Ball.Name>)
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
    fun handleIntake(inputBall: Ball.Name)
    {
        logM.logMd("before intake:", Debug.GENERIC)
        logAllStorageData()

        var curSlot = StorageSlot.BOTTOM
        var rotationTime = Delay.MS.PUSH.PART
        while (curSlot < StorageSlot.MOBILE && _storageCells[curSlot].isEmpty())
        {
            curSlot++
            rotationTime += Delay.MS.PUSH.PART
        }
        curSlot--

        if (curSlot >= StorageSlot.BOTTOM) _storageCells[curSlot].set(inputBall)

        logM.logMd("software storage processing finished", Debug.END)
        logM.logMd("new storage: ", Debug.GENERIC)
        logAllStorageData()

        hwSortingM.reinstantiableForwardBeltsTime(
            rotationTime,
            curSlot == StorageSlot.TURRET)
    }
    fun updateAfterShot()
    {
        _storageCells[StorageSlot.TURRET].set(_storageCells[StorageSlot.CENTER])
        _storageCells[StorageSlot.CENTER].set(_storageCells[StorageSlot.BOTTOM])
        _storageCells[StorageSlot.BOTTOM].set(_storageCells[StorageSlot.MOBILE])
        _storageCells[StorageSlot.MOBILE].empty()
    }



    fun fullRotate()
    {
        logM.logMd("storage before full rotation:", Debug.GENERIC)
        logAllStorageData()

        hwReAdjustStorage()

        hwSortingM.rotateMobileSlot()

        _storageCells[StorageSlot.MOBILE].set(_storageCells[StorageSlot.TURRET])
        _storageCells[StorageSlot.TURRET].empty()

        hwReAdjustStorage()

        logM.logMd("finished full rotation, new storage:", Debug.END)
        logAllStorageData()
    }


    private fun swReAdjustStorage(): Boolean
    {
        logM.logMd("SwReadjust round", Debug.LOGIC)
        if (_storageCells[StorageSlot.TURRET].isEmpty()
            && isNotEmpty())
        {
            updateAfterShot()
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

        logM.logMd("finished readjusting", Debug.END)
        return false
    }
    fun hwReAdjustStorage()
    {
        _cms.canTriggerIntake = false

        while (swReAdjustStorage())
            hwSortingM.forwardBeltsTime(Delay.MS.PUSH.FULL)
    }



    private fun logAllStorageData()
    {
        logM.log("--- Storage status report ---\n" +
                "\n_______________________________________________         _________________" +
                "\n[:  [↓] BTM #0  |  [→] CTR #1  |  [↑] TRT #2  |---   ---|  [←] MBL #3  :]" +
                "\n|:  ${_storageCells[StorageSlot.BOTTOM].formattedName()}  "
                + "|  ${_storageCells[StorageSlot.CENTER].formattedName()}  "
                + "|  ${_storageCells[StorageSlot.TURRET].formattedName()}  |  -----  "
                + "|  ${_storageCells[StorageSlot.MOBILE].formattedName()}  :|" +
                "\n[:  BALL COUNT: ${anyBallCount()}" +
                "                             |---   ---|              :]" +
                "\n===============================================         =================\n ",
            Debug.GENERIC)
    }

    fun anyBallCount(): Int
    {
        var count = 0
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            if (_storageCells[curSlotId].isFilled()) count++
            curSlotId++
        }

        return count
    }

    fun alreadyFull() = anyBallCount() >= MAX_BALL_COUNT
    fun notFullYet()  = anyBallCount() <  MAX_BALL_COUNT

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


    fun ballCountPGA(): CountPGA
    {
        val intPG     = intArrayOf(0, 0, 0)
        var curSlotId = StorageSlot.BOTTOM

        while (curSlotId < STORAGE_SLOT_COUNT)
        {
            intPG[_storageCells[curSlotId].id]++
            curSlotId++
        }

        return toCountPGA(intPG)
    }
    fun toCountPGA(array: IntArray,
                   includeAbstractAny: Boolean = true) : CountPGA
    {
        val abstractAny = if (includeAbstractAny)
            array[Ball.UNKNOWN_COLOR] else 0

        return CountPGA(
            array[Ball.PURPLE],
            array[Ball.GREEN],
            abstractAny)
    }


//    fun handleIdenticalColorRequest(): StorageHandleIdenticalColorsEvent
//    {
//        val curStorage  = ballCountPGA()
//        val purpleCount = curStorage.purple
//        val greenCount  = curStorage.green
//
//        return if (greenCount > purpleCount)
//            StorageHandleIdenticalColorsEvent(
//                greenCount,
//                Ball.Name.GREEN)
//
//        else StorageHandleIdenticalColorsEvent(
//            purpleCount,
//            Ball.Name.PURPLE)
//    }
}