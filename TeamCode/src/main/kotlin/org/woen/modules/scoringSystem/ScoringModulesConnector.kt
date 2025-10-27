package org.woen.modules.scoringSystem


import org.woen.modules.IModule
import org.woen.modules.storage.SwitchStorage
import org.woen.threading.ThreadedEventBus



class ScoringModulesConnector: IModule
{
    private val _storage = SwitchStorage()  //  Schrodinger storage


    override suspend fun process()
    {
        TODO("Not yet implemented")
    }

    override val isBusy: Boolean
        get() = TODO("Not yet implemented")

    override fun dispose() { }

    init
    {

    }


}