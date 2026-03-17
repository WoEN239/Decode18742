package org.woen.scoringSystem.storage


import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.RequestResult
import org.woen.enumerators.Shooting

import org.woen.utils.debug.LogManager

import org.woen.configs.Alias.Request
import org.woen.configs.DebugSettings

import org.woen.utils.debug.Debug
import org.woen.configs.Delay
import org.woen.configs.ProcessId
import org.woen.configs.RobotSettings.CONTROLS
import org.woen.scoringSystem.ConnectorModuleStatus
import org.woen.scoringSystem.misc.DynamicPattern



class Storage
{
    private val _cms: ConnectorModuleStatus
    val cells: Cells
    val logM: LogManager



    constructor(cms: ConnectorModuleStatus)
    {
        _cms = cms

        cells = Cells(_cms)
        
        logM  = LogManager(_cms.collector, DebugSettings.SSM)
    }



    fun relink()
    {
        _cms.dynamicMemoryPattern.fullReset()
        logM.relink(DebugSettings.SSM)
    }




    fun unsafeTestSorting()
    {
        val fill = arrayOf(Ball.Name.GREEN, Ball.Name.PURPLE, Ball.Name.PURPLE)
        cells.lazySet(fill)

        var iteration = 0
        while (iteration < 100)
        {
            logM.logMd("\nIteration: $iteration", Debug.GENERIC)

            cells.fullRotate()
            iteration++
        }
    }



    fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        autoCorrectPattern: Boolean = true): RequestResult.Name
    {
        if (cells.isEmpty()) return Request.FAIL_IS_EMPTY
        if (requestOrder.isEmpty()) return Request.ILLEGAL_ARGUMENT

        val  standardPatternOrder = if (!autoCorrectPattern) requestOrder
        else DynamicPattern.trimPattern(
             _cms.dynamicMemoryPattern.lastUnfinished(),
             requestOrder)

        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> streamDrumRequest()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> shootFinalPhase(
                    standardPatternOrder,
                    false)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> shootFinalPhase(
                        standardPatternOrder,
                        true)
            }

        resumeLogicAfterRequest(
            ProcessId.DRUM_REQUEST,
            cells.isNotEmpty())
        return requestResult
    }
    fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>? = requestOrder,
        autoCorrectRequestPattern:  Boolean = true,
        autoCorrectFailsafePattern: Boolean = true): RequestResult.Name
    {
        if (cells.isEmpty()) return Request.FAIL_IS_EMPTY

        if (failsafeOrder == null || failsafeOrder.isEmpty() ||
            failsafeOrder.contentEquals(requestOrder))
            return shootEntireDrumRequest(shootingMode, requestOrder, autoCorrectRequestPattern)

        val  standardPatternOrder = if (!autoCorrectRequestPattern) requestOrder
        else DynamicPattern.trimPattern(
            _cms.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)

        val  failsafePatternOrder = if (!autoCorrectFailsafePattern) requestOrder
        else DynamicPattern.trimPattern(
            _cms.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)


        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> streamDrumRequest()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> choosePatternForShot(
                    standardPatternOrder,
                    failsafePatternOrder,
                        false)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                    -> choosePatternForShot(
                        standardPatternOrder,
                        failsafePatternOrder,
                        true)
            }

        resumeLogicAfterRequest(
            ProcessId.DRUM_REQUEST,
            cells.isNotEmpty())
        return requestResult
    }



    fun streamDrumRequest(ballCount: Int = 0): RequestResult.Name
    {
        val shotCount = if (ballCount != 0) ballCount
                   else if (CONTROLS.USE_LAZY_VERSION_OF_STREAM_DRUM) 3
                   else cells.anyBallCount()
        
        logM.logMd("Starting stream shooting, count: $ballCount", Debug.START)

        val beltPushTime = when (shotCount)
        {
            3    -> Delay.MS.SHOOTING.FIRE_3
            2    -> Delay.MS.SHOOTING.FIRE_2
            else -> Delay.MS.SHOOTING.FIRE_1
        }

        logM.logMd("Firing time: $beltPushTime", Debug.GENERIC)


//        cells.hwSortingM.stopBelts()
//        cells.hwSortingM.openTurretGate()

//        cells.hwSortingM.forwardBeltsTime(beltPushTime)

        repeat(shotCount)
        { cells.updateAfterShot() }

        return if (cells.isEmpty()) Request.SUCCESS_NOW_EMPTY else Request.SUCCESS
    }


    fun choosePatternForShot(
        requested: Array<BallRequest.Name>,
        failsafe:  Array<BallRequest.Name>,
        shootOnlyValid: Boolean
    ): RequestResult.Name
    {
        val req1 = cells.predictSortSearch(requested).maxSequenceScore
        val req2 = cells.predictSortSearch(failsafe).maxSequenceScore

        return shootFinalPhase(
            if (req1 > req2) requested
            else failsafe, shootOnlyValid)
    }
    fun shootFinalPhase(
        requested: Array<BallRequest.Name>,
        shootOnlyValid: Boolean
    ): RequestResult.Name
    {
        val afterSorting = cells.initiatePredictSort(requested)
        return streamDrumRequest( if (shootOnlyValid)
            afterSorting.totalMatches else 0)
    }



    fun resumeLogicAfterRequest(
        processId: Int,
        doAutoCalibration: Boolean = true)
    {
        logM.logMd("RESUME AFTER REQUEST, process: $processId", Debug.PROCESS_NAME)

        if (doAutoCalibration)
        {
            logM.logMd("Reversing belts for calibration", Debug.START)
            cells.hwSortingM.reverseBeltsTime(Delay.MS.PUSH.HALF)
            logM.logMd("Finished reversing", Debug.END)

            logM.logMd("Starting calibration", Debug.START)
            cells.hwSortingM.forwardBeltsTime(Delay.MS.PUSH.HALF)
            cells.hwSortingM.fullCalibrate()
        }
        else cells.hwSortingM.fullCalibrate()


        logM.logMd("Phase 2 - RESUME AFTER REQUEST, process: $processId", Debug.LOGIC)

        _cms.canTriggerIntake = true

        logM.logMd("FINISHED resume logic", Debug.END)
    }
}