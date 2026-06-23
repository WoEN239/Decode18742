package org.woen.scoringSystem.storage


import org.woen.collector.RunMode
import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest
import org.woen.enumerators.Shooting
import org.woen.enumerators.StorageSlot
import org.woen.enumerators.RequestResult

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.scoringSystem.misc.DynamicPattern
import org.woen.scoringSystem.ConnectorModuleStatus

//import org.woen.scoringSystem.SMC_IsEndGameEvent
import org.woen.scoringSystem.SMC_GetCurrentGameTimerEvent

import org.woen.configs.DelayMS
import org.woen.configs.Hardware
import org.woen.configs.DebugSettings
import org.woen.configs.RobotSettings.AUTONOMOUS
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
        if (!_cms.canTriggerIntake ||
            _cms.sortingPhase.isActive() ||
            !_cms.launchStatus.isClosed() ||
            _cms.shootingPhase.isSensitivePhase() ||
            cells.alreadyFull()) return

        if (!_cms.colorResults.colorIntakeIsActive)
        {
            if (_cms.colorResults.parsedResults[StorageSlot.BOTTOM] == Ball.Name.NONE) return

            if (_cms.shootingPhase.isInactive())
                cells.hwSortingM.hwMotors.forwardBelts(onTime = false)

            _cms.colorResults.reactivateAllColorTargets()
            _cms.colorResults.intakePredictions.clearAll()
        }

        cells.updateStorageWithIntake()

        if (cells.alreadyFull())
        {
            if (CONTROLS.DO_RUMBLE_GAMEPAD_WHEN_STORAGE_FULL_BY_COLORS)
                _cms.collector.opMode.gamepad1.rumble(
                    DelayMS.GAMEPAD_RUMBLE_STORAGE_IS_NOW_FULL)

            /*if (CONTROLS.TRY_AUTO_SORT_WHEN_FULL_IN_ENDGAME
                && _cms.collector.eventBus.invoke(
                    SMC_IsEndGameEvent()).isEndGame)
            {
                logM.logMd("Starting auto sorting [EndGame]", Debug.START)

                startCustomisableDrumRequest(
                    TELEOP.PATTERN_SHOOTING_MODE,
                    _cms.dynamicMemoryPattern.permanent(),
                    TELEOP.AUTOCORRECT_REQUEST_PATTERN)
            }*/
        }
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
                -> if (shootAfterSorting) streamDrumP1()
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
                -> if (shootAfterSorting) streamDrumP1()
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

        if (targetSorting.matches == 0)
        {
            _cms.sortingPhase.setInactive()
            return RequestResult.FAIL_COLORS_NOT_PRESENT
        }

        if (shootAfterSorting) _cms.shootingPhase.startP0()

        if (targetSorting.rotations == 0)
        {
            _cms.sortingPhase.setInactive()
            return RequestResult.SORTING_IGNORED_PATTERN_ALREADY_ALIGNED
        }

        sortingP1(targetSorting.rotations)
        return RequestResult.ROGER_STARTING_SORTING
    }



    fun tryStartStreamDrum(laterGamepadHold: Boolean = false, ballCount: Int = 0)
        =    if (_cms.shootingPhase.isWaitingP0() || _cms.sortingPhase.isActive())
            RequestResult.FAIL_AWAITING_SORTING
        else if (_cms.shootingPhase.isActive() &&
            !CONTROLS.EXTEND_MANUAL_SHOOTING_ON_DOUBLE_PRESS)
            RequestResult.FAIL_IGNORE_DUPLICATE_COMMAND
        else if (_cms.turretGateStatus.isOpened())
             restartStreamDrumP2(laterGamepadHold, ballCount)
        else streamDrumP1(laterGamepadHold, ballCount)
    private fun startStreamDrumState(laterGamepadHold: Boolean = false, ballCount: Int = 0)
    {
        _cms.shootingPhase.startP1(laterGamepadHold)
        _cms.shootingPhase.ballCountForPhase1 =
            if (ballCount == 1 && cells.isLastBall() &&
                CONTROLS.USE_LAUNCHER_FOR_LAST_BALL) -1 else ballCount
    }
    private fun streamDrumP1(laterGamepadHold: Boolean = false, ballCount: Int = 0): RequestResult
    {
        cells.hwSortingM.hwMotors.forwardBelts(onTime = false, _cms.shootingPhase.shotBeltsVoltage)
        cells.hwSortingM.hwMotors.openTurretGate()
        cells.hwSortingM.hwMotors.forwardBrush(onTime = false)

        logM.logMd("StreamDrum P1, debug ballCount: $ballCount", Debug.LOGIC)
        startStreamDrumState(laterGamepadHold, ballCount)

        return RequestResult.ROGER_STARTING_SHOOTING
    }
    private fun restartStreamDrumP2(laterGamepadHold: Boolean = false, ballCount: Int = 0): RequestResult
    {
        startStreamDrumState(laterGamepadHold, ballCount)
        streamDrumP2()
        return RequestResult.ROGER_RESTARTED_SHOOTING
    }
    fun streamDrumP2()
    {
        _cms.shootingPhase.startP2(useColorSensors = CONTROLS.USE_COLOR_SENSORS_FOR_SHOOTING)

        if (_cms.shootingPhase.isNotOnTimeP2())
        {
            cells.hwSortingM.hwMotors.forwardBelts(
                onTime = false, _cms.shootingPhase.shotBeltsVoltage)

            val isLastBall = cells.isLastBall()
            if (isLastBall && CONTROLS.USE_LAUNCHER_FOR_LAST_BALL) streamDrumP3()
            else if (_cms.shootingPhase.isUntilColorsP2())
                _cms.colorResults.reactivateColorTargetsForShooting(isLastBall)

            logM.logMd("StreamDrum P2 (not on time)", Debug.LOGIC)
        }
        else
        {
            val shotCount = calcShotCount()
            val beltPushTime = calcShootingTimeForP2(shotCount)

            cells.hwSortingM.extendableForward(beltPushTime,
                _cms.shootingPhase.shotBeltsVoltage)
            if (shotCount == -1) cells.hwSortingM.hwMotors.openLaunch()

//            repeat (shotCount) { cells.updateAfterShot() }
            cells.clearStorageAfterShooting()

            cells.hwSortingM.lastUpdateTimestampMS =
                cells.hwSortingM.rotatingBeltsTimer.milliseconds()
            logM.logMd("StreamDrum P2, shots: $shotCount, firingMS: $beltPushTime", Debug.LOGIC)
        }
    }
    fun streamDrumP3()
    {
        cells.hwSortingM.hwMotors.logM.logMd("StreamDrum P3, opening launch", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.shootingPhase.startP3()

        cells.hwSortingM.hwMotors.reverseBrush(onTime = false)
        cells.hwSortingM.hwMotors.openLaunch()
        cells.hwSortingM.hwMotors.forwardBelts(onTime = false)
    }
    fun streamDrumCalibrationP4P5()
    {
        logM.logMd("StreamDrum P4, starting calibration", Debug.LOGIC)
        _cms.shootingPhase.shotBeltsVoltage = Hardware.MOTOR.BELTS_FOR_FAST_SHOOTING

        if (cells.isNotEmpty()) cells.hwSortingM.calibrationP4()
        else cells.hwSortingM.calibrationP5()
    }
    private fun calcShotCount(): Int
        = if (_cms.shootingPhase.ballCountForPhase1 != 0)
              _cms.shootingPhase.ballCountForPhase1
        else if (CONTROLS.USE_LAZY_VERSION_OF_STREAM_DRUM) 3
        else cells.anyBallCount()
    private fun calcShootingTimeForP2(shotCount: Int)
        = ( if (_cms.collector.runMode == RunMode.AUTO) 100
            else DelayMS.SHOOTING.ADDITIONAL_TOLERANCE_FOR_TELEOP
        ) + (
            if (_cms.shootingPhase.shotBeltsVoltage == Hardware.MOTOR.BELTS_FOR_FAST_SHOOTING)
                when (shotCount) {
                    3 -> DelayMS.SHOOTING.FAST_3
                    2 -> DelayMS.SHOOTING.FAST_2
                    1 -> DelayMS.SHOOTING.FAST_1
                    else -> DelayMS.SHOOTING.FAST_LAST_WITH_LAUNCHER
                }
            else when (shotCount) {
                    3 -> DelayMS.SHOOTING.SLOW_3
                    2 -> DelayMS.SHOOTING.SLOW_2
                    1 -> DelayMS.SHOOTING.SLOW_1
                    else -> DelayMS.SHOOTING.SLOW_LAST_WITH_LAUNCHER
            }   )


    fun finishCalibration()
    {
        if (CONTROLS.DO_RUMBLE_GAMEPAD_AFTER_SHOT)
            _cms.collector.opMode.gamepad1.rumble(200)

        logM.logMd("Finishing calibration", Debug.LOGIC)
        cells.hwSortingM.hwMotors.stopBelts()

        _cms.shootingPhase.setInactive()

        if (cells.notFullYet() && _cms.sortingPhase.isInactive())
        {
            _cms.colorResults.reactivateColorTargetsForIntake()
            _cms.canTriggerIntake = cells.hwSortingM.canUpdateColors()

            if (((_cms.collector.runMode == RunMode.AUTO &&
                AUTONOMOUS.PRESERVE_LAZY_INTAKE_STATUS_AFTER_SHOOTING
                ) || (
                _cms.lazyIntakeIsActive &&
                _cms.collector.runMode == RunMode.MANUAL &&
                TELEOP.PRESERVE_LAZY_INTAKE_STATUS_AFTER_SHOOTING)))
            {
                cells.hwSortingM.hwMotors.forwardBrush(onTime = false)
                cells.hwSortingM.hwMotors.forwardBelts(onTime = false)
            }
            else
            {
                if (CONTROLS.AUTO_ENABLE_BRUSHES_AFTER_SHOOTING)
                     cells.hwSortingM.hwMotors.forwardBrush(onTime = false)
                else cells.hwSortingM.hwMotors.stopBrush()

                _cms.lazyIntakeIsActive = false
            }
        }
    }



    fun sortingP1(totalRotations: Int)
    {
        logM.logMd("Sorting P1, closing turretGate", Debug.LOGIC)
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
        initialBeltPush: Long = 0,
        voltage: Double = Hardware.MOTOR.BELTS_FORWARD)
    {
        _cms.sortingPhase.switchToNextPhase()
        logM.logMd(if (ifDoneSkipToPhase3) "Sorting HW ReAdjustment (P2)"
            else "Sorting HW ReAdjustment (P9)", Debug.LOGIC)

        if (cells.hwReAdjustStorage(initialBeltPush, voltage))
        {
            if (ifDoneSkipToPhase3) sortingP3()
            else if (_cms.sortingPhase.remainingRotations < 2)
                cells.hwSortingM.hwMotors.stopBelts()
        }
    }
    fun sortingP3()
    {
        val timeMs = DelayMS.REALIGNMENT.SORTING_FORWARD
        if (timeMs <= 0) sortingP4()
        else
        {
            cells.hwSortingM.reinstantiableForward(timeMs)
            logM.logMd("Sorting P3, forwards realignment", Debug.LOGIC)
            _cms.sortingPhase.startPhase3()
        }
    }
    fun sortingP4()
    {
        val timeMs = DelayMS.REALIGNMENT.SORTING_REVERSE
        if (timeMs <= 0) sortingP5()
        else
        {
            cells.hwSortingM.reinstantiableReverse(timeMs, Hardware.MOTOR.BELTS_REVERSE)
            logM.logMd("Sorting P4, reverse realignment", Debug.LOGIC)
            _cms.sortingPhase.startPhase4()
        }
    }
    fun sortingP5()
    {
        if (_cms.gateStatus.isOpened()) sortingP6()
        else
        {
            cells.hwSortingM.hwMotors.openGate()
            logM.logMd("Sorting P5, opening gate", Debug.LOGIC)
            _cms.sortingPhase.startPhase5()
        }
    }
    fun sortingP6()
    {
        if (_cms.pushStatus.isOpened()) sortingP7()
        else
        {
            cells.hwSortingM.hwMotors.openPush()
            logM.logMd("Sorting P6, opening push", Debug.LOGIC)
            _cms.sortingPhase.startPhase6()
        }
    }
    fun sortingP7()
    {
        if (DelayMS.REALIGNMENT.WAITING_IN_SORTING_PASE_7 <= 0) return sortingP8()

        cells.hwSortingM.lastUpdateTimestampMS =
            _cms.collector.eventBus.invoke(
                SMC_GetCurrentGameTimerEvent()).timeMs

        logM.logMd("Sorting P7, waiting for ball to fly in MOBILE position", Debug.LOGIC)
        _cms.sortingPhase.startPhase7()
    }
    fun sortingP8()
    {
        logM.logMd("Sorting P8, closing " +
            if (_cms.sortingPhase.remainingRotations < 2)
                 cells.hwSortingM.hwMotors.closeGateWithPush()
            else cells.hwSortingM.hwMotors.closePush() +
        " push (Rotations: ${_cms.sortingPhase.remainingRotations})", Debug.LOGIC)

        _cms.sortingPhase.startPhase8()
        cells.rotateSwStorage()
        cells.hwSortingM.hwMotors.forwardBelts(onTime = false, 9.0)

        if (_cms.pushStatus.isClosed() && (
              _cms.gateStatus.isClosed() ||
              _cms.sortingPhase.remainingRotations > 1))
            sortingPhaseRealignment(
                ifDoneSkipToPhase3 = false,
                DelayMS.PUSH.PART, 9.0)
    }
}