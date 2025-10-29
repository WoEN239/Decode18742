package org.woen.modules.storage.stream


import barrel.enumerators.RunStatus
import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import android.annotation.SuppressLint

import kotlinx.coroutines.delay
import org.woen.modules.storage.StorageFinishedEveryRequestEvent
import org.woen.modules.storage.StorageFinishedIntakeEvent
import java.util.concurrent.atomic.AtomicReference

import org.woen.threading.hardware.HardwareThreads

import org.woen.threading.ThreadedEventBus
import org.woen.modules.storage.StorageRequestIsReadyEvent

import org.woen.telemetry.Configs.STORAGE.REAL_SLOT_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING
import org.woen.telemetry.Configs.STORAGE.INTAKE_RACE_CONDITION_DELAY
import org.woen.telemetry.Configs.STORAGE.REQUEST_RACE_CONDITION_DELAY



class StreamStorage
{
    private var _ballCount = AtomicReference(0)
    private var _shotWasFired = AtomicReference(false)

    private var _intakeRunStatus  = RunStatus()
    private var _requestRunStatus = RunStatus()
    private lateinit var _hwStreamStorage : HwStreamStorage  //  DO NOT JOIN ASSIGNMENT



    @SuppressLint("SuspiciousIndentation")
    suspend fun handleIntake(): IntakeResult.Name
    {
        if (_ballCount.get() >= 3) return IntakeResult.Name.FAIL_STORAGE_IS_FULL

        if (noIntakeRaceConditionProblems())
        {
                if (doTerminateIntake()) return terminateIntake()
            val intakeResult = storageCanHandleIntake()

                if (doTerminateIntake()) return terminateIntake()
            if (!updateAfterInput(intakeResult))  //  Safe updating after intake
                intakeResult.Set(IntakeResult.FAIL_UNKNOWN, IntakeResult.Name.FAIL_UNKNOWN)

            safeResumeRequestLogic(intakeResult.Name())
            return intakeResult.Name()
        }

        val intakeFail = IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        safeResumeRequestLogic(intakeFail)
        return intakeFail
    }
    private fun storageCanHandleIntake(): IntakeResult
    {
        if (_ballCount.get() < REAL_SLOT_COUNT) return IntakeResult(
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

            delay(INTAKE_RACE_CONDITION_DELAY)
            return _intakeRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private suspend fun noIntakeRaceConditionProblems(): Boolean
    {
        _intakeRunStatus.SafeResetTermination()

        return !intakeRaceConditionIsPresent()
    }
    private fun updateAfterInput(intakeResult: IntakeResult): Boolean
    {
        if (intakeResult.DidFail()) return false

        _ballCount.set(_ballCount.get() + 1)
        return true
    }

    private fun doTerminateIntake(): Boolean
    {
        return _intakeRunStatus.TerminationId() == RunStatus.DO_TERMINATE
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
    fun switchTerminateIntake()
    {
        _intakeRunStatus.DoTerminate()
    }



    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        if (_ballCount.get() <= 0) return RequestResult.Name.FAIL_IS_EMPTY

        handleRequestRaceCondition()
        if (doTerminateRequest()) return terminateRequest()

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
        while (i < REAL_SLOT_COUNT)
        {
            if (doTerminateRequest()) return terminateRequest()
            shootingResult = shootClosest(shootingResult)
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
            waitForShotFiredEvent()
            return updateAfterRequest()
        }
    }
    private suspend fun requestRaceConditionIsPresent(): Boolean
    {
        if (_requestRunStatus.IsActive())
        {
            forceStopIntake()

            delay(REQUEST_RACE_CONDITION_DELAY)
            return _requestRunStatus.IsUsedByAnotherProcess()
        }
        return true
    }
    private suspend fun handleRequestRaceCondition()
    {
        _requestRunStatus.SafeResetTermination()

        while (requestRaceConditionIsPresent())
            delay(REQUEST_RACE_CONDITION_DELAY)
    }

    private fun doTerminateRequest(): Boolean
    {
        return _requestRunStatus.TerminationId() == RunStatus.DO_TERMINATE
    }
    private fun terminateRequest(): RequestResult.Name
    {
        _requestRunStatus.SetTermination(
            RunStatus.IS_TERMINATED,
            RunStatus.TerminationStatus.IS_TERMINATED
        )

        val requestFail = RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
        safeResumeIntakeLogic(requestFail)
        return requestFail
    }
    fun switchTerminateRequest()
    {
        _requestRunStatus.DoTerminate()
    }



    private fun forceStopIntake()
    {
        _intakeRunStatus.Set(
            RunStatus.USED_BY_ANOTHER_PROCESS,
            RunStatus.Name.USED_BY_ANOTHER_PROCESS
        )
    }
    private fun safeResumeIntakeLogic(requestResult: RequestResult.Name)
    {
        if (_intakeRunStatus.IsUsedByAnotherProcess())
            _intakeRunStatus.SetActive()

        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageFinishedEveryRequestEvent(requestResult))
    }

    private fun forceStopRequest()
    {
        _requestRunStatus.Set(
            RunStatus.USED_BY_ANOTHER_PROCESS,
            RunStatus.Name.USED_BY_ANOTHER_PROCESS
        )
    }
    private fun safeResumeRequestLogic(intakeResult: IntakeResult.Name)
    {
        if (_requestRunStatus.IsUsedByAnotherProcess())
            _requestRunStatus.SetActive()

        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageFinishedIntakeEvent(intakeResult))
    }



    fun shotWasFired()
    {
        _shotWasFired.set(true)
    }
    private suspend fun waitForShotFiredEvent()
    {
        ThreadedEventBus.LAZY_INSTANCE.invoke(StorageRequestIsReadyEvent())

        while (!_shotWasFired.get()) delay(DELAY_FOR_EVENT_AWAITING)
        _shotWasFired.set(false)
    }



    fun ballCount(): Int
    {
        return _ballCount.get()
    }



    fun trySafeStart()
    {
        if (_intakeRunStatus.IsInactive())
            _intakeRunStatus.SetActive()
        if (_requestRunStatus.IsInactive())
            _requestRunStatus.SetActive()

        _hwStreamStorage.start()
    }
    suspend fun safeStart(): Boolean
    {
        while (!_intakeRunStatus.IsInactive())
            delay(DELAY_FOR_EVENT_AWAITING)
        _intakeRunStatus.SetActive()

        while (!_requestRunStatus.IsInactive())
            delay(DELAY_FOR_EVENT_AWAITING)
        _requestRunStatus.SetActive()

        _hwStreamStorage.start()

        return true
    }

    suspend fun safeStop(): Boolean
    {
        _intakeRunStatus.DoTerminate()
        while (!_intakeRunStatus.IsTerminated())
            delay(DELAY_FOR_EVENT_AWAITING)
        _intakeRunStatus.SetInactive()

        _requestRunStatus.DoTerminate()
        while (!_intakeRunStatus.IsTerminated())
            delay(DELAY_FOR_EVENT_AWAITING)
        _intakeRunStatus.SetInactive()

        _hwStreamStorage.stop()

        return true
    }
    fun forceStop()
    {
        _intakeRunStatus.SetInactive()
        _intakeRunStatus.DoTerminate()

        _requestRunStatus.SetInactive()
        _requestRunStatus.DoTerminate()

        _hwStreamStorage.stop()
    }



    fun linkHardware()
    {
        _hwStreamStorage = HwStreamStorage("")
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hwStreamStorage)
    }
}