package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Ball
import woen239.enumerators.BallRequest

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import woen239.enumerators.ShotType
import woen239.enumerators.StorageType

import kotlinx.coroutines.delay

import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads

import org.woen.telemetry.Configs.STORAGE.USED_STORAGE_TYPE
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_ONE_BALL_PUSHING_MS

import org.woen.modules.scoringSystem.storage.stream.StreamStorage
import org.woen.modules.scoringSystem.storage.sorting.SortingStorage
import org.woen.telemetry.ThreadedTelemetry


class SwitchStorage  //  Schrodinger storage
{
    val isStream  = StorageType.IsStream(USED_STORAGE_TYPE)
    val isSorting = !isStream

    private var _streamStorage  = StreamStorage()
    private var _sortingStorage = SortingStorage()

    private var _hwSwitchStorage = HwSwitchStorage()



    //-------------  Hardware linker for storage type logic  -------------//
    constructor()
    {
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            ColorSensorsSeeIntakeIncoming::class, {

                ThreadedEventBus.LAZY_INSTANCE.invoke(
                    StorageGetReadyForIntake(it.inputBall)
                )
                ThreadedTelemetry.LAZY_INSTANCE.log("")
                ThreadedTelemetry.LAZY_INSTANCE.log("COLOR SENSORS - START INTAKE")
                //  Alias
            } )


        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(TerminateIntakeEvent::class, {
            terminateIntake()
        } )
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(TerminateRequestEvent::class, {
            terminateRequest()
        } )


        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(ShotWasFiredEvent::class, {
            shotWasFired()
        } )
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(BallWasEatenByTheStorageEvent::class, {
            ballWasEaten()
        } )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(StorageOpenGateForShot::class, {
            _hwSwitchStorage.openGate()
        } )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(StorageCloseGateForShot::class, {
            _hwSwitchStorage.closeGate()
        } )


        if (isStream) _streamStorage.linkHardware()
        else _sortingStorage.linkHardware()

        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSwitchStorage)
    }



    private fun openGate()  = _hwSwitchStorage.openGate()
    private fun closeGate() = _hwSwitchStorage.closeGate()

    suspend fun pushNextWithoutUpdating()
    {
        openGate()

        if (isSorting)
        {
            //_sortingStorage.pushNextWithoutUpdating()
            //closeGate()
            _sortingStorage.pushNextWithoutUpdating(100)
        }
        else
        {
            //_streamStorage.startHwBelt()
            delay(DELAY_FOR_ONE_BALL_PUSHING_MS)
            //_streamStorage.stopHwBelt()
            closeGate()
        }
    }



    //------------------------  UNIVERSAL METHODS  ------------------------//

    suspend fun handleIntake(inputBall: Ball.Name) : IntakeResult.Name
    {
        if (inputBall == Ball.Name.NONE && isSorting)
            return IntakeResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE

        return if (isStream) _streamStorage.handleIntake()
        else _sortingStorage.handleIntake(inputBall)
    }
    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        return if (isStream) _streamStorage.shootEntireDrumRequest()
        else _sortingStorage.shootEntireDrumRequest()
    }

    fun ballCount(): Int
    {
        return if (isStream) _streamStorage.ballCount()
        else _sortingStorage.anyBallCount()
    }



    suspend fun forceSafeStart()
    {
        if (isStream) _streamStorage.forceSafeStart()
        else _sortingStorage.forceSafeStart()
    }
    suspend fun forceSafeStop()
    {
        if (isStream) _streamStorage.forceSafeStop()
        else _sortingStorage.forceSafeStop()
    }
    fun emergencyForceStop()
    {
        if (isStream) _streamStorage.emergencyForceStop()
        else _sortingStorage.emergencyForceStop()
    }



    fun terminateIntake()
    {
        if (isStream) _streamStorage.switchTerminateIntake()
        else _sortingStorage.switchTerminateIntake()
    }
    fun terminateRequest()
    {
        if (isStream) _streamStorage.switchTerminateRequest()
        else _sortingStorage.switchTerminateRequest()
    }
    fun shotWasFired()
    {
        if (isStream) _streamStorage.shotWasFired()
        else _sortingStorage.shotWasFired()
    }
    fun ballWasEaten()
    {
        if (isStream) _streamStorage.ballWasEaten()
        else _sortingStorage.ballWasEaten()
    }



    //------------------------  SORTING ONLY  ------------------------//

    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
    {
        return if (isSorting) _sortingStorage.handleRequest(request)
        else RequestResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE
    }
    suspend fun shootEntireDrumRequest(shotType: ShotType,
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name> = requestOrder
    ): RequestResult.Name
    {
        return if (isSorting) _sortingStorage.shootEntireDrumRequest(
            shotType, requestOrder, failsafeOrder)
        else RequestResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE
    }
    suspend fun shootEntireDrumRequest(
        shotType: ShotType,
        requestOrder:  Array<BallRequest.Name>
    ): RequestResult.Name
    {
        return if (isSorting) _sortingStorage.shootEntireDrumRequest(shotType, requestOrder)
        else RequestResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE
    }


    fun storageData() = if (isSorting) _sortingStorage.storageData() else null

    fun ballColorCountPG() = if (isSorting) _sortingStorage.ballColorCountPG() else null
    fun purpleBallCount()  = if (isSorting) _sortingStorage.selectedBallCount(Ball.Name.PURPLE) else -1
    fun greenBallCount()   = if (isSorting) _sortingStorage.selectedBallCount(Ball.Name.GREEN)  else -1

    fun selectedBallCount(ball: Ball.Name)
        = if (isSorting) _sortingStorage.selectedBallCount(ball) else -1
}