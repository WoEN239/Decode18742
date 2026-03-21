package org.woen.scoringSystem


import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.collector.Collector
import org.woen.collector.RunMode
import org.woen.scoringSystem.storage.Storage

import org.woen.enumerators.Shooting
import org.woen.enumerators.RequestResult
import org.woen.enumerators.phases.ShootingPhase

import org.woen.configs.Delay
import org.woen.configs.DebugSettings
import org.woen.configs.RobotSettings.CONTROLS
import org.woen.configs.RobotSettings.TELEOP
import org.woen.configs.RobotSettings.AUTONOMOUS
import org.woen.enumerators.phases.MotorStatus
import org.woen.enumerators.phases.SortingPhase


class ScoringModulesConnector
{
    private val _cms: ConnectorModuleStatus

    private var _storage: Storage
    var logM: LogManager

    private val _gameTimer = ElapsedTime()
    private var _enteredShootingZoneTimeStamp: Double = 0.0
    private val _inShootingZone = false



    constructor(collector: Collector)
    {
        _cms = ConnectorModuleStatus(collector)
        _storage = Storage(_cms)

        logM = LogManager(collector.telemetry, DebugSettings.SMC)


        subscribeToOdometry()
        subscribeToCameraPattern()
        subscribeToSecondDriverPatternRecalibration()


        collector.startEvent  += {
            _gameTimer.reset()
        }
        collector.updateEvent += {
            update()
        }
    }

    private fun subscribeToSecondDriverPatternRecalibration()
    {
        if (CONTROLS.USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION)
        {
//            _cms.collector.eventBus.invoke(
//                AddGamepad2ListenerEvent(
//                    ClickGamepadListener(
//                        { it.ps },
//                        {
//                            _cms.dynamicMemoryPattern.resetTemporary()
//                        }
//            )   )   )
//            _cms.collector.eventBus.invoke(
//                AddGamepad2ListenerEvent(
//                    ClickGamepadListener(
//                        { it.left_bumper },
//                        {
//                            _cms.dynamicMemoryPattern.addToTemporary()
//                        }
//            )   )   )
//            _cms.collector.eventBus.invoke(
//                AddGamepad2ListenerEvent(
//                    ClickGamepadListener(
//                        { it.right_bumper },
//                        {
//                            _cms.dynamicMemoryPattern.removeFromTemporary()
//                        }
//            )   )   )
//
//
//            _cms.collector.eventBus.invoke(
//                AddGamepad2ListenerEvent(
//                    ClickGamepadListener(
//                        { it.dpad_left },
//                        {
//                            _cms.dynamicMemoryPattern.setPermanent(
//                                Shooting.StockPattern.Request.GPP)
//                            _cms.awaitingPatternFromCamera = false
//                        }
//            )   )   )
//            _cms.collector.eventBus.invoke(
//                AddGamepad2ListenerEvent(
//                    ClickGamepadListener(
//                        { it.dpad_up },
//                        {
//                            _cms.dynamicMemoryPattern.setPermanent(
//                                Shooting.StockPattern.Request.PGP)
//                            _cms.awaitingPatternFromCamera = false
//                        }
//            )   )   )
//            _cms.collector.eventBus.invoke(
//                AddGamepad2ListenerEvent(
//                    ClickGamepadListener(
//                        { it.dpad_right },
//                        {
//                            _cms.dynamicMemoryPattern.setPermanent(
//                                Shooting.StockPattern.Request.PPG)
//                            _cms.awaitingPatternFromCamera = false
//                        }
//            )   )   )

            logM.logMd("Init settings: USE SECOND DRIVER", Debug.GAMEPAD)
        }
        else logM.logMd("Init settings: DON'T use second driver", Debug.GAMEPAD)
    }
    private fun subscribeToCameraPattern()
    {
//        _cms.collector.eventBus.subscribe()
    }
    private fun subscribeToOdometry()
    {
//        _cms.collector.eventBus.subscribe()
//        _inShootingZone = true
//        _enteredShootingZoneTimeStamp = _gameTimer.milliseconds()


//        _cms.collector.eventBus.subscribe()
//        _inShootingZone = false
    }



    fun update()
    {
        _storage.cells.hwSortingM.update()

        _storage.cells.tryHandleIntake()

        updateSorting()
        updateShooting()
        updateCalibrationPhase()
    }


    fun updateShooting()
    {
        when (_cms.shootingPhase.name)
        {
            ShootingPhase.Name.NOT_ACTIVE ->
                if (canStartAutoShooting())
                {
                    if (!isEndGame) logM.logMd(
                        _storage.streamDrumPhase1()
                            .toString(), Debug.START)
                    else logM.logMd(
                        autoShootCustomisablePattern(
                        _cms.collector.runMode == RunMode.AUTO)
                            .toString(), Debug.START)
                }

            ShootingPhase.Name.P0_AWAITING_SORTING ->
                if (canStartAutoShooting())
                    _cms.shootingPhase.switchToNextPhase()

            ShootingPhase.Name.P1_OPENING_TURRET_GATE ->
                if (_cms.turretGateStatus.isFinished())
                    _storage.streamDrumPhase2()

            ShootingPhase.Name.P2_SHOOTING ->
            {
                if (_storage.cells.hwSortingM.wasShotFired())
                    _storage.cells.updateAfterShot()
                if (_storage.cells.hwSortingM.isReadyForShootingPhase3())
                    _storage.cells.hwSortingM.streamDrumPhase3()
            }

            ShootingPhase.Name.P3_OPENING_LAUNCHER ->
                if (_storage.cells.hwSortingM.isReadyForShootingPhase4())
                    _cms.shootingPhase.switchToNextPhase()

            ShootingPhase.Name.P4_CALIBRATING ->
                if (canStartCalibrationAfterShooting())
                    _storage.streamDrumPhase4()
        }
    }
    fun updateSorting()
    {
        when (_cms.sortingPhase.name)
        {
            SortingPhase.Name.P1_CLOSING_TURRET_GATE ->
               if (_cms.turretGateStatus.isClosed())
                   _storage.sortingPhaseRealignment()

            SortingPhase.Name.P2_REALIGN_STORAGE ->
                if (_cms.beltsStatus == MotorStatus.IDLE)
                    _storage.sortingPhase3()

            SortingPhase.Name.P3_REALIGNING_UPWARDS ->
                if (_cms.beltsStatus == MotorStatus.IDLE)
                    _storage.sortingPhase4()

            SortingPhase.Name.P4_REALIGNING_DOWNWARDS ->
                if (_cms.beltsStatus == MotorStatus.IDLE)
                    _storage.sortingPhase5()

            SortingPhase.Name.P5_OPENING_GATE ->
                if (_cms.gateStatus.isOpened())
                    _storage.sortingPhase6()

            SortingPhase.Name.P6_OPENING_PUSH ->
                if (_cms.pushStatus.isOpened())
                    _storage.sortingPhase7()

            SortingPhase.Name.P7_CLOSING_GATE_AND_PUSH ->
                if (_cms.gateStatus.isClosed() &&
                    _cms.pushStatus.isClosed())
                    _storage.sortingPhaseRealignment()

            SortingPhase.Name.P8_REALIGN_STORAGE ->
                if (_cms.beltsStatus == MotorStatus.IDLE)
                {
                    _cms.sortingPhase.remainingRotations--
                    if  (_cms.sortingPhase.remainingRotations > 0)
                         _cms.sortingPhase.startPhase1()
                    else
                    {
                        _cms.sortingPhase.setInactive()
                        if (canStartAutoShooting())
                            _cms.shootingPhase.switchToNextPhase()
                    }
                }

            SortingPhase.Name.NOT_ACTIVE -> { }
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
            _cms.beltsStatus == MotorStatus.IDLE)
            _storage.finishCalibration()


        if (_cms.calibrationPhase.isInactive() &&
            _cms.shootingPhase.isShootingPhase4())
            _cms.shootingPhase.setInactive()
    }



    fun canStartAutoShooting() = _inShootingZone &&
            _gameTimer.milliseconds() - _enteredShootingZoneTimeStamp >
                Delay.MS.SHOOTING.BEFORE_AUTOSHOT &&
            _cms.sortingPhase.isInactive() &&
            _cms.calibrationPhase.isInactive()
    fun autoShootCustomisablePattern(isAuto: Boolean): RequestResult.Name
    {
        return if (isAuto)
        {
            if (_cms.dynamicMemoryPattern.permanentWasDetected())
                _storage.startCustomisableDrumRequest(
                    AUTONOMOUS.PATTERN_SHOOTING_MODE,
                    _cms.dynamicMemoryPattern.permanent(),
                    AUTONOMOUS.AUTOCORRECT_REQUEST_PATTERN)
            else _storage.startCustomisableDrumRequest(
                AUTONOMOUS.FAILSAFE_SHOOTING_MODE,
                AUTONOMOUS.FAILSAFE_PATTERN,
                AUTONOMOUS.AUTOCORRECT_FAILSAFE_PATTERN)
        }
        else _storage.startCustomisableDrumRequest(
                TELEOP.PATTERN_SHOOTING_MODE,
                _cms.dynamicMemoryPattern.permanent(),
                Shooting.StockPattern.Request.STREAM,
                TELEOP.AUTOCORRECT_REQUEST_PATTERN,
                TELEOP.AUTOCORRECT_FAILSAFE_PATTERN)
    }

    fun canStartCalibrationAfterShooting()
        =   _cms.calibrationPhase.isInactive() &&
            _cms.sortingPhase.isInactive() &&
            _storage.cells.hwSortingM.isHardwareIdle()



    val isEndGame get() = _gameTimer.seconds() > 90.0
}