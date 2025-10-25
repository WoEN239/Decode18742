package org.woen.modules.storage.stream


import kotlinx.coroutines.delay
import barrel.enumerators.RunStatus

import android.annotation.SuppressLint

import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

import org.woen.modules.storage.GiveNextRequest
import org.woen.modules.storage.TerminateIntakeEvent
import org.woen.modules.storage.TerminateRequestEvent

import org.woen.telemetry.Configs.STORAGE.REAL_SLOT_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_EVENT_AWAITING
import org.woen.telemetry.Configs.STORAGE.INTAKE_RACE_CONDITION_DELAY
import org.woen.telemetry.Configs.STORAGE.REQUEST_RACE_CONDITION_DELAY



import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads



class StreamStorage
{
    private var _ballCount: Int = 0
    private var _shotWasFired = false

    private var _intakeRunStatus  = RunStatus()
    private var _requestRunStatus = RunStatus()
    private var _hwStreamStorage  = HwStreamStorage("")



    @SuppressLint("SuspiciousIndentation")
    suspend fun handleIntake(): IntakeResult.Name
    {
        if (_ballCount >= 3) return IntakeResult.Name.FAIL_STORAGE_IS_FULL

        if (noIntakeRaceConditionProblems())
        {
                if (doTerminateIntake()) return terminateIntake()
            val intakeResult = storageCanHandleIntake()

                if (doTerminateIntake()) return terminateIntake()
            if (!updateAfterInput(intakeResult))  //  Safe updating after intake
                intakeResult.Set(IntakeResult.FAIL_UNKNOWN, IntakeResult.Name.FAIL_UNKNOWN)

            safeResumeRequestLogic()
            return intakeResult.Name()
        }

        safeResumeRequestLogic()
        return IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
    }
    private fun storageCanHandleIntake(): IntakeResult
    {
        if (_ballCount < REAL_SLOT_COUNT) return IntakeResult(
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

        _ballCount++
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

        safeResumeRequestLogic()
        return IntakeResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }



    suspend fun shootEntireDrumRequest(): RequestResult.Name
    {
        if (_ballCount <= 0) return RequestResult.Name.FAIL_IS_EMPTY

        handleRequestRaceCondition()
        if (doTerminateRequest()) return terminateRequest()

        val requestResult = shootEverything()

        safeResumeIntakeLogic()
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
        _ballCount--

        return if (_ballCount > 0)
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
    private suspend fun waitForShotFiredEvent()
    {
        while (!_shotWasFired) delay(DELAY_FOR_EVENT_AWAITING)
        _shotWasFired = false  //!  Maybe improve this later
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

        safeResumeIntakeLogic()
        return RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED
    }





    fun forceStopIntake()
    {
        _intakeRunStatus.Set(
            RunStatus.USED_BY_ANOTHER_PROCESS,
            RunStatus.Name.USED_BY_ANOTHER_PROCESS
        )
    }
    private fun safeResumeIntakeLogic()
    {
        if (_intakeRunStatus.IsUsedByAnotherProcess())
            _intakeRunStatus.Set(
                RunStatus.ACTIVE,
                RunStatus.Name.ACTIVE
            )
    }

    fun forceStopRequest()
    {
        _requestRunStatus.Set(
            RunStatus.USED_BY_ANOTHER_PROCESS,
            RunStatus.Name.USED_BY_ANOTHER_PROCESS
        )
    }
    private fun safeResumeRequestLogic()
    {
        if (_requestRunStatus.IsUsedByAnotherProcess())
            _requestRunStatus.Set(RunStatus.ACTIVE, RunStatus.Name.ACTIVE)
    }





    fun ballCount(): Int
    {
        return _ballCount
    }

    fun safeStart()
    {
        if (_intakeRunStatus.IsInactive())
            _intakeRunStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE)
        if (_requestRunStatus.IsInactive())
            _requestRunStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE)

        _hwStreamStorage.start()
    }

    fun safeStop(): Boolean
    {
        _intakeRunStatus.DoTerminate()
        while (_intakeRunStatus.IsTerminated())
            _intakeRunStatus.SetInactive()

        _requestRunStatus.DoTerminate()
        while (_intakeRunStatus.IsTerminated())
            _intakeRunStatus.SetInactive()

        _hwStreamStorage.stop()

        return true
    }
    fun forceStop()
    {
        _intakeRunStatus.SetInactive()
        _requestRunStatus.SetInactive()

        _hwStreamStorage.stop()
    }



    init
    {
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(TerminateIntakeEvent::class, {
            _intakeRunStatus.DoTerminate()
        } )
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(TerminateRequestEvent::class, {
            _requestRunStatus.DoTerminate()
        } )
        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(GiveNextRequest::class, {
            _shotWasFired = true
        } )


        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hwStreamStorage)
    }
}