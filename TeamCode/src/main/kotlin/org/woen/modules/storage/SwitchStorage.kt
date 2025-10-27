package org.woen.modules.storage


import barrel.enumerators.Ball
import barrel.enumerators.BallRequest

import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import org.woen.threading.ThreadedEventBus

import barrel.enumerators.ShotType
import barrel.enumerators.StorageType

import org.woen.modules.storage.stream.StreamStorage
import org.woen.modules.storage.sorting.SortingStorage

import org.woen.telemetry.Configs.STORAGE.USED_STORAGE_TYPE



class SwitchStorage
{
    private val _isStream  = StorageType.IsStream(USED_STORAGE_TYPE)
    private val _isSorting = !_isStream

    private var _streamStorage  = StreamStorage()
    private var _sortingStorage = SortingStorage()



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

    suspend fun safeStart()
    {
        if (_isStream) _streamStorage.safeStart()
        else _sortingStorage.safeStart()
    }
    fun trySafeStart()
    {
        if (_isStream) _streamStorage.trySafeStart()
        else _sortingStorage.trySafeStart()
    }

    suspend fun safeStop(): Boolean
    {
        return if (_isStream) _streamStorage.safeStop()
        else _sortingStorage.safeStop()
    }
    fun forceStop()
    {
        if (_isStream) _streamStorage.forceStop()
        else _sortingStorage.forceStop()
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

    fun forceStopIntake()
    {
        if (_isStream) _streamStorage.forceStopIntake()
        else _sortingStorage.forceStopIntake()
    }
    fun safeResumeIntakeLogic()
    {
        if (_isStream) _streamStorage.safeResumeIntakeLogic()
        else _sortingStorage.safeResumeIntakeLogic()
    }

    fun forceStopRequest()
    {
        if (_isStream) _streamStorage.forceStopRequest()
        else _sortingStorage.forceStopRequest()
    }
    fun safeResumeRequestLogic()
    {
        if (_isStream) _streamStorage.safeResumeRequestLogic()
        else _sortingStorage.safeResumeRequestLogic()
    }





    //------------------------  SORTING ONLY  ------------------------//

    suspend fun handleRequest(request: BallRequest.Name): RequestResult.Name
    {
        return if (_isSorting) _sortingStorage.handleRequest(request)
        else RequestResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE
    }

    suspend fun shootEntireDrumRequest(
        requestOrder: Array<BallRequest.Name>,
        shotType: ShotType
    ): RequestResult.Name
    {
        return if (_isSorting) _sortingStorage.shootEntireDrumRequest(requestOrder, shotType)
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
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(GiveNextRequest::class, {
            shotWasFired()
        } )

        if (_isStream) _streamStorage.linkHardware()
        else _sortingStorage.linkHardware()
    }
}