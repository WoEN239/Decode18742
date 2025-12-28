package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

import org.woen.telemetry.LogManager
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads

import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT
import org.woen.modules.scoringSystem.storage.BallCountInStorageEvent

import org.woen.modules.scoringSystem.storage.WaitForTerminateIntakeEvent
import org.woen.modules.scoringSystem.storage.FillStorageWithUnknownColorsEvent
import org.woen.modules.scoringSystem.storage.StorageGetReadyForIntakeEvent
import org.woen.telemetry.Configs.DEBUG_LEVELS.GENERIC_INFO
import org.woen.telemetry.Configs.DEBUG_LEVELS.HARDWARE_LOW
import org.woen.telemetry.Configs.DEBUG_LEVELS.HARDWARE
import org.woen.telemetry.Configs.DEBUG_LEVELS.HARDWARE_HIGH
import org.woen.telemetry.Configs.DEBUG_LEVELS.HSM_DEBUG_LEVELS
import org.woen.telemetry.Configs.DEBUG_LEVELS.HSM_DEBUG_SETTING

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.SORTING_SETTINGS.USE_CURRENT_PROTECTION_FOR_STORAGE_BELTS
import org.woen.telemetry.Configs.SORTING_SETTINGS.TRY_RECALIBRATE_WITH_CURRENT_UNTIL_SUCCESS
import org.woen.telemetry.Configs.SORTING_SETTINGS.SMART_RECALIBRATE_STORAGE_WITH_CURRENT_PROTECTION



class HwSortingManager
{
    private val _hwSorting = HwSorting()
    //private val _hwSensors = HwSortingSensors()

    val logM = LogManager(HSM_DEBUG_SETTING,
        HSM_DEBUG_LEVELS, "HSM")

    val isAwaitingIntake = AtomicBoolean(false)
    val isStoppingBelts  = AtomicBoolean(false)
    val helpPushLastBall = AtomicBoolean(false)



    constructor()
    {
        subscribeToHwEvents()
        addDevices()
    }

    private fun subscribeToHwEvents()
    {
//        _hwSensors.colorSensorsDetectedIntakeEvent +=
//        {
//            logM.logTag("Color sensors sees $it input", "StorageSensors", HARDWARE_LOW)
//
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
//                        logM.logMd("COLOR SENSORS - Start intake", HARDWARE_HIGH)
//                        delay(DELAY.BETWEEN_INTAKES_MS)
//
//                        resumeAwaitingEating()
//                    }
//                    else
//                    {
//                        stopAwaitingEating(true)
//                        delay(DELAY.BETWEEN_INTAKES_MS)
//                    }
//                }
//            }
//        }

        _hwSorting.beltsCurrentPeakedEvent +=
        {
            logM.logTag("BELTS - Current peaked", "StorageSensors", HARDWARE_LOW)

            if (!isStoppingBelts.get())
            {
                isStoppingBelts.set(true)
                logM.logMd("BELTS - Initiating current protection", HARDWARE_HIGH)

                if (USE_CURRENT_PROTECTION_FOR_STORAGE_BELTS)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(WaitForTerminateIntakeEvent())

                if (SMART_RECALIBRATE_STORAGE_WITH_CURRENT_PROTECTION)
                {
                    do
                    {
                        logM.logMd("Belt current protection - attempting storage recalibration", HARDWARE_LOW)

                        val recalibrateResult = ThreadedEventBus.LAZY_INSTANCE.invoke(
                            FillStorageWithUnknownColorsEvent()
                        ).startingResult
                    }
                    while (TRY_RECALIBRATE_WITH_CURRENT_UNTIL_SUCCESS
                        && !recalibrateResult)
                }

                isStoppingBelts.set(false)
            }
        }
    }
    private fun addDevices()
    {
        logM.logMd("INITIALISATION - Adding HwDevices", HARDWARE_HIGH)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSorting)
        //HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSensors)
    }
    fun resetParametersAndLogicToDefault() = stopAwaitingEating(false)



    fun resumeAwaitingEating() = isAwaitingIntake.set(true)
    fun stopAwaitingEating(stopBelts: Boolean)
    {
        logM.logMd("Stopped auto intake awaiting", GENERIC_INFO)
        isAwaitingIntake.set(false)
        if (stopBelts) stopBelts()
    }
    suspend fun fullCalibrate()
    {
        logM.logMd("STARTED full calibration", HARDWARE_HIGH)
        _hwSorting.fullCalibrate()

        while (!_hwSorting.gateServo.atTargetAngle
            || !_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.launchServo.atTargetAngle
            || !_hwSorting.turretGateServo.atTargetAngle)
                delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("Full calibration completed", HARDWARE_HIGH)
    }
    suspend fun openGate()
    {
        logM.logMd("Started OPENING sorting gate", HARDWARE)
        _hwSorting.openGate()

        while (!_hwSorting.gateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("OPENED sorting gate", HARDWARE)
    }
    suspend fun openPush()
    {
        logM.logMd("Started OPENING push", HARDWARE)
        _hwSorting.openPush()

        while (!_hwSorting.pushServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("OPENED push", HARDWARE)
    }
    suspend fun openLaunch()
    {
        logM.logMd("Started OPENING kick", HARDWARE)
        _hwSorting.openLaunch()

        while (!_hwSorting.launchServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("OPENED kick", HARDWARE)
    }
    suspend fun closeLaunch()
    {
        logM.logMd("Started CLOSING kick", HARDWARE)
        _hwSorting.closeLaunch()

        while (!_hwSorting.launchServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("CLOSED kick", HARDWARE)
    }
    suspend fun closeGateWithPush()
    {
        logM.logMd("Started CLOSING sorting gate & push", HARDWARE)
        _hwSorting.closeGate()
        _hwSorting.closePush()

        while (!_hwSorting.pushServo.atTargetAngle
            || !_hwSorting.gateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("CLOSED sorting gate & push", HARDWARE)
    }


    suspend fun openTurretGate()
    {
        logM.logMd("Started OPENING turret gate", HARDWARE)
        _hwSorting.openTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("OPENED turret gate successfully", HARDWARE)
    }
    suspend fun closeTurretGate()
    {
        logM.logMd("Started CLOSING turret gate", HARDWARE)
        _hwSorting.closeTurretGate()

        while (!_hwSorting.turretGateServo.atTargetAngle)
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS)

        logM.logMd("CLOSED turret gate successfully", HARDWARE)
    }



    fun slowStartBelts()
    {
        logM.logMd("SLOW Started hw belts", HARDWARE)
        _hwSorting.slowStartBeltMotors()
    }
    fun startBelts()
    {
        logM.logMd("Started hw belts", HARDWARE)
        _hwSorting.startBeltMotors()
    }
    fun reverseBelts()
    {
        logM.logMd("Reversing hw belts", HARDWARE)
        _hwSorting.reverseBeltMotors()
    }

    fun stopBelts()
    {
        logM.logMd("Stopped hw belts", HARDWARE)
        _hwSorting.stopBeltMotors()
    }



    suspend fun hwForwardBeltsTime(timeMs: Long)
    {
        logM.logMd("Chosen time period: $timeMs", HARDWARE)
        stopAwaitingEating(false)
        startBelts()
        delay(timeMs)
        stopBelts()
    }
    suspend fun hwReverseBeltsTime(timeMs: Long)
    {
        logM.logMd("Chosen time period: $timeMs", HARDWARE)
        stopAwaitingEating(false)
        reverseBelts()
        delay(timeMs)
        stopBelts()
    }
    suspend fun hwRotateMobileSlot()
    {
        logM.logMd("ROTATING MOBILE SLOT", HARDWARE_HIGH)
        stopAwaitingEating(true)
        closeLaunch()
        closeTurretGate()

        //hwForwardBeltsTime(DELAY.SORTING_REALIGNING_FORWARD_MS)
        hwReverseBeltsTime(DELAY.SORTING_REALIGNING_REVERSE_MS)

        openGate()
        openPush()

        reverseBelts()
        delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS * 4)
        closeGateWithPush()
    }

    suspend fun hwSmartPushNextBall()
    {
        logM.logMd("STARTED Smart ball push for shot", HARDWARE_HIGH)
        slowStartBelts()

        if (helpPushLastBall.get())
        {
            openLaunch()
            helpPushLastBall.set(false)
        }
    }
}