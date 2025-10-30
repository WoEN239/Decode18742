package org.woen.modules.scoringSystem


import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference

import woen239.enumerators.Ball
import woen239.enumerators.BallRequest
import woen239.enumerators.ShotType
import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult

import org.woen.modules.IModule
import org.woen.modules.scoringSystem.brush.BrushSoft
import org.woen.modules.scoringSystem.turret.Turret
import org.woen.modules.scoringSystem.storage.SwitchStorage

import org.woen.telemetry.Configs.STORAGE.REAL_SLOT_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING

import org.woen.threading.ThreadedEventBus
import org.woen.modules.scoringSystem.turret.RequestTurretAtTargetEvent
import org.woen.modules.scoringSystem.turret.SetCurrentTurretStateEvent
import org.woen.modules.scoringSystem.storage.StorageIsReadyToEatIntakeEvent   // Intake event
import org.woen.modules.scoringSystem.storage.ShotWasFiredEvent             // Request event
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent    // Request event
import org.woen.modules.scoringSystem.storage.BallWasEatenByTheStorageEvent // Request event



class ScoringModulesConnector: IModule
{
    private val _storage = SwitchStorage()  //  Schrodinger storage
    private val _brush   = BrushSoft()

    private val _isStream  = _storage.isStream()
    private val _isSorting = _storage.isSorting()

    private var _isBusy = AtomicReference(false)





    fun setBusy()
    {
        _isBusy.set(true)
    }
    fun setIdle()
    {
        _isBusy.set(false)
    }



    suspend fun startIntakeProcess(inputBall: Ball.Name): IntakeResult.Name
    {
        /*----------  How intake in connector module works:
         *  0) = Wait while every request is finished (if is busy)
         *
         *  1) If something fails, exit immediately
         *  2) > If all is good - sends event StorageIsReadyToEatIntakeEvent
         *  3) > Awaits BallWasEatenEvent from BOTTOM hardware color sensor
         *  4) > return intake result when fully finished
         */



        if (isBusy) return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        setBusy()

        val intakeResult = _storage.handleIntake(inputBall)


        if (_storage.ballCount() >= REAL_SLOT_COUNT)
            _brush.revers(1000)  //!  Improve this, add to configs

        setIdle()
        return intakeResult
    }
    suspend fun awaitSuccessfulBallIntake()
    {
        TODO("Add awaiting for sensor ball intake detection")
        //!  Ball intake validation by sensors (BOTTOM sensor sees ball = intake success)

        ThreadedEventBus.LAZY_INSTANCE.invoke(BallWasEatenByTheStorageEvent())
    }
    fun currentlyEatingIntakeProcess()
    {
        //!  _brush.StartIntake
        TODO("Active brushes to intake")
    }





    fun setTurretToWaitMode()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
            SetCurrentTurretStateEvent(
                Turret.TurretState.WAITING))
    }
    fun setTurretToShootMode()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(
        SetCurrentTurretStateEvent(
            Turret.TurretState.SHOOT))
    }


    suspend fun startDrumRequest(
        requestPattern: Array<BallRequest.Name>,
        failsafePattern: Array<BallRequest.Name>,
        shotType: ShotType) : RequestResult.Name
    {
        /*----------  How every request in connector module works:
         *  0) = Wait while intake is finished (if is busy)
         *
         *  1) If something fails, finished immediately
         *  2) > If all is good - sends event StorageRequestIsReadyEvent
         *  3) > Waits for the turret being fully accelerated
         *  4) > Push the ball in the turret
         *  5) > Ball flies, and should invoke ShotWasFiredEvent
         *  6) > Awaits ShotWasFiredEvent
         *  7) > > Loop if drum request continues
         *  8) Exit when entire drum was fired
         */

        while (isBusy) delay(DELAY_FOR_EVENT_AWAITING)
        setBusy()


        setTurretToShootMode()
        val requestResult =
            if (_isSorting) _storage.shootEntireDrumRequest(
                requestPattern,
                failsafePattern, shotType)
            else _storage.shootEntireDrumRequest()


        setTurretToWaitMode()
        setIdle()
        return requestResult
    }
    suspend fun startSimpleDrumRequest(): RequestResult.Name
    {
        while (isBusy) delay(DELAY_FOR_EVENT_AWAITING)
        setBusy()


        setTurretToShootMode()
        val requestResult = _storage.shootEntireDrumRequest()


        setTurretToWaitMode()
        setIdle()
        return requestResult
    }
    suspend fun startSingleRequest(ballRequest: BallRequest.Name): RequestResult.Name
    {
        if (_isStream) return RequestResult.Name.FAIL_USING_DIFFERENT_STORAGE_TYPE

        while (isBusy) delay(DELAY_FOR_EVENT_AWAITING)
        setBusy()


        setTurretToShootMode()
        val requestResult = _storage.handleRequest(ballRequest)


        setTurretToWaitMode()
        setIdle()
        return requestResult
    }


    suspend fun currentlyShootingRequestsProcess()
    {
        var turretHasAccelerated = ThreadedEventBus.LAZY_INSTANCE.invoke(
            RequestTurretAtTargetEvent()).atTarget

        while (!turretHasAccelerated)
        {
            delay (DELAY_FOR_EVENT_AWAITING)

            turretHasAccelerated = ThreadedEventBus.LAZY_INSTANCE.invoke(
                RequestTurretAtTargetEvent()).atTarget
        }


        //!  Push the ball to the turret
        TODO("Wait for the ball being pushed by motor")


        ThreadedEventBus.LAZY_INSTANCE.invoke(ShotWasFiredEvent())
    }







    override val isBusy: Boolean get() = _isBusy.get()

    override suspend fun process() { }
    override fun dispose() { }

    init
    {
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageIsReadyToEatIntakeEvent::class, {

                currentlyEatingIntakeProcess()
        } )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(
            StorageRequestIsReadyEvent::class, {

                currentlyShootingRequestsProcess()
        } )

    }
}