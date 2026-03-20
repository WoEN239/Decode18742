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
import org.woen.configs.RobotSettings.CONTROLS
import org.woen.enumerators.CalibrationPhase
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
        
        logM  = LogManager(_cms.collector.telemetry, DebugSettings.SSM)
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



    fun startCustomisableDrumRequest(
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

        return when (shootingMode)
        {
            Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                -> streamDrumPhase1()

            Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                -> shootFinalPhase(
                standardPatternOrder,
                false)

            Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                -> shootFinalPhase(
                    standardPatternOrder,
                    true)
        }
    }
    fun startCustomisableDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>? = requestOrder,
        autoCorrectRequestPattern:  Boolean = true,
        autoCorrectFailsafePattern: Boolean = true): RequestResult.Name
    {
        if (cells.isEmpty()) return Request.FAIL_IS_EMPTY

        if (failsafeOrder == null || failsafeOrder.isEmpty() ||
            failsafeOrder.contentEquals(requestOrder))
            return startCustomisableDrumRequest(shootingMode, requestOrder, autoCorrectRequestPattern)

        val  standardPatternOrder = if (!autoCorrectRequestPattern) requestOrder
        else DynamicPattern.trimPattern(
            _cms.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)

        val  failsafePatternOrder = if (!autoCorrectFailsafePattern) requestOrder
        else DynamicPattern.trimPattern(
            _cms.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)


        return when (shootingMode)
        {
            Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                -> streamDrumPhase1()

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
    }



    fun streamDrumPhase1(ballCount: Int = 0): RequestResult.Name
    {
        if (_cms.shootingPhase.isActive())
            return Request.IGNORED_DUPLICATE_COMMAND

        _cms.shootingPhase.startPhase1()
        _cms.shootingPhase.ballCountForPhase1 =
            if (ballCount == 1 && cells.isLastBall()
                && CONTROLS.USE_LAUNCHER_FOR_LAST_BALL)
            -1 else ballCount

        logM.logMd("StreamDrum phase 1, debug ballCount: $ballCount", Debug.LOGIC)
        cells.hwSortingM.hwMotors.openTurretGate()

        return Request.ROGER_STARTING_SHOOTING
    }
    fun streamDrumPhase2()
    {
        val shotCount = if (_cms.shootingPhase.ballCountForPhase1 != 0)
            _cms.shootingPhase.ballCountForPhase1
        else if (CONTROLS.USE_LAZY_VERSION_OF_STREAM_DRUM) 3
        else cells.anyBallCount()

        logM.logMd("StreamDrum phase 2, shot count: $shotCount", Debug.LOGIC)

        val beltPushTime = when (shotCount)
        {
            3    -> Delay.MS.SHOOTING.FIRE_3
            2    -> Delay.MS.SHOOTING.FIRE_2
            1    -> Delay.MS.SHOOTING.FIRE_1
            else -> Delay.MS.SHOOTING.FIRE_LAST_WITH_LAUNCHER
        }

        logM.logMd("Firing time: $beltPushTime", Debug.GENERIC)
        cells.hwSortingM.extendableForward(beltPushTime)
        if (shotCount == -1) cells.hwSortingM.hwMotors.openLaunch()
    }
    fun streamDrumPhase4()
    {

    }


    private fun choosePatternForShot(
        requested: Array<BallRequest.Name>,
        failsafe:  Array<BallRequest.Name>,
        onlyInSequence: Boolean
    ): RequestResult.Name
    {
        val req1 = cells.predictSortSearch(requested, onlyInSequence).maxSequenceScore
        val req2 = cells.predictSortSearch(
            failsafe, onlyInSequence).maxSequenceScore

        return shootFinalPhase(
            if (req1 > req2) requested
            else failsafe, onlyInSequence)
    }
    private fun shootFinalPhase(
        requested: Array<BallRequest.Name>,
        onlyInSequence: Boolean
    ): RequestResult.Name
    {
        val afterSorting = cells.initiatePredictSort(requested, onlyInSequence)
        if (afterSorting.totalMatches == 0) return Request.COLORS_NOT_PRESENT

        return streamDrumPhase1(if (onlyInSequence)
            afterSorting.totalMatches else 0)  // 0 is auto option
    }



    fun resumeAfterRequestPhase1()
    {
        if (cells.isNotEmpty())
        {
            _cms.calibrationPhase.set(CalibrationPhase.Name.P1_REVERSING_BELTS)
            cells.hwSortingM.extendableReverse(Delay.MS.PUSH.FULL)
        }
        else resumeAfterRequestPhase2()
    }
    fun resumeAfterRequestPhase2()
    {
        _cms.calibrationPhase.set(CalibrationPhase.Name.P2_CLOSING_ALL_SERVOS)
        cells.hwSortingM.startCalibration()

        _cms.canTriggerIntake = true
        logM.logMd("FINISHED resume logic", Debug.END)
    }
}