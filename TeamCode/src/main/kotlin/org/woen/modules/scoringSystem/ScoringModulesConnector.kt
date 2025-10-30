package org.woen.modules.scoringSystem


import woen239.enumerators.Ball
import woen239.enumerators.IntakeResult

import org.woen.modules.IModule
import org.woen.modules.scoringSystem.brush.brush_Soft
import org.woen.modules.scoringSystem.turret.Turret
import org.woen.modules.scoringSystem.storage.SwitchStorage

import org.woen.telemetry.Configs.STORAGE.REAL_SLOT_COUNT

import org.woen.threading.ThreadedEventBus
import org.woen.modules.scoringSystem.storage.BallWasEatenByTheStorageEvent
import org.woen.modules.scoringSystem.storage.StorageIsReadyToEatIntakeEvent



class ScoringModulesConnector: IModule
{
    private val _storage = SwitchStorage()  //  Schrodinger storage
    private val _turret  = Turret()
    private val _brush   = brush_Soft()     //!  = Brush()



    suspend fun startIntakeProcess(inputBall: Ball.Name): IntakeResult.Name
    {
        val intakeResult = _storage.handleIntake(inputBall)
        //  1) If something fails, finished immediately
        //  2) > If all is good - sends event StorageIsReadyToEatIntakeEvent
        //  3) > Awaits BallWasEatenEvent from BOTTOM hardware color sensor
        //  4) > return intake result when fully finished


        if (_storage.ballCount() >= REAL_SLOT_COUNT)
            _brush.revers(1000)  //!  Improve this, add to configs

        return intakeResult
    }
    suspend fun awaitSuccessfulBalIntake()
    {
        TODO("Add awaiting for sensor ball intake detection")
        //!  Ball intake validation by sensors (BOTTOM sensor sees ball = intake success)

        ThreadedEventBus.LAZY_INSTANCE.invoke(BallWasEatenByTheStorageEvent())
    }



    override suspend fun process()
    {
        TODO("Not yet implemented")
    }

    override val isBusy: Boolean
        get() = TODO("Not yet implemented")

    override fun dispose() { }



    init
    {
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageIsReadyToEatIntakeEvent::class, {

            //!  _brush.StartIntake
            TODO("Active brushes to intake")
        } )
    }
}