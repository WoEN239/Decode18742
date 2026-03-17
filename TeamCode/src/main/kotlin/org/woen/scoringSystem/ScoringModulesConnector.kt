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
    private val _collector: Collector
    private val _cms: ConnectorModuleStatus
    private val _storage: Storage
    val logM: LogManager

    private val _gameTimer = ElapsedTime()



    constructor(collector: Collector)
    {
        _collector = collector
        _cms = ConnectorModuleStatus(collector)

        _storage = Storage(_cms)

        logM = LogManager(_collector, DebugSettings.SMC)

//        subscribeToEvents()
//        subscribeToGamepad()
//        subscribeToGamepadTests()
//        subscribeToSecondDriverPatternRecalibration

        collector.startEvent += {

        }
    }


//    private fun subscribeToGamepadTests()
//    {
//        GamepadLI.addGamepad1Listener(
//            createClickDownListener(
//                { it.touchpadWasPressed() }, {
//
//                    logM.logMd("SSM: Touchpad start 100 rotation test", Debug.START)
//                    _runStatus.addProcessToQueue(ProcessId.SORTING_TESTING)
//
//                    SmartCoroutineLI.launch {
//                        _storage.unsafeTestSorting()
//                    }
//
//                    _runStatus.removeProcessFromQueue(ProcessId.SORTING_TESTING)
//                }
//        )   )
//    }


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


    fun relink()
    {
        _storage.relink()
        _storage.cells.relink()
        _storage.cells.hwSortingM.relink()

        logM.relink(DebugSettings.SMC)
        _gameTimer.reset()
    }


}