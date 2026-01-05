package org.woen.modules.scoringSystem.storage.sorting.hardware


import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.enumerators.Ball
import java.util.concurrent.atomic.AtomicBoolean

import org.woen.telemetry.LogManager
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads

import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedGamepad.Companion.createClickDownListener

import org.woen.modules.scoringSystem.storage.Alias.Delay
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
import org.woen.telemetry.Configs.DEBUG_LEVELS.LOGIC_STEPS

import org.woen.telemetry.Configs.DELAY
import org.woen.telemetry.Configs.STORAGE_SENSORS.MIN_TIME_FOR_INTAKE_DETECTION_MS

import org.woen.telemetry.Configs.SORTING_SETTINGS.USE_CURRENT_PROTECTION_FOR_STORAGE_BELTS
import org.woen.telemetry.Configs.SORTING_SETTINGS.TRY_RECALIBRATE_WITH_CURRENT_UNTIL_SUCCESS
import org.woen.telemetry.Configs.SORTING_SETTINGS.SMART_RECALIBRATE_STORAGE_WITH_CURRENT_PROTECTION
import org.woen.telemetry.Configs.STORAGE_SENSORS.MIN_TIME_FOR_INTAKE_NOT_DETECTION_MS



class HwSortingManager
{
    private val _hwSorting = HwSorting()
    private val _hwSensors = HwSortingSensors()

    val logM = LogManager(HSM_DEBUG_SETTING,
        HSM_DEBUG_LEVELS, "HSM")

    val isStoppingBelts   = AtomicBoolean(false)
    val helpPushLastBall  = AtomicBoolean(false)

    val canHandleIntake   = AtomicBoolean(false)
    val autoIntakeTracker = AutoIntakeTracker()

    val intakeDetectedTimer    = ElapsedTime()
    val intakeNotDetectedTimer = ElapsedTime()



    constructor()
    {
        subscribeToHwEvents()
        addDevices()

        ThreadedGamepad.LAZY_INSTANCE.addGamepad1Listener(
            createClickDownListener({ it.ps },   {

                    canHandleIntake.set(true)
        }   )   )
    }

    private fun subscribeToHwEvents()
    {
        _hwSensors.colorSensorsDetectedIntakeEvent +=
        {
            logM.logTag("ColorTr:${it.sensorsId} sees ${it.color} input", "StorageSensors", HARDWARE_LOW)

            val isAwaitingIntake: Boolean
            if (it.color != Ball.Name.NONE)
            {
                intakeNotDetectedTimer.reset()
                logM.logMd("Can handle: ${canHandleIntake.get()}", HARDWARE_LOW)
                logM.logMd("Intake is free: ${autoIntakeTracker.intakeIsFree()}", HARDWARE_LOW)
                isAwaitingIntake = autoIntakeTracker.intakeIsFree() && canHandleIntake.get()
                autoIntakeTracker.safeAddSensorId(it.sensorsId)
            }
            else
            {
                intakeDetectedTimer.reset()
                isAwaitingIntake = false
                if (intakeNotDetectedTimer.milliseconds() > MIN_TIME_FOR_INTAKE_NOT_DETECTION_MS)
                    autoIntakeTracker.safeRemoveSensorId(it.sensorsId)
            }

            if (isAwaitingIntake && intakeDetectedTimer.milliseconds() > MIN_TIME_FOR_INTAKE_DETECTION_MS)
            {
                logM.logMd("Currently addressing auto intake", LOGIC_STEPS)
                stopAwaitingEating(false)

                ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {

                    val storageCanHandleInput = ThreadedEventBus.LAZY_INSTANCE.invoke(
                        BallCountInStorageEvent()).count < MAX_BALL_COUNT

                    logM.logMd("Is not full: $storageCanHandleInput", GENERIC_INFO)

                    if (storageCanHandleInput)
                    {
                        ThreadedEventBus.LAZY_INSTANCE.invoke(
                            StorageGetReadyForIntakeEvent(it.color))
                        delay(DELAY.INTAKE_RACE_CONDITION_MS)

                        logM.logMd("COLOR SENSORS - Started intake", HARDWARE_HIGH)
                        resumeAwaitingEating()
        }   }   }   }



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
        }   }
    }
    private fun addDevices()
    {
        logM.logMd("INITIALISATION - Adding HwDevices", HARDWARE_HIGH)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSorting)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hwSensors)
    }
    fun resetParametersAndLogicToDefault() = stopAwaitingEating(false)



    fun resumeAwaitingEating() = canHandleIntake.set(true)
    fun stopAwaitingEating(stopBelts: Boolean)
    {
        logM.logMd("Stopped auto intake awaiting", GENERIC_INFO)
        canHandleIntake.set(false)
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



    fun shootStartBelts()
    {
        logM.logMd("SHOOT Starterв hw belts", HARDWARE)
        _hwSorting.shootStartBeltMotors()
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



    suspend fun forwardBeltsTime(timeMs: Long)
    {
        logM.logMd("Chosen time period: $timeMs", HARDWARE)
        startBelts()
        delay(timeMs)
        stopBelts()
    }
    suspend fun reverseBeltsTime(timeMs: Long)
    {
        logM.logMd("Chosen time period: $timeMs", HARDWARE)
        reverseBelts()
        delay(timeMs)
        stopBelts()
    }
    suspend fun rotateMobileSlot()
    {
        logM.logMd("ROTATING MOBILE SLOT", HARDWARE_HIGH)
        stopAwaitingEating(true)
        closeLaunch()
        closeTurretGate()

//        slowStartBelts()
//        delay(DELAY.SORTING_REALIGNING_FORWARD_MS)
        reverseBeltsTime(DELAY.SORTING_REALIGNING_REVERSE_MS)

        openGate()
        openPush()

        reverseBelts()
//        delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS * 10)
        closeGateWithPush()
    }

    suspend fun smartPushNextBall()
    {
        logM.logMd("STARTED Smart ball push for shot", HARDWARE_HIGH)
        shootStartBelts()

        if (helpPushLastBall.get())
        {
            delay(DELAY.HARDWARE_REQUEST_FREQUENCY_MS * 8)
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