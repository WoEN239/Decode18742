package org.woen.modules.scoringSystem

import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.scoringSystem.barrel.Barrel
import org.woen.threading.ThreadedEventBus


class ScoringModulesConnector: IModule
{
    private var _barrel: Barrel

    override suspend fun process() {
        TODO("Not yet implemented")
    }

    override val isBusy: Boolean
        get() = TODO("Not yet implemented")

    override fun dispose() { }

    init
    {
        _barrel = Barrel()
    }
}