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

import org.woen.modules.scoringSystem.storage.StorageOpenTurretGateEvent
import org.woen.modules.scoringSystem.storage.StorageCloseTurretGateEvent
import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent

import org.woen.telemetry.Configs.STORAGE.DELAY_BETWEEN_INTAKES_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_HARDWARE_REQUEST_FREQUENCY

import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_ONE_BALL_PUSHING_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_MAX_SERVO_POSITION_CHANGE
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
                isAwaitingIntake.set(false)
                ThreadedEventBus.LAZY_INSTANCE.invoke(StorageGetReadyForIntakeEvent(it))

                ThreadedTelemetry.LAZY_INSTANCE.log("")
                ThreadedTelemetry.LAZY_INSTANCE.log("COLOR SENSORS - START INTAKE")

                ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                    delay(DELAY_BETWEEN_INTAKES_MS)
                }
                isAwaitingIntake.set(true)
            }
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(StorageOpenTurretGateEvent::class, {
            openGate()
        } )

        ThreadedEventBus.LAZY_INSTANCE.subscribe(StorageCloseTurretGateEvent::class, {
            closeGate()
        } )

        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSorting)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            isAwaitingIntake.set(true)
        }
    }



    suspend fun fullCalibrate()
    {
        _hwSorting.fullCalibrate()

        while (!_hwSorting.gateServo.atTargetAngle
            || !_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.fallServo.atTargetAngle
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
    suspend fun openFall()
    {
        _hwSorting.openFall()

        while (!_hwSorting.fallServo.atTargetAngle)
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }
    suspend fun closeFall()
    {
        _hwSorting.closeFall()

        while (!_hwSorting.fallServo.atTargetAngle)
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


    fun safeStart(): Boolean
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
            _hwSorting.stopBeltMotors()
        }

        return stopCondition
    }
    fun forceStop()
    {
        _runStatus.SetInactive()
        _hwSorting.stopBeltMotors()
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
            _hwSorting.stopBeltMotors()
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
            _hwSorting.startBeltMotors()
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
            _hwSorting.reverseBeltMotors()
        }

        return resumeCondition
    }
    suspend fun forceSafeReverse()
    {
        while (!safeReverse())
            delay(DELAY_FOR_HARDWARE_REQUEST_FREQUENCY)
    }



    suspend fun hwRotateBeltCW(timeMs: Long)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HW belt is moving")
        forceSafeResume()
        delay(timeMs)
        forceSafePause()
    }
    suspend fun hwReverseBelt(timeMs: Long)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HW belt is reversing")
        forceSafeReverse()
        delay(timeMs)
        forceSafePause()
    }
    suspend fun hwRotateMobileSlotsCW()
    {
        closeGate()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        forceSafeReverse()
        delay(DELAY_FOR_BALL_TO_PUSHER_ALIGNMENT_MS)
        forceSafePause()
        openGate()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        openPush()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)


        closeGate()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        closePush()
        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
    }
    suspend fun hwRotateFallSlotCW()
    {
        isAwaitingIntake.set(false)
        openFall()

        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE
                + DELAY_FOR_ONE_BALL_PUSHING_MS)

        closeFall()

        delay(DELAY_FOR_MAX_SERVO_POSITION_CHANGE)
        isAwaitingIntake.set(true)
    }
}