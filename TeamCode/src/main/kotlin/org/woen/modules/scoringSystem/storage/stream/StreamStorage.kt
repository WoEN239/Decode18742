package org.woen.modules.scoringSystem.storage.stream


import kotlinx.coroutines.delay
import org.woen.modules.scoringSystem.storage.StorageFinishedEveryRequestEvent
import org.woen.modules.scoringSystem.storage.StorageFinishedIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageIsReadyToEatIntakeEvent
import org.woen.modules.scoringSystem.storage.StorageRequestIsReadyEvent
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EATING_INTAKE_IN_STREAM_STORAGE_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING_MS
import org.woen.telemetry.Configs.STORAGE.INTAKE_RACE_CONDITION_DELAY_MS
import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.REQUEST_RACE_CONDITION_DELAY_MS
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult
import woen239.enumerators.RunStatus
import java.util.concurrent.atomic.AtomicReference


class StreamStorage
{
    private var _intakeRunStatus  = RunStatus()
    private var _requestRunStatus = RunStatus()
    //private lateinit var _hwStreamStorage : HwStreamStorage  //  DO NOT JOIN ASSIGNMENT


    private var _ballCount = AtomicReference(0)
    private var _shotWasFired = AtomicReference(false)
    private var _ballWasEaten = AtomicReference(false)



    suspend fun handleIntake(): IntakeResult.Name
    {
        if (_ballCount.get() >= MAX_BALL_COUNT) return IntakeResult.Name.FAIL_STORAGE_IS_FULL

        if (noIntakeRaceConditionProblems())
        {
            if (doTerminateIntake()) return terminateIntake()

            val storageCanHandle = storageCanHandleIntake()

            val intakeResult = updateAfterInput(storageCanHandle)
            //  Safe updating storage after intake  - wont update if an error occurs

            safeResumeRequestLogic(intakeResult)
            return intakeResult
        }

        val intakeFail = IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        safeResumeRequestLogic(intakeFail)
        return intakeFail
    }
    private fun storageCanHandleIntake(): IntakeResult
    {
        if (_ballCount.get() < MAX_BALL_COUNT) return IntakeResult(
            IntakeResult.SUCCESS,
            IntakeResult.Name.SUCCESS
        )

        return IntakeResult(
            IntakeResult.FAIL_STORAGE_IS_FULL,
            IntakeResult.Name.FAIL_STORAGE_IS_FULL
        )
    }

    private suspend fun intakeRaceConditionIsPresent(): Boolean
    {
        if (_intakeRunStatus.IsActive())
        {
            forceStopRequest()

            delay(INTAKE_RACE_CONDITION_DELAY_MS)
            return _intakeRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private suspend fun noIntakeRaceConditionProblems(): Boolean
    {
        _intakeRunStatus.SafeResetTermination()

        return !intakeRaceConditionIsPresent()
    }
    private suspend fun updateAfterInput(intakeResult: IntakeResult): IntakeResult.Name
    {
        if (intakeResult.DidFail())
            return intakeResult.Name()

        if (!fullWaitForIntakeIsFinishedEvent())
            return terminateIntake()

        _ballCount.set(_ballCount.get() + 1)

        //_hwStreamStorage.start()
        delay(DELAY_FOR_EATING_INTAKE_IN_STREAM_STORAGE_MS)
        //_hwStreamStorage.stop()

        return IntakeResult.Name.SUCCESS
    }


    private fun terminateIntake(): IntakeResult.Name
    {
        _intakeRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        val intakeFail = IntakeResult.Name.FAIL_PROCESS_WAS_TERMINATED
        safeResumeRequestLogic(intakeFail)

        return intakeFail
    }
    private fun doTerminateIntake() = _intakeRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    fun switchTerminateIntake() = _intakeRunStatus.DoTerminate()





    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        if (_ballCount.get() <= 0) return RequestResult.Name.FAIL_IS_EMPTY

        handleRequestRaceCondition()
        if (doTerminateRequest()) return terminateRequest().Name()

        val requestResult = shootEverything()

        safeResumeIntakeLogic(requestResult)
        return requestResult
    }
    private suspend fun shootEverything(): RequestResult.Name
    {
        var shootingResult = RequestResult(
            RequestResult.SUCCESS_IS_NOW_EMPTY,
            RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        )

        var i = 0
        while (i < MAX_BALL_COUNT)
        {
            shootingResult = shootClosest(shootingResult)

            if (doTerminateRequest() || shootingResult.WasTerminated())
                return shootingResult.Name()

            i++
        }
        return shootingResult.Name()
    }
    private fun updateAfterRequest(): RequestResult
    {
        _ballCount.set(_ballCount.get() - 1)

        return if (_ballCount.get() > 0)
            RequestResult(
                RequestResult.SUCCESS,
                RequestResult.Name.SUCCESS
            )
        else RequestResult(
            RequestResult.SUCCESS_IS_NOW_EMPTY,
            RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        )
    }
    private suspend fun shootClosest(requestResult: RequestResult): RequestResult
    {
        if (requestResult.DidFail()) return requestResult
        else
        {
            if (!waitForShotFiredEvent()) return terminateRequest()
            return updateAfterRequest()
        }
    }

    private suspend fun requestRaceConditionIsPresent(): Boolean
    {
        if (_requestRunStatus.IsActive())
        {
            forceStopIntake()

            delay(REQUEST_RACE_CONDITION_DELAY_MS)
            return _requestRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private suspend fun handleRequestRaceCondition()
    {
        _requestRunStatus.SafeResetTermination()

        while (requestRaceConditionIsPresent())
            delay(REQUEST_RACE_CONDITION_DELAY_MS)
    }


    private fun terminateRequest(): RequestResult
    {
        _requestRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        val requestFail = RequestResult(
            RequestResult.FAIL_PROCESS_WAS_TERMINATED,
            RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)

        safeResumeIntakeLogic(requestFail.Name())
        return requestFail
    }
    private fun doTerminateRequest() = _requestRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    fun switchTerminateRequest() = _requestRunStatus.DoTerminate()



    fun ballWasEaten() = _ballWasEaten.set(true)
    private suspend fun fullWaitForIntakeIsFinishedEvent(): Boolean
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageIsReadyToEatIntakeEvent())

        while (!_ballWasEaten.get())
        {
            delay(DELAY_FOR_EVENT_AWAITING_MS)
            if (doTerminateIntake()) return false
        }

        _ballWasEaten.set(false)
        return true
    }


    fun shotWasFired() = _shotWasFired.set(true)
    private suspend fun waitForShotFiredEvent(): Boolean
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageRequestIsReadyEvent())

        while (!_shotWasFired.get())
        {
            delay(DELAY_FOR_EVENT_AWAITING_MS)
            if (doTerminateRequest()) return false
        }

        _shotWasFired.set(false)
        return true
    }



    fun ballCount(): Int = _ballCount.get()



    private fun forceStopIntake() = _intakeRunStatus.SetAlreadyUsed()
    private fun safeResumeIntakeLogic(requestResult: RequestResult.Name)
    {
        if (_intakeRunStatus.IsUsedByAnotherProcess())
            _intakeRunStatus.SetActive()

        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageFinishedEveryRequestEvent(requestResult))
    }

    private fun forceStopRequest() = _requestRunStatus.SetAlreadyUsed()
    private fun safeResumeRequestLogic(intakeResult: IntakeResult.Name)
    {
        if (_requestRunStatus.IsUsedByAnotherProcess())
            _requestRunStatus.SetActive()

        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageFinishedIntakeEvent(intakeResult))
    }


    //fun startHwBelt() = _hwStreamStorage.start()
    //fun stopHwBelt() = _hwStreamStorage.stop()



    suspend fun forceSafeStart()
    {
        while (!_intakeRunStatus.IsInactive())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        _intakeRunStatus.SetActive()

        while (!_requestRunStatus.IsInactive())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        _requestRunStatus.SetActive()

        //_hwStreamStorage.start()
    }

    suspend fun forceSafeStop()
    {
        _intakeRunStatus.DoTerminate()
        while (!_intakeRunStatus.IsTerminated())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        _intakeRunStatus.SetInactive()

        _requestRunStatus.DoTerminate()
        while (!_intakeRunStatus.IsTerminated())
            delay(DELAY_FOR_EVENT_AWAITING_MS)
        _intakeRunStatus.SetInactive()

        //_hwStreamStorage.stop()
    }
    fun emergencyForceStop()
    {
        _intakeRunStatus.SetInactive()
        _intakeRunStatus.DoTerminate()

        _requestRunStatus.SetInactive()
        _requestRunStatus.DoTerminate()

        //_hwStreamStorage.stop()
    }



    fun linkHardware()
    {
        //_hwStreamStorage = HwStreamStorage()
        //HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwStreamStorage)
    }
}