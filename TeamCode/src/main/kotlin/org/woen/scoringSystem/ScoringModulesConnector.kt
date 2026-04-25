package org.woen.scoringSystem


import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager
import org.woen.utils.exponentialFilter.ExponentialFilter

import org.woen.collector.Collector
import org.woen.collector.RunMode
import org.woen.scoringSystem.storage.Storage
import org.woen.scoringSystem.misc.DynamicPattern

import org.woen.enumerators.Ball
import org.woen.enumerators.StockPattern
import org.woen.enumerators.RequestResult
import org.woen.enumerators.phases.SortingPhase
import org.woen.enumerators.phases.ShootingPhase
import org.woen.enumerators.phases.CalibrationPhase

import org.woen.modules.ClickGamepadListener
import org.woen.modules.AddGamepad1ListenerEvent
import org.woen.modules.AddGamepad2ListenerEvent

import org.woen.modules.OnPatternDetectedEvent
import org.woen.modules.drivetrain.RobotExitShootingAreaEvent
import org.woen.modules.drivetrain.RobotEnterShootingAreaEvent

import org.woen.configs.Delay
import org.woen.configs.Hardware
import org.woen.configs.DebugSettings
import org.woen.configs.RobotSettings.CONTROLS
import org.woen.configs.RobotSettings.TELEOP
import org.woen.configs.RobotSettings.AUTONOMOUS
import org.woen.scoringSystem.storage.MAX_BALL_COUNT


data class SMC_IsEndGameEvent(var isEndGame: Boolean = false)
data class SMC_GetCurrentGameTimerEvent(var timeMs: Double = 0.0)


enum class LazyIntakeTask
{
    STOP,
    START,
    SWITCH
}

data class SMC_ForceSetStorage(
    var stockPattern: Array<Ball> = arrayOf(),
    var nameSequence: Array<Ball.Name> = arrayOf())
data class SMC_TryUpdateLazyIntakeEvent(
    var intakeTask: LazyIntakeTask,
    var startingResult: Boolean = false)
data class SMC_TryStartSortingEvent(
    var swapCount: Int,
    var startingResult: Boolean = false)
data class SMC_TryStartCustomisableShootingEvent(
    var isAuto: Boolean,
    var shootAfterSorting: Boolean = true,
    var startingResult: Boolean = false)
data class SMC_TryStartShootingEvent(
    var startingResult: Boolean = false)


data class SMC_ShootingStatus(
    var isFinished: Boolean = false,
    var phase: ShootingPhase = ShootingPhase())
data class SMC_SortingStatus(
    var isFinished: Boolean = false,
    var phase: SortingPhase = SortingPhase())
data class SMC_CalibrationStatus(
    var isFinished: Boolean = false,
    var phase: CalibrationPhase = CalibrationPhase())



class ScoringModulesConnector
{
    private val _cms: ConnectorModuleStatus

    private var _storage: Storage
    var logM: LogManager

    private val _gameTimer = ElapsedTime()
    private var _inShootingZone = false
    private var _enteredShootingZoneTimeStamp: Double = 0.0

    private var _beltR: Double = 10.0
    private val _rFilter = ExponentialFilter(
        org.woen.modules.SIMPLE_STORAGE_CONFIG.R_FILTER_K)



    constructor(collector: Collector)
    {
        _cms = ConnectorModuleStatus(collector)
        _storage = Storage(_cms)

        logM = LogManager(collector.telemetry, DebugSettings.SMC)


        subscribeToOdometry()
        subscribeToCameraPattern()

        subscribeToIsEndGameEvent()
        subscribeToOutsideControlEvents()
        subscribeToGiveStatusInfoEvents()

        subscribeToDriverShootingGamepad1()
        subscribeToDriverMiscellaneousGamepad1()
        subscribeToHelperGamepad2PatternRecalibration()


        collector.startEvent  += {
            _gameTimer.reset()
            _rFilter.start()
        }
        collector.updateEvent += {
            update()
        }
    }



    private fun subscribeToOdometry()
    {
        _cms.collector.eventBus.subscribe(RobotEnterShootingAreaEvent::class)
        {
            _inShootingZone = true
            _enteredShootingZoneTimeStamp = _gameTimer.milliseconds()
        }

        _cms.collector.eventBus.subscribe(RobotExitShootingAreaEvent::class)
        {
            _inShootingZone = false
        }
    }
    private fun subscribeToCameraPattern()
    {
        _cms.collector.eventBus.subscribe(OnPatternDetectedEvent::class)
        {
            if (_cms.awaitingPatternFromCamera)
            {
                val patternString = it.pattern.joinToString(", ")

                logM.logMd("(CAMERA) Storage received pattern: $patternString", Debug.STATUS)
                _cms.dynamicMemoryPattern.setPermanent(it.pattern)
            }
        }
    }
    private fun subscribeToIsEndGameEvent()
    {
        _cms.collector.eventBus.subscribe(SMC_IsEndGameEvent::class)
        {
            it.isEndGame = isEndGame
        }
        _cms.collector.eventBus.subscribe(SMC_GetCurrentGameTimerEvent::class)
        {
            it.timeMs = _gameTimer.milliseconds()
        }
    }
    private fun subscribeToOutsideControlEvents()
    {
        _cms.collector.eventBus.subscribe(SMC_ForceSetStorage::class)
        {
            if (it.stockPattern.isNotEmpty())
                _storage.cells.forceSet(it.stockPattern)
            else if (it.nameSequence.isNotEmpty())
                _storage.cells.forceSet(it.nameSequence)
        }
        _cms.collector.eventBus.subscribe(SMC_TryUpdateLazyIntakeEvent::class)
        {
            it.startingResult = tryUpdateLazyIntake(it.intakeTask)
        }
        _cms.collector.eventBus.subscribe(SMC_TryStartSortingEvent::class)
        {
            it.startingResult = tryStartSorting(it.swapCount)
        }
        _cms.collector.eventBus.subscribe(SMC_TryStartShootingEvent::class)
        {
            it.startingResult = tryStartShooting(laterGamepadHold = false)
        }
        _cms.collector.eventBus.subscribe(SMC_TryStartCustomisableShootingEvent::class)
        {
            it.startingResult = isFullyIdle()
            logM.logMd("Try start CustomisableShooting: ${it.startingResult}")
            if (it.startingResult) autoShootCustomisablePattern(it.isAuto, it.shootAfterSorting)
            else logStartingError("CustomisableShooting")
        }
    }
    private fun subscribeToGiveStatusInfoEvents()
    {
        _cms.collector.eventBus.subscribe(SMC_SortingStatus::class)
        {
            it.phase = SortingPhase(_cms.sortingPhase)
            it.isFinished = _cms.sortingPhase.isInactive()
        }
        _cms.collector.eventBus.subscribe(SMC_ShootingStatus::class)
        {
            it.phase = ShootingPhase(_cms.shootingPhase)
            it.isFinished = _cms.shootingPhase.isNotShooting()
        }
        _cms.collector.eventBus.subscribe(SMC_CalibrationStatus::class)
        {
            it.phase = CalibrationPhase(_cms.calibrationPhase)
            it.isFinished = _cms.calibrationPhase.isInactive()
        }
    }
    private fun subscribeToDriverShootingGamepad1()
    {
        if (!CONTROLS.HOLD_FOR_MANUAL_SHOOTING)
        {
            _cms.collector.eventBus.invoke(
                AddGamepad1ListenerEvent(
                    ClickGamepadListener(
                        buttonSuppler = { it.right_bumper },
                        activationState = true,
                        onTriggered = {
                            tryStartShooting(laterGamepadHold = false)
                        }
            )   )   )

            logM.logMd("Init settings: USE PRESSING for regular shooting", Debug.GAMEPAD)
        }
        else
        {
            _cms.collector.eventBus.invoke(
                AddGamepad1ListenerEvent(
                    ClickGamepadListener(
                        buttonSuppler = { it.right_bumper },
                        activationState = true,
                        onTriggered = {
                            tryStartShooting(laterGamepadHold = true)
                        }
            )   )   )

            _cms.collector.eventBus.invoke(
                AddGamepad1ListenerEvent(
                    ClickGamepadListener(
                        buttonSuppler = { it.right_bumper },
                        activationState = false,
                        onTriggered = {
                            if (_cms.sortingPhase.isInactive())
                            {
                                if (_cms.shootingPhase.isGamepadHoldPhase2() &&
                                    CONTROLS.USE_LAUNCHER_AFTER_GAMEPAD_HOLD_SHOOT)
                                    _storage.cells.hwSortingM.streamDrumPhase3()
                                else _cms.shootingPhase.startPhase4()
                        }   }
            )   )   )

            logM.logMd("Init settings: USE HOLDING for regular shooting", Debug.GAMEPAD)
        }



        if (CONTROLS.ENABLE_GAMEPAD_CUSTOMISABLE_SHOOTING)
        {
            _cms.collector.eventBus.invoke(
                AddGamepad1ListenerEvent(
                    ClickGamepadListener(
                        buttonSuppler = { it.triangle },
                        activationState = true,
                        onTriggered = {
                            if (_cms.shootingPhase.isInactive()
                                && isFullyIdle())
                            {
                                val requestResult = autoShootCustomisablePattern(false)
                                val resultString  =
                                    if (requestResult == RequestResult.ROGER_STARTING_SORTING)
                                        requestResult.toString() +
                                                ", rotations: " +
                                                _cms.sortingPhase.remainingRotations
                                    else requestResult.toString()

                                logM.logMd(resultString, Debug.START)
                            }
                            else logStartingError("Manual Customisable shooting")
                        }
            )   )   )
        }
        else logM.logMd("Init settings: DISABLE Manual Customisable shooting", Debug.GAMEPAD)
    }
    private fun subscribeToDriverMiscellaneousGamepad1()
    {
        if (CONTROLS.ENABLE_GAMEPAD_CONTROLLED_LAZY_INTAKE)
        {
            if (!CONTROLS.HOLD_FOR_LAZY_INTAKE)
            {
                _cms.collector.eventBus.invoke(
                    AddGamepad1ListenerEvent(
                        ClickGamepadListener(
                            { it.left_bumper },
                            {
                                tryUpdateLazyIntake(LazyIntakeTask.SWITCH)
                            }
                )   )   )
            }
            else
            {
                _cms.collector.eventBus.invoke(
                    AddGamepad1ListenerEvent(
                        ClickGamepadListener(
                            buttonSuppler = { it.left_bumper },
                            activationState = true,
                            onTriggered = {
                                tryUpdateLazyIntake(LazyIntakeTask.START)
                            }
                )   )   )

                _cms.collector.eventBus.invoke(
                    AddGamepad1ListenerEvent(
                        ClickGamepadListener(
                            buttonSuppler = { it.left_bumper },
                            activationState = false,
                            onTriggered = {
                                tryUpdateLazyIntake(LazyIntakeTask.STOP)
                            }
                )   )   )
            }

            logM.logMd("Init settings: ENABLE Manual LazyIntake," +
                    " HOLD: ${CONTROLS.HOLD_FOR_LAZY_INTAKE}", Debug.GAMEPAD)
        }
        else logM.logMd("Init settings: DISABLE LazyIntake", Debug.GAMEPAD)


        if (CONTROLS.ENABLE_GAMEPAD_CONTROLLED_COLOR_INTAKE)
        {
            _cms.collector.eventBus.invoke(
                AddGamepad1ListenerEvent(
                    ClickGamepadListener(
                        { it.square },
                        {
                            logM.logMd("Gamepad - GREEN intake", Debug.GAMEPAD)
                            _storage.cells.hwSortingM.startBrushTime(
                                forward = true, Delay.MS.BRUSH_FORWARD)
                            _storage.cells.handleIntake(Ball.Name.GREEN)
                        }
            )   )   )
            _cms.collector.eventBus.invoke(
                AddGamepad1ListenerEvent(
                    ClickGamepadListener(
                        { it.circle },
                        {
                            logM.logMd("Gamepad - PURPLE intake", Debug.GAMEPAD)
                            _storage.cells.hwSortingM.startBrushTime(
                                forward = true, Delay.MS.BRUSH_FORWARD)
                            _storage.cells.handleIntake(Ball.Name.PURPLE)
                        }
            )   )   )

            logM.logMd("Init settings: ENABLE Manual color sensors imitation", Debug.GAMEPAD)
        }
        else logM.logMd("Init settings: DISABLE Manual color sensors imitation", Debug.GAMEPAD)


        if (CONTROLS.ENABLE_GAMEPAD_CONTROLLED_SORTING_SWAPS)
        {
            _cms.collector.eventBus.invoke(
                AddGamepad1ListenerEvent(
                    ClickGamepadListener(
                        { it.right_trigger > 0.5 },
                        {
                            tryStartSorting(CONTROLS.SWAPS_PER_MANUAL_BUTTON_SWITCH)
                        }
            )   )   )

            logM.logMd("Init settings: ENABLE Manual Sorting swaps", Debug.GAMEPAD)
        }
        else logM.logMd("Init settings: DISABLE Manual sorting swaps", Debug.GAMEPAD)
    }
    private fun subscribeToHelperGamepad2PatternRecalibration()
    {
        if (CONTROLS.USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION)
        {
            _cms.collector.eventBus.invoke(
                AddGamepad2ListenerEvent(
                    ClickGamepadListener(
                        { it.ps },
                        {
                            logM.logMd("(GP2) Reset temporary pattern", Debug.STATUS)
                            _cms.dynamicMemoryPattern.resetOffset()
                            _cms.collector.opMode.gamepad2.rumble(100)
                        }
            )   )   )
            _cms.collector.eventBus.invoke(
                AddGamepad2ListenerEvent(
                    ClickGamepadListener(
                        { it.right_bumper },
                        {
                            _cms.dynamicMemoryPattern.addToOffset()
                            logM.logMd("(GP2) + Added 1 to temporary, curCount: " +
                                "${_cms.dynamicMemoryPattern.offset()}", Debug.STATUS)

                            val pattern = DynamicPattern.trimPattern(
                                _cms.dynamicMemoryPattern.permanent(),
                                _cms.dynamicMemoryPattern.offset())
                                .joinToString(", ")
                            logM.logMd("CurAutoPattern: $pattern", Debug.STATUS)
                        }
            )   )   )
            _cms.collector.eventBus.invoke(
                AddGamepad2ListenerEvent(
                    ClickGamepadListener(
                        { it.left_bumper },
                        {
                            _cms.dynamicMemoryPattern.removeFromOffset()
                            logM.logMd("(GP2) - Removed 1 from temporary, curCount: " +
                                "${_cms.dynamicMemoryPattern.offset()}", Debug.STATUS)

                            val pattern = DynamicPattern.trimPattern(
                                _cms.dynamicMemoryPattern.permanent(),
                                _cms.dynamicMemoryPattern.offset())
                                .joinToString(", ")
                            logM.logMd("CurAutoPattern: $pattern", Debug.STATUS)
                        }
            )   )   )


            _cms.collector.eventBus.invoke(
                AddGamepad2ListenerEvent(
                    ClickGamepadListener(
                        { it.dpad_left },
                        {
                            logM.logMd("(GP2) Set PERMANENT: GPP", Debug.STATUS)
                            _cms.dynamicMemoryPattern.setPermanent(
                                StockPattern.Request.GPP)
                            _cms.awaitingPatternFromCamera = false
                        }
            )   )   )
            _cms.collector.eventBus.invoke(
                AddGamepad2ListenerEvent(
                    ClickGamepadListener(
                        { it.dpad_up },
                        {
                            logM.logMd("(GP2) Set PERMANENT: PGP", Debug.STATUS)
                            _cms.dynamicMemoryPattern.setPermanent(
                                StockPattern.Request.PGP)
                            _cms.awaitingPatternFromCamera = false
                        }
            )   )   )
            _cms.collector.eventBus.invoke(
                AddGamepad2ListenerEvent(
                    ClickGamepadListener(
                        { it.dpad_right },
                        {
                            logM.logMd("(GP2) Set PERMANENT: PPG", Debug.STATUS)
                            _cms.dynamicMemoryPattern.setPermanent(
                                StockPattern.Request.PPG)
                            _cms.awaitingPatternFromCamera = false
                        }
            )   )   )

            logM.logMd("Init settings: USE Second driver", Debug.GAMEPAD)
        }
        else logM.logMd("Init settings: DON'T use second driver", Debug.GAMEPAD)
    }



    fun update()
    {
        tryUpdateBallCountOnBeltsCurrent()
        //  Comment if you don't use this for better performance

        _storage.tryHandleIntake()

        updateSorting()
        updateShooting()
        updateCalibrationPhase()

        _storage.cells.hwSortingM.update()
    }


    fun updateSorting()
    {
        when (_cms.sortingPhase.name)
        {
            SortingPhase.Name.P1_CLOSING_TURRET_GATE ->
                if (_cms.turretGateStatus.isClosed())
                    _storage.sortingPhaseRealignment(
                        ifDoneSkipToPhase3 = true)

            SortingPhase.Name.P2_REALIGN_STORAGE ->
                if (_cms.beltsStatus.notOnTime())
                    _storage.sortingPhase3()

            SortingPhase.Name.P3_REALIGNING_UPWARDS ->
                if (_cms.beltsStatus.notOnTime())
                    _storage.sortingPhase4()

            SortingPhase.Name.P4_REALIGNING_DOWNWARDS ->
                if (_cms.beltsStatus.isIdle())
                    _storage.sortingPhase5()

            SortingPhase.Name.P5_OPENING_GATE ->
                if (_cms.gateStatus.isOpened())
                    _storage.sortingPhase6()

            SortingPhase.Name.P6_OPENING_PUSH ->
                if (_cms.pushStatus.isOpened())
                    _storage.sortingPhase7()

            SortingPhase.Name.P7_WAIT_SOME_TIME ->
                if (Delay.MS.REALIGNMENT.WAITING_IN_SORTING_PASE_7 <= 0
                    || (_gameTimer.milliseconds() -
                        Delay.MS.REALIGNMENT.WAITING_IN_SORTING_PASE_7 >
                        _storage.cells.hwSortingM.timeSinceLastShotUpdateMs))
                    _storage.sortingPhase8()

            SortingPhase.Name.P8_CLOSING_GATE_AND_PUSH ->
                if (_cms.pushStatus.isClosed())
                {
                    _storage.cells.hwSortingM.hwMotors.forwardBelts(
                        onTime = false, 11.0)

                    if (_cms.gateStatus.isClosed() ||
                        _cms.sortingPhase.remainingRotations > 1)
                            _storage.sortingPhaseRealignment(
                                ifDoneSkipToPhase3 = false)
                }

            SortingPhase.Name.P9_REALIGN_STORAGE ->
                if (_cms.beltsStatus.isIdle()
                    || (_cms.sortingPhase.remainingRotations > 1 &&
                        _gameTimer.milliseconds() >
                        _storage.cells.hwSortingM.timeSinceLastShotUpdateMs))
                {
                    _cms.sortingPhase.remainingRotations--
                    if  (_cms.sortingPhase.remainingRotations > 0)
                        _storage.sortingPhase1(
                            _cms.sortingPhase.remainingRotations)
                    else
                    {
                        _cms.sortingPhase.setInactive()
                        _storage.cells.hwSortingM.hwMotors.stopBelts()

//                        if (canStartAutoShooting())
                        if (isFullyIdle() &&
                            CONTROLS.USE_AUTO_SHOOTING_WHEN_IN_ZONE)
                        {
                            _cms.shootingPhase.setInactive()
                            logM.logMd(
                                _storage.streamDrumPhase1()
                                    .toString(), Debug.START)
                        }
                        else _cms.shootingPhase.setInactive()
                    }
                }

            SortingPhase.Name.NOT_ACTIVE -> { }
        }
    }
    fun updateShooting()
    {
        when (_cms.shootingPhase.name)
        {
            ShootingPhase.Name.NOT_ACTIVE ->
                if (canStartAutoShooting())
                {
                    if (!(CONTROLS.DISABLE_AUTO_SHOOTING_IN_END_GAME
                        && isEndGame)) logM.logMd(
                        _storage.streamDrumPhase1()
                            .toString(), Debug.START)
                    else
                    {
                        val requestResult = autoShootCustomisablePattern(
                            _cms.collector.runMode == RunMode.AUTO)
                        val resultString  =
                            if (requestResult == RequestResult.ROGER_STARTING_SORTING)
                                requestResult.toString() +
                                ", rotations: " +
                                _cms.sortingPhase.remainingRotations
                            else requestResult.toString()

                        logM.logMd(resultString, Debug.START)
                    }
                }

            ShootingPhase.Name.P0_AWAITING_SORTING ->
//                if (canStartAutoShooting())
                if (isFullyIdle())
                {
                    _cms.shootingPhase.setInactive()
                    logM.logMd(
                        _storage.streamDrumPhase1()
                            .toString(), Debug.START)
                }

            ShootingPhase.Name.P1_OPENING_TURRET_GATE,
            ShootingPhase.Name.P1_OPENING_TURRET_GATE_LATER_GAMEPAD_HOLD ->
                if (_cms.turretGateStatus.isFinished())
                    _storage.streamDrumPhase2()

            ShootingPhase.Name.P2_SHOOT_BELTS_ON_TIME,
                ShootingPhase.Name.P2_SHOOT_BELTS_ON_GAMEPAD_HOLD ->
            {
                if (_storage.cells.hwSortingM.wasShotFired())
                    _storage.cells.updateAfterShot()

                if (_cms.shootingPhase.isRegularPhase2() &&
                     CONTROLS.USE_LAUNCHER_FOR_LAST_BALL &&
                    _storage.cells.hwSortingM.isReadyForShootingPhase3())
                    _storage.cells.hwSortingM.streamDrumPhase3()

                if (_storage.cells.hwSortingM.isReadyForShootingPhase4())
                    _cms.shootingPhase.startPhase4()
            }

            ShootingPhase.Name.P3_OPENING_LAUNCHER ->
            {
                if (_storage.cells.hwSortingM.wasShotFired())
                    _storage.cells.updateAfterShot()

                if (_storage.cells.hwSortingM.isReadyForShootingPhase4())
                {
                    _storage.cells.hwSortingM.hwMotors.stopBelts()
                    _cms.shootingPhase.startPhase4()
                }
            }

            ShootingPhase.Name.P4_CALIBRATING ->
                if (canStartCalibrationAfterShooting())
                    _storage.streamDrumPhase4()
        }
    }
    fun updateCalibrationPhase()
    {
        if (_cms.calibrationPhase.isCalibrationPhase2() &&
            _storage.cells.hwSortingM.closedAllServos())
        {
            if (_storage.cells.isNotEmpty())
                _storage.cells.hwSortingM.calibrationPhase3()
            else _storage.finishCalibration()
        }
        if (_cms.calibrationPhase.isCalibrationPhase3() &&
            _cms.beltsStatus.notOnTime())
            _storage.finishCalibration()
    }


    fun tryUpdateBallCountOnBeltsCurrent()
    {
        if (CONTROLS.ENABLE_GAMEPAD_CONTROLLED_LAZY_INTAKE &&
            CONTROLS.ENABLE_BALL_COUNT_PREDICTION_IN_LAZY_INTAKE &&
            _cms.lazyIntakeIsActive && isFullyIdle() &&
            (!_cms.ballCountForLED.infoIsGuaranteed ||
              _cms.ballCountForLED.count == 0))
        {
            val current = _storage.cells.hwSortingM.hwMotors.getBeltsCurrent()

            if (current > 1.0)
            {
                val rawR = _cms.collector.battery.currentVoltage / current
                _beltR = _rFilter.updateRaw(_beltR, rawR - _beltR)
            }
            else
            {
                _rFilter.updateRaw(_beltR, 0.0)
                _beltR = 10.0
            }

            var ballCount = _cms.ballCountForLED.count
            val countBasedOnCurrent
               = if (_beltR < org.woen.modules.SIMPLE_STORAGE_CONFIG.FULL_R) 3
            else if (_beltR < org.woen.modules.SIMPLE_STORAGE_CONFIG.TWO_R)  2
            else if (_beltR < org.woen.modules.SIMPLE_STORAGE_CONFIG.ONE_R)  1
            else -1

            if (ballCount < countBasedOnCurrent) ballCount = countBasedOnCurrent

            if (ballCount > _cms.ballCountForLED.count)
            _storage.cells.updateBallCountForLEDLINE(
                ballCount, infoIsGuaranteed = false)
        }
    }



    private fun hardStartLazyIntake(intakeTaskName: String)
    {
        _cms.lazyIntakeIsActive = true
        logM.logMd("($intakeTaskName) Started LazyIntake", Debug.START)

        _storage.cells.hwSortingM.hwMotors.forwardBelts(onTime = false)
        _storage.cells.hwSortingM.hwMotors.forwardBrush(onTime = false)
    }
    private fun hardStopLazyIntake(intakeTaskName: String)
    {
        _cms.lazyIntakeIsActive = false
        logM.logMd("($intakeTaskName) Stopped LazyIntake", Debug.END)

        _storage.cells.hwSortingM.hwMotors.stopBelts()
        _storage.cells.hwSortingM.hwMotors.stopBrush()

        if (CONTROLS.REVERSE_BRUSHES_AFTER_LAZY_INTAKE)
            _storage.cells.hwSortingM.startBrushTime(
                forward = false, Delay.MS.BRUSH_REVERSE)
    }
    private fun tryUpdateLazyIntake(intakeTask: LazyIntakeTask): Boolean
    {
        val canManage = _cms.sortingPhase.isInactive() &&
            _cms.shootingPhase.isInactive() &&
            _cms.calibrationPhase.isInactive()


        if (canManage) when (intakeTask)
        {
            LazyIntakeTask.STOP   -> hardStopLazyIntake ("Stop")
            LazyIntakeTask.START  -> hardStartLazyIntake("Start")
            LazyIntakeTask.SWITCH ->
            {
                if (_cms.lazyIntakeIsActive)
                     hardStopLazyIntake ("Switch")
                else hardStartLazyIntake("Switch")
            }
        }
        else logStartingError("LazyIntake")

        return canManage
    }

    private fun tryStartSorting(swapCount: Int): Boolean
    {
        val canStart = _cms.sortingPhase.isInactive()  &&
                _cms.shootingPhase.isInactive() &&
                _cms.calibrationPhase.isInactive()

        if (canStart) _storage.sortingPhase1(swapCount)
        else logStartingError("Gamepad/ExtEvent SortingSwaps (${swapCount})")

        return canStart
    }
    private fun tryStartShooting(laterGamepadHold: Boolean = false): Boolean
    {
        val canStart = _cms.shootingPhase.isInactive() && isFullyIdle()

        if (canStart) logM.logMd(_storage.streamDrumPhase1(laterGamepadHold).toString(), Debug.START)
        else logStartingError("Gamepad/ExtEvent Shooting")

        return canStart
    }



    fun canStartAutoShooting()
        =   _inShootingZone &&
            CONTROLS.USE_AUTO_SHOOTING_WHEN_IN_ZONE &&
            _gameTimer.milliseconds() - _enteredShootingZoneTimeStamp >
                Delay.MS.SHOOTING.BEFORE_AUTOSHOT &&
            isFullyIdle()
    fun isFullyIdle()
        =   _cms.sortingPhase.isInactive() &&
            _cms.shootingPhase.isInactive() &&
            _cms.calibrationPhase.isInactive()
    fun autoShootCustomisablePattern(
        isAuto: Boolean,
        shootAfterSorting: Boolean = true): RequestResult
    {
        _cms.shootingPhase.shotBeltsVoltage = Hardware.MOTOR.BELTS_FOR_SLOW_SHOOTING

        return if (isAuto)
        {
            if (_cms.dynamicMemoryPattern.permanentWasDetected())
                _storage.startCustomisableDrumRequest(
                    AUTONOMOUS.PATTERN_SHOOTING_MODE,
                    _cms.dynamicMemoryPattern.permanent(),
                    shootAfterSorting,
                    AUTONOMOUS.AUTOCORRECT_REQUEST_PATTERN)
            else _storage.startCustomisableDrumRequest(
                AUTONOMOUS.FAILSAFE_SHOOTING_MODE,
                AUTONOMOUS.FAILSAFE_PATTERN,
                shootAfterSorting,
                AUTONOMOUS.AUTOCORRECT_FAILSAFE_PATTERN)
        }
        else _storage.startCustomisableDrumRequest(
                TELEOP.PATTERN_SHOOTING_MODE,
                _cms.dynamicMemoryPattern.permanent(),
                StockPattern.Request.STREAM,
                shootAfterSorting,
                TELEOP.AUTOCORRECT_REQUEST_PATTERN,
                TELEOP.AUTOCORRECT_FAILSAFE_PATTERN)
    }

    fun canStartCalibrationAfterShooting()
        =   _cms.calibrationPhase.isInactive() &&
            _cms.sortingPhase.isInactive() &&
            _storage.cells.hwSortingM.isHardwareIdle()



    fun logStartingError(processName: String)
    {
        logM.logMd("Unable to start $processName, " +
                "another process is unfinished", Debug.ERROR)
        logM.logMd("Shooting: ${_cms.shootingPhase.name}, " +
                "Sorting: ${_cms.sortingPhase.name}, " +
                "Calibration: ${_cms.calibrationPhase.name}", Debug.GENERIC)
    }

    val isEndGame get() = _gameTimer.seconds() > 90.0
}