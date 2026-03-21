package org.woen.scoringSystem.storage


import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.RequestResult
import org.woen.enumerators.Shooting

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.scoringSystem.misc.DynamicPattern
import org.woen.scoringSystem.ConnectorModuleStatus

import org.woen.configs.Alias.Request
import org.woen.configs.DebugSettings

import org.woen.configs.Delay
import org.woen.configs.RobotSettings.CONTROLS



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

        if (failsafeOrder.isNullOrEmpty() ||
            failsafeOrder.contentEquals(requestOrder))
            return startCustomisableDrumRequest(
                shootingMode, requestOrder, autoCorrectRequestPattern)

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
        val targetSorting = cells.predictSortSearch(requested, onlyInSequence)
        if (targetSorting.totalMatches == 0) return Request.COLORS_NOT_PRESENT

        sortingPhase1(targetSorting.totalRotations)
        return Request.ROGER_STARTING_SORTING
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
        cells.hwSortingM.timeSinceLastShotUpdateMs =
            cells.hwSortingM.rotatingBeltsTimer.milliseconds()

        if (shotCount == -1) cells.hwSortingM.hwMotors.openLaunch()
    }
    fun streamDrumPhase4()
    {
        if (cells.isNotEmpty()) cells.hwSortingM.calibrationPhase1()
        else cells.hwSortingM.calibrationPhase2()
    }

    fun finishCalibration()
    {
        _cms.calibrationPhase.setInactive()
        if (cells.notFullYet() && _cms.sortingPhase.isInactive())
        {
            _cms.canTriggerIntake = true
            cells.hwSortingM.hwMotors.forwardBrush()
        }
    }



    fun unsafeTestSorting()
    {
        val fill = arrayOf(Ball.Name.GREEN, Ball.Name.PURPLE, Ball.Name.PURPLE)
        cells.lazySet(fill)

        if (_cms.sortingPhase.isActive())
            return logM.logMd("-!- Could not start SortingTest:" +
                    " sorting is already active", Debug.ERROR)

        sortingPhase1(100)
    }

    fun sortingPhase1(totalRotations: Int)
    {
        _cms.sortingPhase.startPhase1()
        _cms.sortingPhase.remainingRotations = totalRotations

        cells.hwSortingM.hwMotors.closeTurretGate()
    }
    fun sortingPhase2()
    {
        _cms.sortingPhase.switchToNextPhase()
        cells.hwReAdjustStorage()
    }
}