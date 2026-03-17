package org.woen.scoringSystem


import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.collector.Collector
import org.woen.scoringSystem.storage.Storage

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



        collector.startEvent  += {

        }
        collector.updateEvent += {
            update()
        }
    }


    private fun subscribeToSecondDriverPatternRecalibration()
    {
        if (CONTROLS.USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION)
        {
//            GamepadLI.addGamepad2Listener(
//                createClickDownListener({ it.triangle }, {
//
//                        _storageLogic.dynamicMemoryPattern.resetTemporary()
//            }   )   )
//
//            GamepadLI.addGamepad2Listener(
//                createClickDownListener({ it.square }, {
//
//                        _storageLogic.dynamicMemoryPattern.addToTemporary()
//            }   )   )
//
//            GamepadLI.addGamepad2Listener(
//                createClickDownListener({ it.circle }, {
//
//                        _storageLogic.dynamicMemoryPattern.removeFromTemporary()
//            }   )   )
//
//
//
//            GamepadLI.addGamepad2Listener(
//                createClickDownListener({ it.dpad_left }, {
//
//                        _storageLogic.dynamicMemoryPattern.setPermanent(
//                            Shooting.StockPattern.Sequence.Request.GPP)
//            }   )   )
//
//            GamepadLI.addGamepad2Listener(
//                createClickDownListener({ it.dpad_up }, {
//
//                        _storageLogic.dynamicMemoryPattern.setPermanent(
//                            Shooting.StockPattern.Sequence.Request.PGP)
//            }   )   )
//
//            GamepadLI.addGamepad2Listener(
//                createClickDownListener({ it.dpad_right }, {
//
//                        _storageLogic.dynamicMemoryPattern.setPermanent(
//                            Shooting.StockPattern.Sequence.Request.PPG)
//            }   )   )


            logM.logMd("Init settings: USE SECOND DRIVER", Debug.GAMEPAD)
        }
        else logM.logMd("Init settings: DON'T use second driver", Debug.GAMEPAD)
    }


    fun update()
    {

    }
}