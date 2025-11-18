package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import java.util.concurrent.atomic.AtomicReference

import woen239.enumerators.RunStatus

import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.HardwareThreads

import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent
import org.woen.modules.scoringSystem.storage.StorageOpenTurretGateEvent
import org.woen.modules.scoringSystem.storage.StorageCloseTurretGateEvent
import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent

import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_BETWEEN_INTAKES_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_HARDWARE_REQUEST_FREQUENCY



class HwSortingManager
{
    private val _hwSorting = HwSorting()
    private val _hwSensors = HwSortingSensors()

    private val _runStatus = RunStatus(RunStatus.USED_BY_ANOTHER_PROCESS,
                                       RunStatus.Name.USED_BY_ANOTHER_PROCESS)
    val isAwaitingIntake   = AtomicReference(false)



    constructor()
    {
        _hwSensors.colorSensorsTriggerAutoIntakeEvent += {
            if (isAwaitingIntake.get())
            {
                ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {

                    val storageCanHandleInput = ThreadedEventBus.LAZY_INSTANCE.invoke(
                        BallCountInStorageEvent()).count < MAX_BALL_COUNT

                    if (storageCanHandleInput)
                    {
                        ThreadedEventBus.LAZY_INSTANCE.invoke(
                            StorageGetReadyForIntakeEvent(it))

                        ThreadedTelemetry.LAZY_INSTANCE.log("")
                        ThreadedTelemetry.LAZY_INSTANCE.log("COLOR SENSORS - START INTAKE")

                        delay(DELAY_BETWEEN_INTAKES_MS)

                        resumeAwaitingEating(true)
                    }
                    else
                    {
                        stopAwaitingEating(true)
                        delay(DELAY_BETWEEN_INTAKES_MS)
                    }
                }
            }
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(StorageOpenTurretGateEvent::class, {
            openTurretGate()
        } )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(StorageCloseTurretGateEvent::class, {
            closeTurretGate()
        } )

        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSorting)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSensors)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                resumeAwaitingEating(true)
            }
        }
    }



    suspend fun resumeAwaitingEating(resumeBelts: Boolean)
    {
        isAwaitingIntake.set(true)
        //if (resumeBelts) forceSafeSlowResumeBelts()
    }
    suspend fun stopAwaitingEating(stopBelts: Boolean)
    {
        isAwaitingIntake.set(false)
        if (stopBelts) forceSafePauseBelts()
    }
    suspend fun fullCalibrate()
    {
        _hwSorting.fullCalibrate()

        while (!_hwSorting.gateServo.atTargetAngle
            || !_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.turretGateServo.atTargetAngle)
                delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }
    suspend fun openGate()
    {
        _hwSorting.openGate()

        while (!_hwSorting.gateServo.atTargetAngle)
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }
    suspend fun closeGate()
    {
        _hwSorting.closeGate()

        while (!_hwSorting.gateServo.atTargetAngle)
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }
    suspend fun openPush()
    {
        _hwSorting.openPush()

        while (!_hwSorting.pushServo.atTargetAngle)
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }
    suspend fun closePush()
    {
        _hwSorting.closePush()

        while (!_hwSorting.pushServo.atTargetAngle)
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }


    suspend fun openTurretGate()
    {
        _hwSorting.openTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }
    suspend fun closeTurretGate()
    {
        _hwSorting.closeTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }


    fun safeStartBelts(): Boolean
    {
        if (_runStatus.IsActive()) return true

        val startCondition = _runStatus.IsInactive()
        if (startCondition)
        {
            _runStatus.SetActive()
            _hwSorting.startBeltMotors()
        }

        return startCondition
    }
    suspend fun forceSafeStartBelts()
    {
        while (!safeStartBelts())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }
    fun safeStopBelts(): Boolean
    {
        if (_runStatus.IsInactive()) return true  //  Already stopped

        val stopCondition = _runStatus.IsActive()
        if (stopCondition)
        {
            _runStatus.SetInactive()
            _hwSorting.stopBeltMotors()
        }

        return stopCondition
    }
    fun forceStopBelts()
    {
        _runStatus.SetInactive()
        _hwSorting.stopBeltMotors()
    }
    suspend fun forceSafeStopBelts()
    {
        while (!safeStopBelts())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }

    fun safePauseBelts(): Boolean
    {
        if (_runStatus.IsUsedByAnotherProcess()) return true  //  Already paused

        val pauseCondition = _runStatus.IsActive()
        if (pauseCondition)
        {
            _runStatus.Set(
                RunStatus.USED_BY_ANOTHER_PROCESS,
                RunStatus.Name.USED_BY_ANOTHER_PROCESS
            )
            _hwSorting.stopBeltMotors()
        }

        return pauseCondition
    }
    suspend fun forceSafePauseBelts()
    {
        while (!safePauseBelts())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }

    fun safeResumeBelts(): Boolean
    {
        if (_runStatus.IsActive()) return true  //  Already active

        val resumeCondition = _runStatus.IsUsedByAnotherProcess()  //  NOT INACTIVE
        if (resumeCondition)
        {
            _runStatus.SetActive()
            _hwSorting.startBeltMotors()
        }

        return resumeCondition
    }
    suspend fun forceSafeResumeBelts()
    {
        while (!safeResumeBelts())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }

    fun safeSlowResumeBelts(): Boolean
    {
        if (_runStatus.IsActive()) return true  //  Already active

        val resumeCondition = _runStatus.IsUsedByAnotherProcess()  //  NOT INACTIVE
        if (resumeCondition)
        {
            _runStatus.SetActive()
            _hwSorting.slowStartBeltMotors()
        }

        return resumeCondition
    }
    suspend fun forceSafeSlowResumeBelts()
    {
        while (!safeSlowResumeBelts())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }

    fun safeReverseBelts(): Boolean
    {
        if (_runStatus.IsActive()) return true  //  Already active

        val resumeCondition = _runStatus.IsUsedByAnotherProcess()  //  NOT INACTIVE
        if (resumeCondition)
        {
            _runStatus.SetActive()
            _hwSorting.reverseBeltMotors()
        }

        return resumeCondition
    }
    suspend fun forceSafeReverseBelts()
    {
        while (!safeReverseBelts())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }



    suspend fun hwRotateBeltForward(timeMs: Long)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HW belt is moving")
        stopAwaitingEating(false)
        forceSafeResumeBelts()
        delay(timeMs)
        forceSafePauseBelts()
    }
    suspend fun hwReverseBelt(timeMs: Long)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HW belt is reversing")

        stopAwaitingEating(false)
        forceSafeReverseBelts()
        delay(timeMs)
        forceSafePauseBelts()
    }
    suspend fun hwRotateMobileSlots()
    {
        stopAwaitingEating(true)
        closeTurretGate()
        forceSafePauseBelts()

        openGate()
        openPush()

        closeGate()
        closePush()
    }
}