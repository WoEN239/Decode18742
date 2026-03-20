package org.woen.scoringSystem


import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.collector.Collector
import org.woen.collector.RunMode
import org.woen.scoringSystem.storage.Storage

import org.woen.enumerators.ShootingPhase

import org.woen.configs.Delay
import org.woen.configs.DebugSettings
import org.woen.configs.RobotSettings
import org.woen.configs.RobotSettings.CONTROLS
import org.woen.configs.RobotSettings.TELEOP
import org.woen.configs.RobotSettings.AUTONOMOUS
import org.woen.enumerators.RequestResult
import org.woen.enumerators.Shooting


class ScoringModulesConnector
{
    private val _cms: ConnectorModuleStatus

    private var _storage: Storage
    var logM: LogManager

    private val _gameTimer = ElapsedTime()
    private val _enteredShootingZoneTimeStamp: Long = 0
    private val _inShootingZone = false



    constructor(collector: Collector)
    {
        _cms = ConnectorModuleStatus(collector)
        _storage = Storage(_cms)

        logM = LogManager(collector.telemetry, DebugSettings.SMC)


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



    fun update()
    {
        _storage.cells.hwSortingM.update()

        _storage.cells.tryHandleIntake()

        updateShooting()
    }


    fun updateShooting()
    {
        when (_cms.shootingPhase.name)
        {
            ShootingPhase.Name.NOT_ACTIVE ->
                if (canStartAutoShooting())
                    if (!isEndGame) _storage.streamDrumPhase1()
                    else autoShootCustomisablePattern(
                        _cms.collector.runMode == RunMode.AUTO)

            ShootingPhase.Name.P1_OPENING_TURRET_GATE ->
                if (_cms.turretGateStatus.isFinished())
                    _storage.streamDrumPhase2()

            ShootingPhase.Name.P2_SHOOTING ->
                if (_storage.cells.hwSortingM.isReadyForShootingPhase3())
                    _storage.cells.hwSortingM.streamDrumPhase3()

            else -> { }
        }
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



    val isEndGame get() = _gameTimer.seconds() > 90.0
}