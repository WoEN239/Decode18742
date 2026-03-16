package org.woen.scoringSystem.storage.hardware


import kotlin.math.max
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.configs.DebugSettings

import org.woen.utils.debug.Debug
import org.woen.utils.debug.LogManager

import org.woen.configs.Delay
import org.woen.scoringSystem.ConnectorModuleStatus

//import org.woen.telemetry.configs.RobotSettings.SORTING



class HwSortingManager
{
    private val _hwSorting = HwSorting()
//    private val _hwSensors = HwSortingSensors()
    private val _cms: ConnectorModuleStatus

    val logM: LogManager

    var currentPushTime: Long = 0
    val reinstantiableBeltsTimer = ElapsedTime()



    constructor(cms: ConnectorModuleStatus)
    {
        _cms = cms

        logM = LogManager(_cms.collector, DebugSettings.HSM)

        subscribeToColorDetectEvents()
    }



    private fun subscribeToColorDetectEvents()
    {
//        _hwSensors.colorSensorsDetectedIntakeEvent +=
//        {
//            if (canHandleIntake.get())
//            {
//                logM.logMdTag(
//                    "Color sensors detected: ${it.color}",
//                    "StorageSensors", Debug.HW_LOW)
//
//                EventBusLI.invoke(
//                    StorageGetReadyForIntakeEvent(
//                        it.color))
//            }
//        }
    }
    fun relink()
    {
        fullCalibrate()

        logM.relink(DebugSettings.HSM)
    }



    fun fullCalibrate()
    {
//        logM.logMd("STARTED full calibration", Debug.HW_HIGH)
//        _hwSorting.fullCalibrate()
//
//        while (!_hwSorting.gateServo.atTargetAngle
//            || !_hwSorting.pushServo.atTargetAngle
//            || !_hwSorting.launchServo.atTargetAngle
//            || !_hwSorting.turretGateServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)
//            ;

        logM.logMd("Full calibration completed", Debug.HW_HIGH)
    }


    fun openGate()
    {
        logM.logMd("Started OPENING sorting gate", Debug.HW)
//        _hwSorting.openGate()
//
//        while (!_hwSorting.gateServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)

        logM.logMd("OPENED sorting gate", Debug.HW)
    }
    fun openPush()
    {
        logM.logMd("Started OPENING push", Debug.HW)
//        _hwSorting.openPush()
//
//        while (!_hwSorting.pushServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)

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
    fun closeGateWithPush()
    {
        logM.logMd("Started CLOSING sorting gate & push", Debug.HW)
//        _hwSorting.closeGate()
//        _hwSorting.closePush()
//
//        while (!_hwSorting.pushServo.atTargetAngle
//            || !_hwSorting.gateServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)

        logM.logMd("CLOSED sorting gate & push", Debug.HW)
    }


    fun openLaunch()
    {
        logM.logMd("Started OPENING kick", Debug.HW)
//        _hwSorting.openLaunch()
//
//        while (!_hwSorting.launchServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)

        logM.logMd("OPENED kick", Debug.HW)
    }
    fun closeLaunch()
    {
        logM.logMd("Started CLOSING kick", Debug.HW)
//        _hwSorting.closeLaunch()
//
//        while (!_hwSorting.launchServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)

        logM.logMd("CLOSED kick", Debug.HW)
    }


    fun openTurretGate()
    {
        logM.logMd("Started OPENING turret gate", Debug.HW)
//        _hwSorting.openTurretGate()
//
//        while (!_hwSorting.turretGateServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)

        logM.logMd("OPENED turret gate successfully", Debug.HW)
    }
    fun closeTurretGate()
    {
        logM.logMd("Started CLOSING turret gate", Debug.HW)
//        _hwSorting.closeTurretGate()
//
//        while (!_hwSorting.turretGateServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)

        logM.logMd("CLOSED turret gate successfully", Debug.HW)
    }


    fun shootStartBelts()
    {
        logM.logMd("SHOOT Started hw belts", Debug.HW)
//        _hwSorting.shootStartBeltMotors()
    }
    fun lazyStartBelts()
    {
        logM.logMd("LAZY Started hw belts", Debug.HW)
//        _hwSorting.lazyStartBeltMotors()
    }

    fun slowStartBelts()
    {
        logM.logMd("SLOW Started hw belts", Debug.HW)
//        _hwSorting.slowStartBeltMotors()
    }

    fun startBelts()
    {
        logM.logMd("Started hw belts", Debug.HW)
//        _hwSorting.fastStartBeltMotors()
    }
    fun reverseBelts()
    {
        logM.logMd("Reversing hw belts", Debug.HW)
//        _hwSorting.reverseBeltMotors()
    }
    fun stopBelts()
    {
        logM.logMd("Stopped hw belts", Debug.HW)
//        _hwSorting.stopBeltMotors()
    }


    fun reinstantiableForwardBeltsTime(timeMs: Long, firstInstance: Boolean = false)
    {
        val pushTime: Long =
            if (firstInstance) timeMs
            else max(timeMs,
                    currentPushTime
                        - reinstantiableBeltsTimer
                            .milliseconds().toLong())

        reinstantiableBeltsTimer.reset()
        currentPushTime = pushTime
        logM.logMd("Chosen time period: $pushTime", Debug.HW)

//        val pushing = SmartCoroutineLI.launch {
//
//            startBelts()
//            delay(pushTime)
//
//            if (reinstantiableBeltsTimer.milliseconds()
//                > currentPushTime.get())
//                stopBelts()
//        }
//
//        while (!pushing.isCompleted)
//            delay(Delay.MS.AWAIT.EVENTS)
    }


    fun forwardBeltsTime(timeMs: Long)
    {
        logM.logMd("Chosen time period: $timeMs", Debug.HW)
        startBelts()

//        delay(timeMs)
        stopBelts()
    }
    fun reverseBeltsTime(timeMs: Long)
    {
        logM.logMd("Chosen time period: $timeMs", Debug.HW)
        reverseBelts()

//        delay(timeMs)
        stopBelts()
    }


    fun rotateMobileSlot()
    {
        logM.logMd("ROTATING MOBILE SLOT", Debug.HW_HIGH)
        _cms.canTriggerIntake = false

        closeLaunch()
        closeTurretGate()

//        slowStartBelts()
//        delay(Delay.MS.REALIGNMENT.SORTING_FORWARD)
        reverseBeltsTime(Delay.MS.REALIGNMENT.SORTING_REVERSE)

        openGate()
        openPush()

        forwardBeltsTime(Delay.MS.HW_REQUEST_FREQUENCY * 8)

        closeGateWithPush()
        forwardBeltsTime(Delay.MS.REALIGNMENT.SORTING_FORWARD)
    }
}