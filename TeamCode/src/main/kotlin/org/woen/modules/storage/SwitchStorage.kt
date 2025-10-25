package org.woen.modules.storage


import barrel.enumerators.Ball

import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import barrel.enumerators.StorageType
import org.woen.modules.storage.sorting.Storage
import org.woen.modules.storage.stream.StreamStorage

import org.woen.telemetry.Configs.STORAGE.USED_STORAGE_TYPE



class SwitchStorage
{
    private val _isStream = StorageType.IsStream(USED_STORAGE_TYPE)

    private lateinit var _sortingStorage: Storage
    private lateinit var _streamStorage:  StreamStorage



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
        else _sortingStorage.ballCount()
    }

    fun safeStart()
    {
        if (_isStream) _streamStorage.safeStart()
        else _sortingStorage.safeStart()
    }
    fun safeStop(): Boolean
    {
        return if (_isStream) _streamStorage.safeStop()
        else _sortingStorage.safeStop()
    }
    fun forceStop()
    {
        if (_isStream) _streamStorage.forceStop()
        else _sortingStorage.forceStop()
    }



    init
    {
        if (_isStream) _streamStorage = StreamStorage()
        else _sortingStorage = Storage()
    }
}