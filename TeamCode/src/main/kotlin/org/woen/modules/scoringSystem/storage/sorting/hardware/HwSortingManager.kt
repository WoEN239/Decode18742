package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

import woen239.enumerators.RunStatus

import org.woen.hotRun.HotRun
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.HardwareThreads

import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent
import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent

import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT
import org.woen.telemetry.Configs.STORAGE.DELAY_BETWEEN_INTAKES_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_HARDWARE_REQUEST_FREQUENCY
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_SORTING_REALIGNING_FORWARD_MS
import org.woen.telemetry.Configs.STORAGE.DELAY_FOR_SORTING_REALIGNING_REVERSE_MS



class HwSortingManager
{
    private val _hwSorting = HwSorting()
    //private val _hwSensors = HwSortingSensors()

    val isAwaitingIntake   = AtomicBoolean(false)



    constructor()
    {
        subscribeAndHandleColorSensorsEvent()
        addDevices()

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            resetParametersAndLogicToDefault()
        }
    }

    private fun subscribeAndHandleColorSensorsEvent()
    {
//        _hwSensors.colorSensorsTriggerAutoIntakeEvent +=
//        {
//            if (isAwaitingIntake.get())
//            {
//                ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
//
//                    val storageCanHandleInput = ThreadedEventBus.LAZY_INSTANCE.invoke(
//                        BallCountInStorageEvent()).count + 1 < MAX_BALL_COUNT
//
//                    if (storageCanHandleInput)
//                    {
//                        ThreadedEventBus.LAZY_INSTANCE.invoke(
//                            StorageGetReadyForIntakeEvent(it))
//
//                        ThreadedTelemetry.LAZY_INSTANCE.log("\nCOLOR SENSORS - START INTAKE")
//                        delay(DELAY_BETWEEN_INTAKES_MS)
//
//                        resumeAwaitingEating()
//                    }
//                    else
//                    {
//                        stopAwaitingEating(true)
//                        delay(DELAY_BETWEEN_INTAKES_MS)
//                    }
//                }
//            }
//        }
    }
    private fun addDevices()
    {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSorting)
        //HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSensors)
    }
    fun resetParametersAndLogicToDefault()
    {
        isAwaitingIntake.set(false)

        ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            fullCalibrate()
            resumeAwaitingEating()
        }
    }



    fun resumeAwaitingEating() = isAwaitingIntake.set(true)
    fun stopAwaitingEating(stopBelts: Boolean)
    {
        isAwaitingIntake.set(false)
        if (stopBelts) stopBelts()
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



    fun startBelts()   = _hwSorting.startBeltMotors()
    fun reverseBelts() = _hwSorting.reverseBeltMotors()

    fun stopBelts() = _hwSorting.stopBeltMotors()



    suspend fun hwRotateBeltForward(timeMs: Long)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HW belt is moving")

        stopAwaitingEating(false)
        startBelts()
        delay(timeMs)
        stopBelts()

        ThreadedTelemetry.LAZY_INSTANCE.log(("HW belt stopped"))
    }
    suspend fun hwReverseBelts(timeMs: Long)
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HW belt is reversing")

        stopAwaitingEating(false)
        startBelts()
        delay(timeMs)
        stopBelts()

        ThreadedTelemetry.LAZY_INSTANCE.log(("HW belt stopped"))
    }
    suspend fun hwRotateMobileSlots()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("rotating mobile slot")
        stopAwaitingEating(true)
        closeTurretGate()

        startBelts()
        delay(DELAY_FOR_SORTING_REALIGNING_FORWARD_MS)
        reverseBelts()
        delay(DELAY_FOR_SORTING_REALIGNING_REVERSE_MS)
        stopBelts()

        openGate()
        openPush()

        closeGate()
        closePush()
    }
}