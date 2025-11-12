package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlinx.coroutines.delay
import org.woen.hotRun.HotRun
import java.util.concurrent.atomic.AtomicReference

import woen239.enumerators.RunStatus

import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads

import org.woen.modules.scoringSystem.storage.StorageOpenTurretGateEvent
import org.woen.modules.scoringSystem.storage.StorageCloseTurretGateEvent
import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent

import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_ONE_BALL_PUSHING_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_MAX_SERVO_POSITION_CHANGE
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_HARDWARE_REQUEST_FREQUENCY
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_BALL_TO_PUSHER_ALIGNMENT_MS



class HwSortingManager
{
    private val _hwSorting = HwSorting()
    private val _hwSensors = HwSortingSensors()

    private val _runStatus = RunStatus(RunStatus.USED_BY_ANOTHER_PROCESS,
                      RunStatus.Name.USED_BY_ANOTHER_PROCESS)
    val isAwaitingIntake = AtomicReference(false)



    constructor()
    {
        _hwSensors.colorSensorsTriggerAutoIntakeEvent += {
            if (isAwaitingIntake.get())
            {
                ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGetReadyForIntakeEvent(it))

                ThreadedTelemetry.LAZY_INSTANCE.log("")
                ThreadedTelemetry.LAZY_INSTANCE.log("COLOR SENSORS - START INTAKE")
            }
        }

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(StorageOpenTurretGateEvent::class, {
            _hwSorting.openGate()
        } )

        ThreadedEventBus.Companion.LAZY_INSTANCE.subscribe(StorageCloseTurretGateEvent::class, {
            _hwSorting.closeGate()
        } )

        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSorting)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            isAwaitingIntake.set(true)
        }
    }



    fun safeStart(): Boolean
    {
        if (_runStatus.IsActive()) return true

        val startCondition = _runStatus.IsInactive()
        if (startCondition)
        {
            _runStatus.SetActive()
            _hwSorting.startBeltMotor()
        }

        return startCondition
    }
    suspend fun forceSafeStart()
    {
        while (!safeStart())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }
    fun safeStop(): Boolean
    {
        if (_runStatus.IsInactive()) return true  //  Already stopped

        val stopCondition = _runStatus.IsActive()
        if (stopCondition)
        {
            _runStatus.SetInactive()
            _hwSorting.stopBeltMotor()
        }

        return stopCondition
    }
    fun forceStop()
    {
        _runStatus.SetInactive()
        _hwSorting.stopBeltMotor()
    }
    suspend fun forceSafeStop()
    {
        while (!safeStop())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }

    fun safePause(): Boolean
    {
        if (_runStatus.IsUsedByAnotherProcess()) return true  //  Already paused

        val pauseCondition = _runStatus.IsActive()
        if (pauseCondition)
        {
            _runStatus.Set(
                RunStatus.USED_BY_ANOTHER_PROCESS,
                RunStatus.Name.USED_BY_ANOTHER_PROCESS
            )
            _hwSorting.stopBeltMotor()
        }

        return pauseCondition
    }
    suspend fun forceSafePause()
    {
        while (!safePause())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }

    fun safeResume(): Boolean
    {
        if (_runStatus.IsActive()) return true  //  Already active

        val resumeCondition = _runStatus.IsUsedByAnotherProcess()  //  NOT INACTIVE
        if (resumeCondition)
        {
            _runStatus.SetActive()
            _hwSorting.startBeltMotor()
        }

        return resumeCondition
    }
    suspend fun forceSafeResume()
    {
        while (!safeResume())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }

    fun safeReverse(): Boolean
    {
        if (_runStatus.IsActive()) return true  //  Already active

        val resumeCondition = _runStatus.IsUsedByAnotherProcess()  //  NOT INACTIVE
        if (resumeCondition)
        {
            _runStatus.SetActive()
            _hwSorting.reverseBeltMotor()
        }

        return resumeCondition
    }
    suspend fun forceSafeReverse()
    {
        while (!safeReverse())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }



    fun openTurretGate()  = _hwSorting.openTurretGate()
    fun closeTurretGate() = _hwSorting.closeTurretGate()



    suspend fun hwLaunchLastBall()
    {
        _hwSorting.openLaunch()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        _hwSorting.closeLaunch()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
    }
    suspend fun hwRotateBeltCW(timeMs: Long)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HW BELT IS MOVING")
        forceSafeResume()
        delay(timeMs)
        forceSafePause()
    }
    suspend fun hwRotateMobileSlotsCW()
    {
        _hwSorting.closeGate()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        forceSafeReverse()
        delay(DELAY_FOR_BALL_TO_PUSHER_ALIGNMENT_MS)
        forceSafePause()
        _hwSorting.openGate()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        _hwSorting.openPush()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)


        _hwSorting.closeGate()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        _hwSorting.closePush()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
    }
    suspend fun hwRotateFallSlotCW()
    {
        isAwaitingIntake.set(false)
        _hwSorting.openFall()

        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE
                + DELAY_FOR_ONE_BALL_PUSHING_MS)

        _hwSorting.closeFall()

        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        isAwaitingIntake.set(true)
    }
}