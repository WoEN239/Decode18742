package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Ball
import woen239.enumerators.BallRequest

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import woen239.enumerators.ShotType
import woen239.enumerators.StorageType

import kotlinx.coroutines.delay

import org.woen.threading.ThreadedEventBus

import org.woen.telemetry.Configs.STORAGE.USED_STORAGE_TYPE
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_ONE_BALL_PUSHING_MS

import org.woen.modules.scoringSystem.storage.stream.StreamStorage
import org.woen.modules.scoringSystem.storage.sorting.SortingStorage



class SwitchStorage  //  Schrodinger storage
{
    private val _isStream  = StorageType.IsStream(USED_STORAGE_TYPE)
    private val _isSorting = !_isStream

    private var _streamStorage  = StreamStorage()
    private var _sortingStorage = SortingStorage()

    private var _hwSwitchStorage = HwSwitchStorage()



    fun isStream():  Boolean
    {
        return _isStream
    }
    fun isSorting(): Boolean
    {
        return _isSorting
    }


    private fun openGate()
    {
        _hwSwitchStorage.openGate()
    }
    private fun closeGate()
    {
        _hwSwitchStorage.closeGate()
    }

    suspend fun pushNext()
    {
        openGate()
        if (_isSorting)
        {
            _sortingStorage.pushNext()
            closeGate()
            _sortingStorage.pushNext(100)
        }
        else
        {
            delay(DELAY_FOR_ONE_BALL_PUSHING_MS)
            closeGate()
        }
    }



    //------------------------  UNIVERSAL METHODS  ------------------------//

    suspend fun handleIntake(inputBall: Ball.Name) : IntakeResult.Name
    {
        return if (_isStream) _streamStorage.handleIntake()
        else _sortingStorage.handleIntake(inputBall)
    }
    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        return if (_isStream) _streamStorage.shootEntireDrumRequest()
        else _sortingStorage.shootEntireDrumRequest()
    }

    fun ballCount(): Int
    {
        return if (_isStream) _streamStorage.ballCount()
        else _sortingStorage.anyBallCount()
    }



    suspend fun forceSafeStart()
    {
        if (_isStream) _streamStorage.forceSafeStart()
        else _sortingStorage.forceSafeStart()
    }
    suspend fun forceSafeStop()
    {
        if (_isStream) _streamStorage.forceSafeStop()
        else _sortingStorage.forceSafeStop()
    }
    fun emergencyForceStop()
    {
        if (_isStream) _streamStorage.emergencyForceStop()
        else _sortingStorage.emergencyForceStop()
    }



    fun terminateIntake()
    {
        if (_isStream) _streamStorage.switchTerminateIntake()
        else _sortingStorage.switchTerminateIntake()
    }
    fun terminateRequest()
    {
        if (_isStream) _streamStorage.switchTerminateRequest()
        else _sortingStorage.switchTerminateRequest()
    }
    fun shotWasFired()
    {
        if (_isStream) _streamStorage.shotWasFired()
        else _sortingStorage.shotWasFired()
    }
    fun ballWasEaten()
    {
        if (_isStream) _streamStorage.ballWasEaten()
        else _sortingStorage.ballWasEaten()
    }





    //------------------------  SORTING ONLY  ------------------------//

    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
    {
        return if (_isSorting) _sortingStorage.handleRequest(request)
        else RequestResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE
    }
    suspend fun shootEntireDrumRequest(
        requestOrder:  Array<BallRequest.Name>,
        failsafeOrder: Array<BallRequest.Name> = requestOrder,
        shotType: ShotType = ShotType.FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
    ): RequestResult.Name
    {
        return if (_isSorting) _sortingStorage.shootEntireDrumRequest(requestOrder, failsafeOrder, shotType)
        else RequestResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE
    }


    fun storageRaw(): Array<Ball>?
    {
        return if (_isSorting) _sortingStorage.storageRaw()
        else null
    }
    fun storageFiltered():  Array<Ball>?
    {
        return if (_isSorting) _sortingStorage.storageFiltered()
        else null
    }

    fun ballColorCountPG(): IntArray?
    {
        return if (_isSorting) _sortingStorage.ballColorCountPG()
        else null
    }

    fun purpleBallCount(): Int?
    {
        return if (_isSorting) _sortingStorage.purpleBallCount()
        else null
    }
    fun greenBallCount():  Int?
    {
        return if (_isSorting) _sortingStorage.greenBallCount()
        else null
    }
    fun selectedBallCount(ball: Ball.Name): Int?
    {
        return if (_isSorting) _sortingStorage.selectedBallCount(ball)
        else null
    }





    //-------------  Hardware linker for storage type logic  -------------//
    init
    {
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


        if (_isStream) _streamStorage.linkHardware()
        else _sortingStorage.linkHardware()
    }
}