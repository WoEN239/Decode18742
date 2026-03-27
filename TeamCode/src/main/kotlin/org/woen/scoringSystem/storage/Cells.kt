package org.woen.scoringSystem.storage


import kotlin.math.min
import kotlin.math.floor

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest
import org.woen.enumerators.StorageSlot

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.collector.RunMode
import org.woen.modules.BallCountUpdateEvent

import org.woen.scoringSystem.ConnectorModuleStatus
import org.woen.scoringSystem.storage.hardware.HwSortingManager

import org.woen.configs.Delay
import org.woen.configs.DebugSettings
import org.woen.configs.RobotSettings.ROBOT
import org.woen.configs.RobotSettings.SORTING
import org.woen.configs.RobotSettings.SORTING.PREDICT.TRUE_MATCH_WEIGHT
import org.woen.configs.RobotSettings.SORTING.PREDICT.PSEUDO_MATCH_WEIGHT



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


const val MAX_BALL_COUNT     = 3
const val STORAGE_SLOT_COUNT = 4



class PredictSortResult(
    var totalRotations: Int,
    var maxSequenceScore: Double,
    var totalMatches: Int)



class Cells
{
    private val _storageCells: Array<Ball>
    private val _cms: ConnectorModuleStatus
    val hwSortingM: HwSortingManager

    val logM: LogManager



    constructor(cms: ConnectorModuleStatus)
    {
        _cms = cms

        _storageCells = if (_cms.collector.runMode == RunMode.AUTO)
             ROBOT.AUTONOMOUS_INITIAL_LOAD_FROM_TURRET_TO_BOTTOM
        else ROBOT.TELEOP_INITIAL_LOAD_FROM_TURRET_TO_BOTTOM

        hwSortingM = HwSortingManager(_cms)

        logM = LogManager(_cms.collector.telemetry, DebugSettings.CELLS)
        updateBallCountForLEDLINE()
    }



    private fun predictSortSearchLogic(
        requested: Array<BallRequest>,
        trimmedRequestSize: Int,
        onlyInSequence: Boolean): PredictSortResult
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
                val storageBall = _storageCells[ROBOT.REQUEST_SEARCH_ORDER[requestId % MAX_BALL_COUNT]]
                //  Taking the module for the storage ball by 3 prevents counting empty mobile slot


                if (storageBall.isTrueMatch(curRequest))
                    localMaximum += TRUE_MATCH_WEIGHT
                else if (storageBall.isPseudoMatch(curRequest))
                    localMaximum += PSEUDO_MATCH_WEIGHT
                else if (onlyInSequence)
                    requestId += trimmedRequestSize

                logM.logMd("[=] RequestId: ${requestId % MAX_BALL_COUNT}, " +
                        "did match: ${requestId < trimmedRequestSize + startRequestId}"
                        + "\n    Request ball: ${curRequest}, storage ball: ${storageBall.name}",
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
    fun predictSortSearch(
        requested: Array<BallRequest>,
        onlyInSequence: Boolean): PredictSortResult
    {
        val trimmedRequestSize = min(requested.size, MAX_BALL_COUNT)
        if (trimmedRequestSize == 0)
            return PredictSortResult(0,
                0.0, 0)

        val patternString = requested.joinToString(", ")
        logM.logMd("Start predict sort search, PATTERN: $patternString", Debug.START)
        return predictSortSearchLogic(
            requested, trimmedRequestSize, onlyInSequence)
    }



    fun lazySet(inputFromTurretSlotToBottom: Array<Ball.Name>)
    {
        if (inputFromTurretSlotToBottom.size > MAX_BALL_COUNT) return

        var    curSlot  = StorageSlot.BOTTOM
        while (curSlot <= StorageSlot.TURRET)
        {
            _storageCells[ROBOT.INTAKE_INPUT_ORDER[curSlot]].set(
                inputFromTurretSlotToBottom[curSlot])
            curSlot++
        }

        updateBallCountForLEDLINE()
        logAllStorageData()
    }
    fun handleIntake(inputBall: Ball.Name)
    {
        logM.logMd("Color sensors triggered intake: $inputBall", Debug.START)
        var curSlot = StorageSlot.BOTTOM
        var rotationTime = Delay.MS.PUSH.PART
        while (curSlot < StorageSlot.MOBILE && _storageCells[curSlot].isEmpty())
        {
            curSlot++
            rotationTime += Delay.MS.PUSH.PART
        }
        curSlot--

        if (curSlot >= StorageSlot.BOTTOM) _storageCells[curSlot].set(inputBall)
        updateBallCountForLEDLINE(MAX_BALL_COUNT - curSlot)

        logM.logMd("Storage after intake: ", Debug.GENERIC)
        logAllStorageData()

        if (_cms.shootingPhase.isActive())
             hwSortingM.extendableForward(rotationTime)
        else hwSortingM.reinstantiableForward(rotationTime)
    }
    fun updateAfterShot()
    {
        hwSortingM.timeSinceLastShotUpdateMs = hwSortingM.rotatingBeltsTimer.milliseconds()

        _storageCells[StorageSlot.TURRET].set(_storageCells[StorageSlot.CENTER])
        _storageCells[StorageSlot.CENTER].set(_storageCells[StorageSlot.BOTTOM])
        _storageCells[StorageSlot.BOTTOM].set(_storageCells[StorageSlot.MOBILE])
        _storageCells[StorageSlot.MOBILE].empty()

        val ballCount = anyBallCount()
        logM.logMd("Considering shot fired, ballCount after update: $ballCount", Debug.STATUS)
        updateBallCountForLEDLINE(ballCount)
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
    fun hwReAdjustStorage(initialBeltPush: Long = 0)
    {
        if (_cms.calibrationPhase.isActive())
            return logM.logMd("-!- Failed to perform storage ReAdjustment", Debug.ERROR)

        _cms.canTriggerIntake = false

        var beltPushTime: Long = initialBeltPush
        while (swReAdjustStorage())
            beltPushTime += Delay.MS.PUSH.FULL

        hwSortingM.reinstantiableForward(beltPushTime)
    }



    fun logAllStorageData()
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
            Debug.STATUS)
    }
    fun updateBallCountForLEDLINE(ballCount: Int = -1)
        =   _cms.collector.eventBus.invoke(
                BallCountUpdateEvent(
                    if (ballCount != -1) ballCount
                    else anyBallCount()))

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
    fun isLastBall() = anyBallCount() == 1

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
}