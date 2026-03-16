package org.woen.scoringSystem.storage


import org.woen.collector.Collector

import org.woen.enumerators.Ball
import org.woen.enumerators.BallRequest

import org.woen.enumerators.RequestResult
import org.woen.enumerators.Shooting

import org.woen.utils.debug.LogManager

import org.woen.configs.Alias.Request
import org.woen.configs.Alias.MAX_BALL_COUNT

import org.woen.utils.debug.Debug
import org.woen.configs.Delay
import org.woen.configs.ProcessId
import org.woen.configs.RobotSettings.SORTING
import org.woen.configs.RobotSettings.SHOOTING
import org.woen.configs.RobotSettings.CONTROLS
import org.woen.scoringSystem.ConnectorModuleStatus
import org.woen.scoringSystem.misc.DynamicPattern
import kotlin.math.min



class Storage
{
    private val _collector: Collector
    private val _cms: ConnectorModuleStatus
    val cells: Cells
    val logM: LogManager



    constructor(collector: Collector, cms: ConnectorModuleStatus)
    {
        _collector = collector
        _cms = cms

        cells = Cells(_collector, cms)
        
        logM  = LogManager(_collector, Debug.SSM)

        subscribeToInfoEvents()
        subscribeToActionEvents()
        subscribeToSecondDriverPatternRecalibration()

        cells.hwSortingM.reset()
        cells.reset()
        logM.reset(Debug.SSM)
    }



    private fun subscribeToInfoEvents()
    {
    }
    private fun subscribeToActionEvents()
    {
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





    fun unsafeTestSorting()
    {
        val fill = arrayOf(Ball.Name.GREEN, Ball.Name.PURPLE, Ball.Name.PURPLE)
        cells.lazySet(fill)

        var iteration = 0
        while (iteration < 100)
        {
            logM.logMd("\nIteration: $iteration", Debug.GENERIC)

            cells.fullRotate()
            iteration++
        }
    }
//    suspend fun hwSmartPushNextBall()
//        = cells.hwSortingM.smartPushNextBall()
    private fun inversePattern(
        initial: Array<BallRequest.Name>): Array<BallRequest.Name>
    {
        var i = 0
        while (i < initial.size)
        {
            initial[i] = BallRequest.toInverse(initial[i])
            i++
        }
        return initial
    }



    fun handleRequest(request: BallRequest.Name)
    {
        cells.hwReAdjustStorage()

        logM.logMd("searching for request slot", Debug.LOGIC)
        val requestResult = cells.handleRequest(request)
        logM.logMd("FINISHED searching, result: ${requestResult.name}", Debug.END)


        val shootingResult = shootRequestFinalPhase(
            requestResult)
    }
    


    fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        includePreviousUnfinishedToRequest: Boolean = true,
        autoUpdateUnfinishedForNextPattern: Boolean = true):       RequestResult.Name
    {
        if (cells.isEmpty()) return Request.FAIL_IS_EMPTY
        if (requestOrder.isEmpty())  return Request.ILLEGAL_ARGUMENT

        var  standardPatternOrder = if (!includePreviousUnfinishedToRequest) requestOrder
        else DynamicPattern.trimPattern(
             _cms.dynamicMemoryPattern.lastUnfinished(),
             requestOrder)

        if (autoUpdateUnfinishedForNextPattern)
            _cms.dynamicMemoryPattern.setTemporary(standardPatternOrder)

        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> streamDrumRequest()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> shootDrumCanSkipLogic(
                    standardPatternOrder)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN -> {}
//                    -> shootDrumUntilPatternBreaks(
//                    standardPatternOrder,
//                        autoUpdateUnfinishedForNextPattern)

                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID -> {}
//                    -> shootDrumRequestEntireIsValid(standardPatternOrder)
            }

        resumeLogicAfterRequest(
            ProcessId.DRUM_REQUEST,
            cells.isNotEmpty())
        return Request.SUCCESS_NOW_EMPTY
    }
    fun shootEntireDrumRequest(
        shootingMode:  Shooting.Mode,
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name>? = requestOrder,
        includePreviousUnfinishedToRequest:       Boolean = true,
        includePreviousUnfinishedToFailsafe:      Boolean = true,
        autoUpdateUnfinishedForNextPattern:       Boolean = true,
        ifAutoUpdatingUnfinishedUseFailsafeOrder: Boolean = true): RequestResult.Name
    {
        if (failsafeOrder == null || failsafeOrder.isEmpty() ||
            failsafeOrder.contentEquals(requestOrder))
            return shootEntireDrumRequest(shootingMode, requestOrder, includePreviousUnfinishedToRequest)

        if (cells.isEmpty()) return Request.FAIL_IS_EMPTY


        var  standardPatternOrder = if (!includePreviousUnfinishedToRequest) requestOrder
        else DynamicPattern.trimPattern(
            _cms.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)

        var  failsafePatternOrder = if (!includePreviousUnfinishedToFailsafe) requestOrder
        else DynamicPattern.trimPattern(
            _cms.dynamicMemoryPattern.lastUnfinished(),
            requestOrder)

        if (SORTING.INVERSE_SHOOTING_PATTERNS)
        {
            standardPatternOrder = inversePattern(standardPatternOrder)
            failsafePatternOrder = inversePattern(failsafePatternOrder)
        }


        val autoUpdateUnfinishedWithFailsafe =
                autoUpdateUnfinishedForNextPattern &&
                ifAutoUpdatingUnfinishedUseFailsafeOrder
        if (autoUpdateUnfinishedForNextPattern)
            _cms.dynamicMemoryPattern.setTemporary(standardPatternOrder)


        val requestResult =
            when (shootingMode)
            {
                Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE
                    -> streamDrumRequest()

                Shooting.Mode.FIRE_PATTERN_CAN_SKIP
                    -> shootDrumCanSkip(
                    standardPatternOrder,
                    failsafePatternOrder)

                Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN -> {}
//                    -> shootDrumUntilPatternBreaks(
//                    standardPatternOrder,
//                    failsafePatternOrder,
//                    autoUpdateUnfinishedWithFailsafe)

                Shooting.Mode.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID -> {}
//                    -> shootDrumRequestEntireIsValid(
//                    standardPatternOrder,
//                    failsafePatternOrder,
//                    autoUpdateUnfinishedWithFailsafe)
            }

        resumeLogicAfterRequest(
            ProcessId.DRUM_REQUEST,
            cells.isNotEmpty())
        return Request.SUCCESS_NOW_EMPTY
    }




    fun reset()
    {
        _cms.dynamicMemoryPattern.fullReset()
        logM.reset(Debug.SSL)
    }

    


    private fun rotateToFoundBall(
        requestResult: RequestResult,
        doExtraRecalibration: Boolean): RequestResult
    {
        if (requestResult.didFail()) return requestResult  //  Request search failed
        
        if (doExtraRecalibration)
        {
            cells.hwSortingM.reverseBeltsTime(Delay.MS.PUSH.PART)
            cells.hwSortingM.closeTurretGate()
            cells.hwSortingM.slowStartBelts()

//            delay(Delay.MS.PUSH.HALF)
            cells.hwSortingM.stopBelts()
        }

        val fullRotations = when (requestResult.name)
        {
            Request.MOBILE_SLOT -> 2
            Request.BOTTOM_SLOT -> 2
            Request.CENTER_SLOT -> 1
            else -> -1
        }

        logM.logMd("rotating cur slot times: $fullRotations", Debug.LOGIC)
        repeat(fullRotations)
        { cells.fullRotate() }

        logM.logMd("sorting finished - success", Debug.END)
        logM.logMd("Getting ready to shoot", Debug.START)
        return Request.F_SUCCESS
    }
    fun shootRequestFinalPhase(
        requestResult: RequestResult): RequestResult.Name
    {
//        if (requestResult.didFail())
            return requestResult.name

//        val updateResult = if (requestResult == Request.TURRET_SLOT) Request.TURRET_SLOT
//        else rotateToFoundBall(requestResult)
//
//        logM.logMd("Finished updating", Debug.END)
//
//        return if (updateResult.didSucceed())
//        {
//            if (!fullWaitForShotFired())
//                Request.TERMINATED
//            else if (cells.isNotEmpty())
//                Request.SUCCESS
//            else Request.SUCCESS_NOW_EMPTY
//        }
//        else updateResult.name
    }



    

    fun streamDrumRequest()
    {
        val ballCount = if (SHOOTING.USE_LAZY_VERSION_OF_STREAM_DRUM) 3
                        else cells.anyBallCount()
        
        logM.logMd("Starting stream shooting, count: $ballCount", Debug.START)

        val beltPushTime = when (ballCount)
        {
            3    -> Delay.MS.SHOOTING.FIRE_3
            2    -> Delay.MS.SHOOTING.FIRE_2
            else -> Delay.MS.SHOOTING.FIRE_1
        }

        logM.logMd("Firing time: $beltPushTime", Debug.GENERIC)


        cells.hwSortingM.stopBelts()
        cells.hwSortingM.openTurretGate()

        cells.hwSortingM.forwardBeltsTime(beltPushTime)

        cells.fullEmptyStorageCells()
    }


    fun shootDrumCanSkip(
        requested: Array<BallRequest.Name>,
        failsafe:  Array<BallRequest.Name>
    ): RequestResult.Name
    {
        val req1 = cells.predictSortSearch(requested).maxSequenceScore
        val req2 = cells.predictSortSearch(failsafe).maxSequenceScore

        return shootDrumCanSkipLogic(
            if (req1 > req2) requested
                        else failsafe)
    }
    fun shootDrumCanSkipLogic(
        requested: Array<BallRequest.Name>
    ): RequestResult.Name
    {
        cells.initiatePredictSort(requested)
        streamDrumRequest()
        return Request.SUCCESS_NOW_EMPTY
    }



//    suspend fun shootDrumUntilPatternBreaks(
//        requested: Array<BallRequest.Name>,
//        autoUpdatePatternWhenSucceed: Boolean
//    ): RequestResult.Name
//            = shootDrumUntilBreaksLogic(
//        requested,
//        min(requested.size, MAX_BALL_COUNT),
//        Request.COLOR_NOT_PRESENT,
//        cells
//            .initiatePredictSort(requested),
//        autoUpdatePatternWhenSucceed)
//    suspend fun shootDrumUntilPatternBreaks(
//        requested: Array<BallRequest.Name>,
//        failsafe:  Array<BallRequest.Name>,
//        autoUpdatePatternWhenSucceed:     Boolean = true,
//        autoUpdateUnfinishedWithFailsafe: Boolean = true
//    ): RequestResult.Name
//    {
//        var isNowPerfectlySorted = cells.tryInitiatePredictSort(requested)
//        var trimmedRequestSize   = min(requested.size, MAX_BALL_COUNT)
//
//        val shootingResult = shootDrumUntilBreaksLogic(
//            requested, trimmedRequestSize,
//            Request.COLOR_NOT_PRESENT,
//            isNowPerfectlySorted,
//            autoUpdatePatternWhenSucceed)
//
//        if (Request.didSucceed   (shootingResult) ||
//            Request.wasTerminated(shootingResult))
//            return shootingResult
//        else if (autoUpdateUnfinishedWithFailsafe)
//            dynamicMemoryPattern.setTemporary(failsafe)
//
//        isNowPerfectlySorted = cells.tryInitiatePredictSort(failsafe)
//        trimmedRequestSize   = min(failsafe.size, MAX_BALL_COUNT)
//
//        return shootDrumUntilBreaksLogic(
//            failsafe, trimmedRequestSize,
//            Request.COLOR_NOT_PRESENT,
//            isNowPerfectlySorted,
//            autoUpdateUnfinishedWithFailsafe)
//    }
//    suspend fun shootDrumUntilBreaksLogic(
//        requested: Array<BallRequest.Name>,
//        trimmedRequestSize: Int,
//        defaultError: RequestResult.Name,
//        isNowPerfectlySorted:         Boolean = false,
//        autoUpdatePatternWhenSucceed: Boolean = true): RequestResult.Name
//    {
//        var isNowPerfectlySorted = isNowPerfectlySorted
//        var shootingResult  = defaultError
//        var curRequestId    = 0
//
//        cells.hwReAdjustStorage()
//
//        while (curRequestId < trimmedRequestSize)
//        {
//            if (furtherDoPredictSort(curRequestId, isNowPerfectlySorted))
//                isNowPerfectlySorted = cells.tryInitiatePredictSort(requested)
//
//            if (isNowPerfectlySorted)
//            {
//                streamDrumRequest()
//                return Request.SUCCESS_NOW_EMPTY
//            }
//
//            val requestResult = cells.handleRequest(
//                requested[curRequestId])
//
//            shootingResult = shootRequestFinalPhase(
//                requestResult, ProcessId.DRUM_REQUEST,
//                autoUpdatePatternWhenSucceed)
//
//
//            if (Request.wasTerminated(shootingResult)) return Request.TERMINATED
//            if (Request.didFail(      shootingResult)) return defaultError
//
//            curRequestId++
//        }
//        return shootingResult
//    }
//
//
//
//    suspend fun shootDrumRequestEntireIsValid(
//        requested: Array<BallRequest.Name>,
//        autoUpdatePatternWhenSucceed: Boolean = true
//    ): RequestResult.Name
//    {
//        val curStoragePG = cells.ballCountPGA()
//        val requestPGA   = countPGA(requested)
//
//        if (canCompleteEntireRequest(curStoragePG, requestPGA))
//            return shootDrumUntilBreaksLogic(
//                requested,
//                min(requested.size, MAX_BALL_COUNT),
//                Request.FAIL_UNKNOWN,
//                cells
//                    .initiatePredictSort(requested),
//                autoUpdatePatternWhenSucceed)
//
//        return Request.NOT_ENOUGH_COLORS
//    }
//    suspend fun shootDrumRequestEntireIsValid(
//        requested: Array<BallRequest.Name>,
//        failsafe:  Array<BallRequest.Name>,
//        autoUpdatePatternWhenSucceed:     Boolean = true,
//        autoUpdateUnfinishedWithFailsafe: Boolean = true
//    ): RequestResult.Name
//    {
//        val curStoragePG = cells.ballCountPGA()
//        var requestPGA   = countPGA(requested)
//
//        if (canCompleteEntireRequest(curStoragePG, requestPGA))  //  All good
//            return shootDrumUntilBreaksLogic(
//                requested,
//                min(requested.size, MAX_BALL_COUNT),
//                Request.FAIL_UNKNOWN,
//                cells
//                    .initiatePredictSort(requested),
//                autoUpdatePatternWhenSucceed)
//
//        if (autoUpdateUnfinishedWithFailsafe)
//            _cms.dynamicMemoryPattern.setTemporary(failsafe)
//
//        requestPGA = countPGA(failsafe)
//
//        if (canCompleteEntireRequest(curStoragePG, requestPGA))  //  Failsafe good
//            return shootDrumUntilBreaksLogic(
//                failsafe,
//                min(failsafe.size, MAX_BALL_COUNT),
//                Request.FAIL_UNKNOWN,
//                cells
//                    .initiatePredictSort(failsafe),
//                autoUpdateUnfinishedWithFailsafe)
//
//        return Request.NOT_ENOUGH_COLORS
//    }



    private fun furtherDoPredictSort(
        curRequestId: Int,
        isNowPerfectlySorted: Boolean)
            = !(SORTING.PREDICT.ALWAYS_TRY_IN_ADVANCE && curRequestId == 0)
            && !isNowPerfectlySorted && SORTING.PREDICT.FURTHER_TRY_IN_ACTION
    private fun countPGA(order: Array<BallRequest.Name>): CountPGA
    {
        val intPGAN = intArrayOf(0, 0, 0, 0)
        //  Purple, Green, AbstractAny, Nothing

        var curRequestId = 0
        val ballCountInRequest = min(order.size, MAX_BALL_COUNT)

        while (curRequestId < ballCountInRequest)
        {
            intPGAN[
                BallRequest.toAbstractBallId
                    (
                    order[curRequestId]
                )
            ]++

            curRequestId++
        }
        return cells.toCountPGA(intPGAN)
    }
    private fun canCompleteEntireRequest(curStoragePG: CountPGA, requestPGA: CountPGA): Boolean
    {
        val futureStorage = CountPGA(
            curStoragePG.purple - requestPGA.purple,
            curStoragePG.green  - requestPGA.green,
            curStoragePG.any)

        return futureStorage.purple >= 0
            && futureStorage.green  >= 0

            && futureStorage.purple +
               futureStorage.green  +
               futureStorage.any   >= requestPGA.any
    }





    fun resumeLogicAfterRequest(
        processId: Int,
        doAutoCalibration: Boolean = true)
    {
        logM.logMd("RESUME AFTER REQUEST, process: $processId", Debug.PROCESS_NAME)

        if (doAutoCalibration)
        {
            logM.logMd("Reversing belts for calibration", Debug.START)
            cells.hwSortingM.reverseBeltsTime(Delay.MS.PUSH.HALF)
            logM.logMd("Finished reversing", Debug.END)

            logM.logMd("Starting calibration", Debug.START)
            cells.hwSortingM.forwardBeltsTime(Delay.MS.PUSH.HALF)
            cells.hwSortingM.fullCalibrate()
        }
        else cells.hwSortingM.fullCalibrate()


        logM.logMd("Phase 2 - RESUME AFTER REQUEST, process: $processId", Debug.LOGIC)

        _cms.canTriggerIntake.set(true)

        logM.logMd("FINISHED resume logic", Debug.END)
    }
}