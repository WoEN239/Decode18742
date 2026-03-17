package org.woen.scoringSystem.storage.hardware


import kotlin.math.max
import com.qualcomm.robotcore.util.ElapsedTime
import com.qualcomm.robotcore.hardware.DcMotorEx
import org.woen.collector.RunMode

import org.woen.configs.Hardware
import org.woen.utils.motor.MotorOnly

import org.woen.utils.debug.Debug
import org.woen.configs.DebugSettings
import org.woen.configs.RobotSettings.TELEOP
import org.woen.configs.RobotSettings.AUTONOMOUS
import org.woen.enumerators.Ball
import org.woen.utils.debug.LogManager

import org.woen.scoringSystem.ConnectorModuleStatus


enum class HwStatus
{
    IDLE,
    BELTS_FORWARD,
    BELTS_REVERSE
}



class HwSortingManager
{
    private val _beltMotor  : MotorOnly
    private val _brushMotor : MotorOnly
    private val _hwSensors: HwSensors
    private val _cms: ConnectorModuleStatus

    val logM: LogManager

    var imCurrentlyRotating = false

    var currentPushTime: Long = 0
    val reinstantiableBeltsTimer = ElapsedTime()



    constructor(cms: ConnectorModuleStatus)
    {
        _cms = cms
        _hwSensors = HwSensors(cms)

        _beltMotor = MotorOnly(
            _cms.collector.hardwareMap.get(
                Hardware.DEVICE_NAMES.STORAGE_BELT_MOTOR)
                    as DcMotorEx)
        _brushMotor = MotorOnly(
            _cms.collector.hardwareMap.get(
                Hardware.DEVICE_NAMES.BRUSH_MOTOR)
                    as DcMotorEx)


        logM = LogManager(_cms.collector.telemetry, DebugSettings.HSM)
    }



    fun updateColors(): Ball.Name
    {
        return if  (_cms.canTriggerIntake && (
                        (_cms.collector.runMode == RunMode.AUTO
                        && !AUTONOMOUS.IGNORE_COLOR_SENSORS)
                    ) || (
                        _cms.collector.runMode == RunMode.MANUAL
                        && !TELEOP.IGNORE_COLOR_SENSORS))
            _hwSensors.update()
        else Ball.Name.NONE
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
    fun closeGateWithPush()
    {
        logM.logMd("Started CLOSING gate & push", Debug.HW)
//        _hwSorting.closeGate()
//        _hwSorting.closePush()
//
//        while (!_hwSorting.gateServo.atTargetAngle
//            && !_hwSorting.pushServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)

        logM.logMd("CLOSED gate & push", Debug.HW)
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

    fun startBelts()
    {
        logM.logMd("Started hw belts", Debug.HW)
        _beltMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BELTS_FORWARD)
    }
    fun reverseBelts()
    {
        logM.logMd("Reversing hw belts", Debug.HW)
        _beltMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BELTS_REVERSE)
    }
    fun stopBelts()
    {
        logM.logMd("Stopped hw belts", Debug.HW)
        _beltMotor.power = 0.0
    }


    fun startBrush()
    {
        logM.logMd("Started hw brush", Debug.HW)
        _brushMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BRUSH_FORWARD)
    }
    fun reverseBrush()
    {
        logM.logMd("Reversing hw belts", Debug.HW)
        _brushMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BRUSH_REVERSE)
    }


    fun reinstantiableForwardBeltsTime(timeMs: Long)
    {
        val pushTime: Long =
            if (!imCurrentlyRotating) timeMs
            else max(timeMs,
                    currentPushTime
                        - reinstantiableBeltsTimer
                            .milliseconds().toLong())

        imCurrentlyRotating = true
        reinstantiableBeltsTimer.reset()
        currentPushTime = pushTime
        logM.logMd("Rotate belts period: $pushTime", Debug.HW)

        startBelts()
    }
    fun extendableForwardBeltsTime(timeMs: Long)
    {
        val pushTime: Long =
            if (!imCurrentlyRotating) timeMs
            else timeMs + currentPushTime
                 - reinstantiableBeltsTimer
                    .milliseconds().toLong()

        reinstantiableBeltsTimer.reset()
        currentPushTime = pushTime
        logM.logMd("Rotate belts period: $pushTime", Debug.HW)

        startBelts()
    }


    fun rotateMobileSlot()
    {
        logM.logMd("ROTATING MOBILE SLOT", Debug.HW_HIGH)

        closeTurretGate()

//        slowStartBelts()
//        delay(Delay.MS.REALIGNMENT.SORTING_FORWARD)
//        reverseBeltsTime(Delay.MS.REALIGNMENT.SORTING_REVERSE)

        openGate()
        openPush()

//        forwardBeltsTime(Delay.MS.HW_REQUEST_FREQUENCY * 8)

//        closeGateWithPush
//        forwardBeltsTime(Delay.MS.REALIGNMENT.SORTING_FORWARD)
    }
}