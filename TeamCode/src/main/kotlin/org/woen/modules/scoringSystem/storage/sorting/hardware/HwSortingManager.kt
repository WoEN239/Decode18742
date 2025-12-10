package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

import org.woen.hotRun.HotRun
import org.woen.threading.ThreadManager
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.HardwareThreads

import org.woen.telemetry.Configs.DELAY



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
//                        ThreadedTelemetry.LAZY_INSTANCE.log("", "COLOR SENSORS - START INTAKE")
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
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: STARTED full calibration")
        _hwSorting.fullCalibrate()

        while (!_hwSorting.gateServo.atTargetAngle
            || !_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.turretGateServo.atTargetAngle)
                delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Full calibration completed")
    }
    suspend fun openGate()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Started OPENING sorting gate")
        _hwSorting.openGate()

        while (!_hwSorting.gateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: OPENED sorting gate")
    }
    suspend fun openPush()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Started OPENING push")
        _hwSorting.openPush()

        while (!_hwSorting.pushServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: OPENED push")
    }
    suspend fun closeGateWithPush()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Started CLOSING sorting gate & push")
        _hwSorting.closeGate()
        _hwSorting.closePush()

        while (!_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.gateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: CLOSED sorting gate & push")
    }


    suspend fun openTurretGate()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Started OPENING turret gate")
        _hwSorting.openTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: OPENED turret gate successfully")
    }
    suspend fun closeTurretGate()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Started CLOSING turret gate")
        _hwSorting.closeTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: CLOSED turret gate successfully")
    }



    fun startBelts()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Started hw belts")
        _hwSorting.startBeltMotors()
    }
    fun reverseBelts()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Reversing hw belts")
        _hwSorting.reverseBeltMotors()
    }

    fun stopBelts()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Stopped hw belts")
        _hwSorting.stopBeltMotors()
    }



    suspend fun hwForwardBeltsTime(timeMs: Long)
    {
        stopAwaitingEating(false)
        startBelts()
        delay(timeMs)
        stopBelts()
    }
    suspend fun hwReverseBeltsTime(timeMs: Long)
    {
        stopAwaitingEating(false)
        reverseBelts()
        delay(timeMs)
        stopBelts()
    }
    suspend fun hwRotateMobileSlot()
    {
        ThreadedTelemetry.LAZY_INSTANCE.log("HWSMM: Rotating mobile slot")
        stopAwaitingEating(true)
        closeTurretGate()

        hwForwardBeltsTime(DELAY.SORTING_REALIGNING_FORWARD_MS)
        hwReverseBeltsTime(DELAY.SORTING_REALIGNING_REVERSE_MS)

        openGate()
        openPush()

        closeGateWithPush()
    }
}