package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

import org.woen.telemetry.Configs.DELAY

import org.woen.threading.hardware.HardwareThreads

import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.TelemetryLI
import org.woen.modules.scoringSystem.storage.StopAnyIntakeEvent


class HwSortingManager
{
    private val _hwSorting = HwSorting()
    //private val _hwSensors = HwSortingSensors()

    val isAwaitingIntake   = AtomicBoolean(false)
    val isStoppingBelts    = AtomicBoolean(false)


    constructor()
    {
        subscribeAndHandleColorSensorsEvent()
        addDevices()
    }

    private fun subscribeAndHandleColorSensorsEvent()
    {
//        _hwSensors.colorSensorsTriggerAutoIntakeEvent +=
//        {
//            if (isAwaitingIntake.get())
//            {
//                SmartCoroutineLI.launch {
//
//                    val storageCanHandleInput = EventBusLI.invoke(
//                        BallCountInStorageEvent()).count + 1 < MAX_BALL_COUNT
//
//                    if (storageCanHandleInput)
//                    {
//                        EventBusLI.invoke(StorageGetReadyForIntakeEvent(it))
//
//                        TelemetryLI.log("", "COLOR SENSORS - START INTAKE")
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

        _hwSorting.beltsCurrentPeakedEvent +=
        {
            if (!isStoppingBelts.get())
            {
                isStoppingBelts.set(true)

                EventBusLI.invoke(StopAnyIntakeEvent())

                isStoppingBelts.set(false)
            }
        }
    }
    private fun addDevices()
    {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSorting)
        //HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSensors)
    }
    fun resetParametersAndLogicToDefault() = stopAwaitingEating(false)



    fun resumeAwaitingEating() = isAwaitingIntake.set(true)
    fun stopAwaitingEating(stopBelts: Boolean)
    {
        isAwaitingIntake.set(false)
        if (stopBelts) stopBelts()
    }
    suspend fun fullCalibrate()
    {
        TelemetryLI.log("HWSMM: STARTED full calibration")
        _hwSorting.fullCalibrate()

        while (!_hwSorting.gateServo.atTargetAngle
            || !_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.turretGateServo.atTargetAngle)
                delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        TelemetryLI.log("HWSMM: Full calibration completed")
    }
    suspend fun openGate()
    {
        TelemetryLI.log("HWSMM: Started OPENING sorting gate")
        _hwSorting.openGate()

        while (!_hwSorting.gateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        TelemetryLI.log("HWSMM: OPENED sorting gate")
    }
    suspend fun openPush()
    {
        TelemetryLI.log("HWSMM: Started OPENING push")
        _hwSorting.openPush()

        while (!_hwSorting.pushServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        TelemetryLI.log("HWSMM: OPENED push")
    }
    suspend fun closeGateWithPush()
    {
        TelemetryLI.log("HWSMM: Started CLOSING sorting gate & push")
        _hwSorting.closeGate()
        _hwSorting.closePush()

        while (!_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.gateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        TelemetryLI.log("HWSMM: CLOSED sorting gate & push")
    }


    suspend fun openTurretGate()
    {
        TelemetryLI.log("HWSMM: Started OPENING turret gate")
        _hwSorting.openTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        TelemetryLI.log("HWSMM: OPENED turret gate successfully")
    }
    suspend fun closeTurretGate()
    {
        TelemetryLI.log("HWSMM: Started CLOSING turret gate")
        _hwSorting.closeTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        TelemetryLI.log("HWSMM: CLOSED turret gate successfully")
    }



    fun slowStartBelts()
    {
        TelemetryLI.log("HWSMM: SLOW Started hw belts")
        _hwSorting.slowStartBeltMotors()
    }
    fun startBelts()
    {
        TelemetryLI.log("HWSMM: Started hw belts")
        _hwSorting.startBeltMotors()
    }
    fun reverseBelts()
    {
        TelemetryLI.log("HWSMM: Reversing hw belts")
        _hwSorting.reverseBeltMotors()
    }

    fun stopBelts()
    {
        TelemetryLI.log("HWSMM: Stopped hw belts")
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
        TelemetryLI.log("HWSMM: Rotating mobile slot")
        stopAwaitingEating(true)
        closeTurretGate()

        //hwForwardBeltsTime(DELAY.SORTING_REALIGNING_FORWARD_MS)
        hwReverseBeltsTime(DELAY.SORTING_REALIGNING_REVERSE_MS)

        openGate()
        openPush()

        delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS * 4)
        closeGateWithPush()
    }
}