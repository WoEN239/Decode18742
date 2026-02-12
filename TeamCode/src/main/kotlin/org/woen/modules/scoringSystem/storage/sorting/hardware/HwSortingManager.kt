package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.qualcomm.robotcore.util.ElapsedTime

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

import org.woen.telemetry.LogManager
import org.woen.threading.hardware.HardwareThreads

import org.woen.modules.scoringSystem.storage.Alias.EventBusLI
import org.woen.modules.scoringSystem.storage.Alias.SmartCoroutineLI

import org.woen.modules.scoringSystem.storage.FillStorageWithUnknownColorsEvent
import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent

import org.woen.telemetry.configs.Debug
import org.woen.telemetry.configs.Configs.DELAY
import org.woen.telemetry.configs.RobotSettings.SORTING



class HwSortingManager
{
    private val _hwSorting = HwSorting()
    private val _hwSensors = HwSortingSensors()

    val logM = LogManager(
         Debug.HSM_DEBUG_SETTING,
        Debug.HSM_WARNING_SETTING,
         Debug.HSM_DEBUG_LEVELS,
        Debug.HSM_WARNING_LEVELS,
        "HSM")

    val isStoppingBelts  = AtomicBoolean(false)
    val helpPushLastBall = AtomicBoolean(false)

    val canHandleIntake = AtomicBoolean(false)
    val currentPushTime = AtomicLong(0)
    val reinstantiableBeltsTimer = ElapsedTime()



    constructor()
    {
        subscribeToHwEvents()
        addDevices()
    }



    private fun subscribeToHwEvents()
    {
        _hwSensors.colorSensorsDetectedIntakeEvent +=
        {
            if (canHandleIntake.get())
            {
                logM.logMdTag(
                    "Color sensors detected: ${it.color}",
                    "StorageSensors", Debug.HW_LOW)

                EventBusLI.invoke(
                    StorageGetReadyForIntakeEvent(
                        it.color))
            }
        }

        _hwSorting.beltsCurrentPeakedEvent +=
        {
            logM.logMdTag("BELTS - Current peaked", "StorageSensors", Debug.HW_LOW)

            if (!isStoppingBelts.get())
            {
                isStoppingBelts.set(true)
                logM.logMd("BELTS - Initiating current protection", Debug.HW_HIGH)

                if (SORTING.USE_CURRENT_PROTECTION_FOR_STORAGE_BELTS)
//                    EventBusLI.invoke(WaitForTerminateIntakeEvent())

                    if (SORTING.SMART_RECALIBRATE_STORAGE_WITH_CURRENT_PROTECTION)
                    {
                        do
                        {
                            logM.logMd("Belt current protection - " +
                                    "attempting storage recalibration", Debug.HW_LOW)

                            val recalibrateResult = EventBusLI.invoke(
                                FillStorageWithUnknownColorsEvent()
                            ).startingResult
                        }
                        while (SORTING.TRY_RECALIBRATE_WITH_CURRENT_UNTIL_SUCCESS
                            && !recalibrateResult)
                    }

                isStoppingBelts.set(false)
            }
        }
    }
    private fun addDevices()
    {
        logM.logMd("INITIALISATION - Adding HwDevices", Debug.HW_HIGH)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSorting)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSensors)
    }
    suspend fun resetParametersAndLogicToDefault()
    {
        stopAwaitingEating(false)
        fullCalibrate()

        logM.reset(
            Debug.HSM_DEBUG_SETTING,
            Debug.HSM_WARNING_SETTING,
            Debug.HSM_DEBUG_LEVELS,
            Debug.HSM_WARNING_LEVELS,
            "HSM")
    }


    fun resumeAwaitingEating() = canHandleIntake.set(true)
    fun stopAwaitingEating(stopBelts: Boolean)
    {
        logM.logMd("Stopped auto intake awaiting", Debug.GENERIC)

        canHandleIntake.set(false)
        if (stopBelts) stopBelts()
    }


    suspend fun fullCalibrate()
    {
        logM.logMd("STARTED full calibration", Debug.HW_HIGH)
        _hwSorting.fullCalibrate()

        while (!_hwSorting.gateServo.atTargetAngle
            || !_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.launchServo.atTargetAngle
            || !_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("Full calibration completed", Debug.HW_HIGH)
    }


    suspend fun openGate()
    {
        logM.logMd("Started OPENING sorting gate", Debug.HW)
        _hwSorting.openGate()

        while (!_hwSorting.gateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("OPENED sorting gate", Debug.HW)
    }
    suspend fun openPush()
    {
        logM.logMd("Started OPENING push", Debug.HW)
        _hwSorting.openPush()

        while (!_hwSorting.pushServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("OPENED push", Debug.HW)
    }
//    suspend fun closePush()
//    {
//        logM.logMd("Started CLOSING push", HARDWARE)
//        _hwSorting.closePush()
//
//        while (!_hwSorting.pushServo.atTargetAngle)
//            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)
//
//        logM.logMd("CLOSED push", HARDWARE)
//    }
    suspend fun closeGateWithPush()
    {
        logM.logMd("Started CLOSING sorting gate & push", Debug.HW)
        _hwSorting.closeGate()
        _hwSorting.closePush()

        while (!_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.gateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("CLOSED sorting gate & push", Debug.HW)
    }


    suspend fun openLaunch()
    {
        logM.logMd("Started OPENING kick", Debug.HW)
        _hwSorting.openLaunch()

        while (!_hwSorting.launchServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("OPENED kick", Debug.HW)
    }
    suspend fun closeLaunch()
    {
        logM.logMd("Started CLOSING kick", Debug.HW)
        _hwSorting.closeLaunch()

        while (!_hwSorting.launchServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("CLOSED kick", Debug.HW)
    }


    suspend fun openTurretGate()
    {
        logM.logMd("Started OPENING turret gate", Debug.HW)
        _hwSorting.openTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("OPENED turret gate successfully", Debug.HW)
    }
    suspend fun closeTurretGate()
    {
        logM.logMd("Started CLOSING turret gate", Debug.HW)
        _hwSorting.closeTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("CLOSED turret gate successfully", Debug.HW)
    }


    fun shootStartBelts()
    {
        logM.logMd("SHOOT Started hw belts", Debug.HW)
        _hwSorting.shootStartBeltMotors()
    }
    fun slowStartBelts()
    {
        logM.logMd("SLOW Started hw belts", Debug.HW)
        _hwSorting.slowStartBeltMotors()
    }

    fun startBelts()
    {
        logM.logMd("Started hw belts", Debug.HW)
        _hwSorting.startBeltMotors()
    }
    fun reverseBelts()
    {
        logM.logMd("Reversing hw belts", Debug.HW)
        _hwSorting.reverseBeltMotors()
    }
    fun stopBelts()
    {
        logM.logMd("Stopped hw belts", Debug.HW)
        _hwSorting.stopBeltMotors()
    }


    suspend fun reinstantiableForwardBeltsTime(timeMs: Long, firstInstance: Boolean = false)
    {
        val pushTime =
            if (firstInstance) timeMs
            else max(
                timeMs,
                currentPushTime.get()
                        - reinstantiableBeltsTimer
                            .milliseconds().toLong())

        reinstantiableBeltsTimer.reset()
        currentPushTime.set(pushTime)
        logM.logMd("Chosen time period: $pushTime", Debug.HW)

        val pushing = SmartCoroutineLI.launch {

            startBelts()
            delay(pushTime)

            if (reinstantiableBeltsTimer.milliseconds()
                > currentPushTime.get())
                stopBelts()
        }

        while (!pushing.isCompleted)
            delay(DELAY.EVENT_AWAITING_MS)
    }


    suspend fun forwardBeltsTime(timeMs: Long)
    {
        logM.logMd("Chosen time period: $timeMs", Debug.HW)
        startBelts()

        delay(timeMs)
        stopBelts()
    }
    suspend fun reverseBeltsTime(timeMs: Long)
    {
        logM.logMd("Chosen time period: $timeMs", Debug.HW)
        reverseBelts()

        delay(timeMs)
        stopBelts()
    }


    suspend fun rotateMobileSlot()
    {
        logM.logMd("ROTATING MOBILE SLOT", Debug.HW_HIGH)
        stopAwaitingEating(true)

        closeLaunch()
        closeTurretGate()

//        slowStartBelts()
//        delay(DELAY.SORTING_REALIGNING_FORWARD_MS)
        reverseBeltsTime(DELAY.SORTING_REALIGNING_REVERSE_MS)

        openGate()
        openPush()

        reverseBelts()
        delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS * 8)

        closeGateWithPush()
        forwardBeltsTime(DELAY.SORTING_REALIGNING_FORWARD_MS)
    }


    suspend fun smartPushNextBall()
    {
        logM.logMd("STARTED Smart ball push for shot", Debug.HW_HIGH)
        shootStartBelts()

        if (helpPushLastBall.get())
        {
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS * 4)
            openLaunch()
            helpPushLastBall.set(false)
        }
    }
    suspend fun pushLastBallFast()
    {
        openLaunch()
        closeLaunch()
        stopBelts()
        helpPushLastBall.set(false)
    }
}