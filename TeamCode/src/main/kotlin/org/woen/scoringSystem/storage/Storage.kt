package org.woen.scoringSystem.storage


import org.woen.collector.RunMode
import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest
import org.woen.enumerators.Shooting
import org.woen.enumerators.RequestResult

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.scoringSystem.misc.DynamicPattern
import org.woen.scoringSystem.ConnectorModuleStatus

import org.woen.scoringSystem.SMC_IsEndGameEvent
import org.woen.scoringSystem.SMC_GetCurrentGameTimerEvent

import org.woen.configs.Delay
import org.woen.configs.Hardware
import org.woen.configs.DebugSettings
import org.woen.configs.RobotSettings.CONTROLS
import org.woen.configs.RobotSettings.TELEOP



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



    fun tryHandleIntake()
    {
        if (_cms.sortingPhase.isActive() ||
            _cms.shootingPhase.isShootingPhase3() ||
            _cms.shootingPhase.isShootingPhase4() ||
           !_cms.canTriggerIntake ||
            cells.alreadyFull()) return

        val inputBall = cells.hwSortingM.updateColors()
        if (inputBall == Ball.Name.NONE) return

        cells.handleIntake(inputBall)

        if (cells.alreadyFull())
        {
            _cms.collector.opMode.gamepad1.rumble(
                Delay.MS.GAMEPAD_RUMBLE_STORAGE_IS_NOW_FULL)

            if (CONTROLS.TRY_AUTO_SORT_WHEN_FULL_IN_ENDGAME
                && _cms.collector.eventBus.invoke(
                    SMC_IsEndGameEvent()).isEndGame)
            {
                logM.logMd("Starting auto sorting [EndGame]", Debug.START)

                startCustomisableDrumRequest(
                    TELEOP.PATTERN_SHOOTING_MODE,
                    _cms.dynamicMemoryPattern.permanent(),
                    TELEOP.AUTOCORRECT_REQUEST_PATTERN)
        }   }
    }



    fun startCustomisableDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest>,
        shootAfterSorting: Boolean = true,
        autoCorrectPattern: Boolean = true): RequestResult
    {
        if (cells.isEmpty()) return RequestResult.FAIL_IS_EMPTY
        if (requestOrder.isEmpty()) return RequestResult.FAIL_ILLEGAL_ARGUMENT

        val  standardPatternOrder = if (!autoCorrectPattern) requestOrder
        else DynamicPattern.trimPattern(
                requestOrder,
                _cms.dynamicMemoryPattern.offset())

        return when (shootingMode)
        {
            Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                -> if (shootAfterSorting) streamDrumPhase1()
                   else RequestResult.SORTING_FAILED_NOT_SHOOTING

            Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                -> initiateSorting(
                    standardPatternOrder,
                    false,
                    shootAfterSorting)

            Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                -> initiateSorting(
                    standardPatternOrder,
                    true,
                    shootAfterSorting)
        }
    }
    fun startCustomisableDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest>,
        failsafeOrder: Array<BallRequest>? = requestOrder,
        shootAfterSorting: Boolean = true,
        autoCorrectRequestPattern:  Boolean = true,
        autoCorrectFailsafePattern: Boolean = true): RequestResult
    {
        if (cells.isEmpty()) return RequestResult.FAIL_IS_EMPTY

        if (failsafeOrder.isNullOrEmpty() ||
            failsafeOrder.contentEquals(requestOrder))
            return startCustomisableDrumRequest(
                shootingMode, requestOrder, autoCorrectRequestPattern)

        val  standardPatternOrder = if (!autoCorrectRequestPattern) requestOrder
        else DynamicPattern.trimPattern(requestOrder,
                _cms.dynamicMemoryPattern.offset())

        val  failsafePatternOrder = if (!autoCorrectFailsafePattern) requestOrder
        else DynamicPattern.trimPattern(requestOrder,
                _cms.dynamicMemoryPattern.offset())


        return when (shootingMode)
        {
            Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                -> if (shootAfterSorting) streamDrumPhase1()
                   else RequestResult.SORTING_FAILED_NOT_SHOOTING

            Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                -> choosePatternBeforeSorting(
                    standardPatternOrder,
                    failsafePatternOrder,
                    false,
                    shootAfterSorting)

            Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN
                -> choosePatternBeforeSorting(
                    standardPatternOrder,
                    failsafePatternOrder,
                    true,
                    shootAfterSorting)
        }
    }



    private fun choosePatternBeforeSorting(
        requested: Array<BallRequest>,
        failsafe:  Array<BallRequest>,
        onlyInSequence: Boolean,
        shootAfterSorting: Boolean = true
    ): RequestResult
    {
        val req1 = cells.predictSortSearch(requested, onlyInSequence)
        val req2 = cells.predictSortSearch(
            failsafe, onlyInSequence)

        val pickDefault = req1.maxSequenceScore > req2.maxSequenceScore

        return initiateSorting(
                if (pickDefault) requested
                else failsafe,
            onlyInSequence,
            shootAfterSorting,
                if (pickDefault) req1
                else req2)
    }
    private fun initiateSorting(
        requested: Array<BallRequest>,
        onlyInSequence: Boolean,
        shootAfterSorting: Boolean = true,
        predictSortResult: PredictSortResult? = null
    ): RequestResult
    {
        val targetSorting = predictSortResult ?:
            cells.predictSortSearch(requested, onlyInSequence)

        if (targetSorting.totalMatches == 0)
        {
            _cms.sortingPhase.setInactive()
            return RequestResult.FAIL_COLORS_NOT_PRESENT
        }

        if (shootAfterSorting) _cms.shootingPhase.startPhase0()

        if (targetSorting.totalRotations == 0)
        {
            _cms.sortingPhase.setInactive()
            return RequestResult.SORTING_IGNORED_PATTERN_ALREADY_ALIGNED
        }

        sortingPhase1(targetSorting.totalRotations)
        return RequestResult.ROGER_STARTING_SORTING
    }



    fun streamDrumPhase1(
        laterGamepadHold: Boolean = false,
        ballCount: Int = 0): RequestResult
    {
        if (_cms.shootingPhase.isShootingPhase0())
            return RequestResult.FAIL_AWAITING_SORTING
        if (_cms.shootingPhase.isActive())
            return RequestResult.FAIL_IGNORE_DUPLICATE_COMMAND

        _cms.shootingPhase.startPhase1(laterGamepadHold)
        _cms.shootingPhase.ballCountForPhase1 =
            if (ballCount == 1 && cells.isLastBall()
                && CONTROLS.USE_LAUNCHER_FOR_LAST_BALL)
                -1 else ballCount

        if (_cms.lazyIntakeIsActive) cells.hwSortingM.hwMotors.stopBelts()

        logM.logMd("StreamDrum phase 1, debug ballCount: $ballCount", Debug.LOGIC)
        cells.hwSortingM.hwMotors.openTurretGate()

        return RequestResult.ROGER_STARTING_SHOOTING
    }
    fun streamDrumPhase2()
    {
        _cms.shootingPhase.switchToNextPhase()

        val shotCount = if (_cms.shootingPhase.ballCountForPhase1 != 0)
            _cms.shootingPhase.ballCountForPhase1
        else if (CONTROLS.USE_LAZY_VERSION_OF_STREAM_DRUM) 3
        else cells.anyBallCount()

        logM.logMd("StreamDrum phase 2, shot count: $shotCount", Debug.LOGIC)

        var beltPushTime = if (_cms.shootingPhase.shotBeltsVoltage
            == Hardware.MOTOR.BELTS_FOR_FAST_SHOOTING)
                when(shotCount)
                {
                    3    -> Delay.MS.SHOOTING.FAST_3
                    2    -> Delay.MS.SHOOTING.FAST_2
                    1    -> Delay.MS.SHOOTING.FAST_1
                    else -> Delay.MS.SHOOTING.FAST_LAST_WITH_LAUNCHER
                }
                else when(shotCount)
                {
                    3    -> Delay.MS.SHOOTING.SLOW_3
                    2    -> Delay.MS.SHOOTING.SLOW_2
                    1    -> Delay.MS.SHOOTING.SLOW_1
                    else -> Delay.MS.SHOOTING.SLOW_LAST_WITH_LAUNCHER
                }

        if (_cms.collector.runMode == RunMode.MANUAL)
            beltPushTime += Delay.MS.SHOOTING.ADDITIONAL_TOLERANCE_FOR_TELEOP

        logM.logMd("Firing time: $beltPushTime", Debug.GENERIC)

        if (_cms.shootingPhase.isGamepadHoldPhase2())
            cells.hwSortingM.hwMotors.forwardBelts(
                onTime = false, _cms.shootingPhase.shotBeltsVoltage)
        else cells.hwSortingM.extendableForward(
            beltPushTime, _cms.shootingPhase.shotBeltsVoltage)

        cells.hwSortingM.timeSinceLastShotUpdateMs =
            cells.hwSortingM.rotatingBeltsTimer.milliseconds()

        if (shotCount == -1) cells.hwSortingM.hwMotors.openLaunch()
    }
    fun streamDrumPhase4()
    {
        logM.logMd("StreamDrum phase 4, starting calibration", Debug.LOGIC)
        _cms.shootingPhase.shotBeltsVoltage = Hardware.MOTOR.BELTS_FOR_FAST_SHOOTING

        if (cells.isNotEmpty()) cells.hwSortingM.calibrationPhase1()
        else cells.hwSortingM.calibrationPhase2()
    }

    fun finishCalibration()
    {
        logM.logMd("Finishing calibration", Debug.LOGIC)

        cells.hwSortingM.hwMotors.stopBelts()

        _cms.calibrationPhase.setInactive()
        _cms.shootingPhase.setInactive()
        if (cells.notFullYet() && _cms.sortingPhase.isInactive())
        {
            _cms.canTriggerIntake = true

            if (CONTROLS.AUTO_ENABLE_BRUSHES_AFTER_SHOOTING)
                 cells.hwSortingM.hwMotors.forwardBrush(onTime = false)
            else cells.hwSortingM.hwMotors.stopBrush()


            if (CONTROLS.AUTO_PRESERVE_LAZY_INTAKE_STATUS
                && _cms.lazyIntakeIsActive)
            {
                cells.hwSortingM.hwMotors.forwardBrush(onTime = false)
                cells.hwSortingM.hwMotors.forwardBelts(onTime = false)
            }
        }
    }



    fun sortingPhase1(totalRotations: Int)
    {
        logM.logMd("Sorting phase 1, closing turretGate", Debug.LOGIC)
        _cms.sortingPhase.startPhase1()
        _cms.sortingPhase.remainingRotations = totalRotations
        cells.hwSortingM.hwMotors.openGate()

        if (_cms.turretGateStatus.isClosed())
            sortingPhaseRealignment(ifDoneSkipToPhase3 = true)
        else
        {
            if (_cms.lazyIntakeIsActive) cells.hwSortingM.hwMotors.stopBelts()

            cells.hwSortingM.hwMotors.closeTurretGate()
        }
    }
    fun sortingPhaseRealignment(
        ifDoneSkipToPhase3: Boolean,
        initialBeltPush: Long = 0)
    {
        logM.logMd("Sorting HW ReAdjustment (Phase 2 or 9)", Debug.LOGIC)
        _cms.sortingPhase.switchToNextPhase()

        if (cells.hwReAdjustStorage(initialBeltPush))
        {
            if (ifDoneSkipToPhase3)
                sortingPhase3()
            else if (_cms.sortingPhase.remainingRotations < 2)
                cells.hwSortingM.hwMotors.stopBelts()
        }
    }
    fun sortingPhase3()
    {
        logM.logMd("Sorting phase 3, forwards realignment", Debug.LOGIC)
        _cms.sortingPhase.switchToNextPhase()
        val timeMs = Delay.MS.REALIGNMENT.SORTING_FORWARD

        if (timeMs <= 0) sortingPhase4()
        else cells.hwSortingM.reinstantiableForward(timeMs)
    }
    fun sortingPhase4()
    {
        logM.logMd("Sorting phase 4, reverse realignment", Debug.LOGIC)
        _cms.sortingPhase.switchToNextPhase()
        val timeMs = Delay.MS.REALIGNMENT.SORTING_REVERSE

        if (timeMs <= 0) sortingPhase5()
        else cells.hwSortingM.reinstantiableReverse(
            timeMs, Hardware.MOTOR.BELTS_REVERSE)
    }
    fun sortingPhase5()
    {
        logM.logMd("Sorting phase 5, opening gate", Debug.LOGIC)
        _cms.sortingPhase.switchToNextPhase()

        if (_cms.gateStatus.isOpened())
            sortingPhase6()
        else cells.hwSortingM.hwMotors.openGate()
    }
    fun sortingPhase6()
    {
        logM.logMd("Sorting phase 6, opening push", Debug.LOGIC)
        _cms.sortingPhase.switchToNextPhase()
        if (_cms.pushStatus.isOpened())
            sortingPhase7()
        else cells.hwSortingM.hwMotors.openPush()
    }
    fun sortingPhase7()
    {
        cells.rotateSwStorage()
        logM.logMd("Sorting phase 7, waiting for ball to fly in MOBILE position", Debug.LOGIC)
        _cms.sortingPhase.switchToNextPhase()

        if (Delay.MS.REALIGNMENT.WAITING_IN_SORTING_PASE_7 <= 0)
            return sortingPhase8()

        cells.hwSortingM.timeSinceLastShotUpdateMs =
            _cms.collector.eventBus.invoke(
                SMC_GetCurrentGameTimerEvent()
            ).timeMs
    }
    fun sortingPhase8()
    {
        _cms.sortingPhase.switchToNextPhase()

//        cells.hwSortingM.hwMotors.reverseBelts(onTime = false)
        cells.hwSortingM.hwMotors.forwardBelts(onTime = false, 9.0)


        if (_cms.pushStatus.isClosed() && (
            _cms.gateStatus.isClosed() ||
            _cms.sortingPhase.remainingRotations > 1))
                sortingPhaseRealignment(
                    ifDoneSkipToPhase3 = false,
                    Delay.MS.PUSH.PART)


        if (_cms.sortingPhase.remainingRotations < 2)
        {
            cells.hwSortingM.hwMotors.closeGateWithPush()
            logM.logMd("Sorting phase 8, closing gate with push" +
                    " (Rotations: ${_cms.sortingPhase.remainingRotations})", Debug.LOGIC)
        }
        else
        {
            logM.logMd("Sorting phase 8, closing ONLY push" +
                    " (Rotations: ${_cms.sortingPhase.remainingRotations})", Debug.LOGIC)
            cells.hwSortingM.hwMotors.closePush()
        }
    }
}