package org.woen.scoringSystem


import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.collector.Collector
import org.woen.scoringSystem.storage.Storage

import org.woen.modules.ClickGamepadListener
import org.woen.modules.AddGamepadListenerEvent

import org.woen.enumerators.Shooting
import org.woen.configs.DebugSettings
import org.woen.configs.RobotSettings.CONTROLS



class ScoringModulesConnector
{
    private val _cms: ConnectorModuleStatus

    private var _storage: Storage
    var logM: LogManager

    private val _gameTimer = ElapsedTime()



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

    }
}