package org.woen.scoringSystem


import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.Collector

import org.woen.enumerators.BallRequest

import org.woen.enumerators.Shooting

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.configs.RobotSettings.TELEOP
import org.woen.scoringSystem.storage.Storage
import java.util.concurrent.atomic.AtomicBoolean



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
        _cms = ConnectorModuleStatus()

        _storage = Storage(_collector, _cms)

        logM = LogManager(_collector, Debug.SMC)

//        subscribeToEvents()
//        subscribeToGamepad()
//        subscribeToGamepadTests()

        collector.startEvent += {

            _storage.reset()
            _storage.cells.reset()
            _storage.cells.hwSortingM.reset()

            logM.reset(Debug.SMC)
            _gameTimer.reset()
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





//    private fun forwardBrushes()
//        = _storage.cells.hwSortingM.forwardBrushes()
//
//    private fun reverseBrushes()
//            = _storage.cells.hwSortingM.reverseBrushes()



    private fun startDrumRequest(
        shootingMode:    Shooting.Mode,
        requestPattern:  Array<BallRequest.Name>,
        failsafePattern: Array<BallRequest.Name>
    )
    {
        logM.logMd("Started - SMART drum request", Debug.START)
        val requestResult = _storage.shootEntireDrumRequest(
                shootingMode,
                requestPattern,
                failsafePattern,
            TELEOP.INCLUDE_PREVIOUS_UNFINISHED_TO_REQUEST_ORDER,
            TELEOP.INCLUDE_PREVIOUS_UNFINISHED_TO_FAILSAFE_ORDER,
            TELEOP.AUTO_UPDATE_UNFINISHED_FOR_NEXT_PATTERN,
            TELEOP.IF_AUTO_UPDATE_UNFINISHED_USE_FAILSAFE_ORDER)


        logM.logMd("FINISHED - SMART drum request", Debug.END)

    }
    private fun startStreamDrumRequest()
    {
        logM.logMd("Started  - StreamDrum request", Debug.START)
        val requestResult = _storage.streamDrumRequest()
        logM.logMd("FINISHED - StreamDrum request", Debug.END)
    }
    private fun startSingleRequest(
        ballRequest: BallRequest.Name)
    {
        logM.logMd("Started - Single request", Debug.START)
        val requestResult = _storage.handleRequest(ballRequest)
        logM.logMd("FINISHED - Single request", Debug.END)
    }
}