package org.woen.scoringSystem.storage.hardware


import com.qualcomm.robotcore.hardware.DcMotorEx

import org.woen.scoringSystem.ConnectorModuleStatus
import org.woen.utils.motor.MotorOnly
import org.woen.utils.debug.LogManager
import org.woen.configs.Hardware
import org.woen.configs.DebugSettings



class HwMotors
{
    private val _cms: ConnectorModuleStatus
    private val _beltMotor  : MotorOnly
    private val _brushMotor : MotorOnly

    val logM: LogManager


    constructor(cms: ConnectorModuleStatus)
    {
        _cms = cms

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





    fun openGate()
    {
//        _hwSorting.openGate()
    }
    fun openPush()
    {
//        _hwSorting.openPush()
//
//        while (!_hwSorting.pushServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)
    }
    fun closeGateWithPush()
    {
//        _hwSorting.closeGate()
//        _hwSorting.closePush()
//
//        while (!_hwSorting.gateServo.atTargetAngle
//            && !_hwSorting.pushServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)
    }


    fun openTurretGate()
    {
//        _hwSorting.openTurretGate()
//
//        while (!_hwSorting.turretGateServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)
    }
    fun closeTurretGate()
    {
//        _hwSorting.closeTurretGate()
//
//        while (!_hwSorting.turretGateServo.atTargetAngle)
//            delay(Delay.MS.HW_REQUEST_FREQUENCY)
    }

    fun startBelts()
    {
        _cms.beltsStatus = MotorStatus.FORWARD
        _beltMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BELTS_FORWARD)
    }
    fun reverseBelts()
    {
        _cms.beltsStatus = MotorStatus.REVERSE
        _beltMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BELTS_REVERSE)
    }
    fun stopBelts()
    {
        _cms.beltsStatus = MotorStatus.IDLE
        _beltMotor.power = 0.0
    }


    fun startBrush()
    {
        _cms.brushStatus = MotorStatus.FORWARD
        _brushMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BRUSH_FORWARD)
    }
    fun reverseBrush()
    {
        _cms.beltsStatus = MotorStatus.REVERSE
        _brushMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BRUSH_REVERSE)
    }
}