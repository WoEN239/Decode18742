package org.woen.scoringSystem.storage.hardware


import com.qualcomm.robotcore.hardware.DcMotorEx

import org.woen.utils.motor.MotorOnly
import org.woen.utils.debug.LogManager
import org.woen.utils.drivers.SoftServo
import org.woen.scoringSystem.ConnectorModuleStatus

import org.woen.enumerators.phases.MotorStatus
import org.woen.enumerators.phases.ServoStatus

import org.woen.configs.Hardware
import org.woen.configs.DebugSettings



class HwMotors
{
    private val _cms: ConnectorModuleStatus
    val logM: LogManager

    private val _beltMotor  : MotorOnly
    private val _brushMotor : MotorOnly

    private val _gateServo   : SoftServo
    private val _pushServo   : SoftServo
    private val _launchServo : SoftServo
    private val _turretGateServo : SoftServo



    constructor(cms: ConnectorModuleStatus)
    {
        _cms = cms
        logM = LogManager(_cms.collector.telemetry, DebugSettings.HSM)


        _beltMotor = MotorOnly(
            _cms.collector.hardwareMap.get(
                Hardware.DEVICE_NAMES.STORAGE_BELT_MOTOR)
                    as DcMotorEx)
        _brushMotor = MotorOnly(
            _cms.collector.hardwareMap.get(
                Hardware.DEVICE_NAMES.BRUSH_MOTOR)
                    as DcMotorEx)


        _gateServo = SoftServo(
            Hardware.DEVICE_NAMES.GATE_SERVO,
            _cms.collector.hardwareMap,
            Hardware.SERVO.GATE_CLOSE)

        _pushServo = SoftServo(
            Hardware.DEVICE_NAMES.PUSH_SERVO,
            _cms.collector.hardwareMap,
            Hardware.SERVO.PUSH_CLOSE)

        _launchServo = SoftServo(
            Hardware.DEVICE_NAMES.LAUNCH_SERVO,
            _cms.collector.hardwareMap,
            Hardware.SERVO.LAUNCH_CLOSE)

        _turretGateServo = SoftServo(
            Hardware.DEVICE_NAMES.TURRET_GATE_SERVO,
            _cms.collector.hardwareMap,
            Hardware.SERVO.TURRET_GATE_CLOSE)

        _gateServo.start()
        _pushServo.start()
        _launchServo.start()
        _turretGateServo.start()
    }


    fun updateServos()
    {
        _gateServo.update()
        _pushServo.update()
        _launchServo.update()
        _turretGateServo.update()

        _cms.gateStatus.tryUpdate(_gateServo.atTarget)
        _cms.pushStatus.tryUpdate(_pushServo.atTarget)
        _cms.launchStatus.tryUpdate(_launchServo.atTarget)
        _cms.turretGateStatus.tryUpdate(_turretGateServo.atTarget)
    }



    fun openGate()
    {
        _gateServo.targetPosition = Hardware.SERVO.GATE_OPEN
        _cms.gateStatus.set(ServoStatus.Name.OPENING)
    }
    fun openPush()
    {
        _pushServo.targetPosition = Hardware.SERVO.PUSH_OPEN
        _cms.pushStatus.set(ServoStatus.Name.OPENING)
    }
    fun closeGateWithPush()
    {
        _gateServo.targetPosition = Hardware.SERVO.GATE_CLOSE
        _pushServo.targetPosition = Hardware.SERVO.PUSH_CLOSE
        _cms.gateStatus.set(ServoStatus.Name.CLOSING)
        _cms.pushStatus.set(ServoStatus.Name.CLOSING)
    }


    fun openLaunch()
    {
        _launchServo.targetPosition = Hardware.SERVO.LAUNCH_OPEN
        _cms.launchStatus.set(ServoStatus.Name.OPENING)
    }
    fun closeLaunch()
    {
        _launchServo.targetPosition = Hardware.SERVO.LAUNCH_CLOSE
        _cms.launchStatus.set(ServoStatus.Name.CLOSING)
    }

    fun openTurretGate()
    {
        _turretGateServo.targetPosition = Hardware.SERVO.TURRET_GATE_OPEN
        _cms.turretGateStatus.set(ServoStatus.Name.OPENING)
    }
    fun closeTurretGate()
    {
        _turretGateServo.targetPosition = Hardware.SERVO.TURRET_GATE_CLOSE
        _cms.turretGateStatus.set(ServoStatus.Name.CLOSING)
    }


    fun lazyForwardBelts(voltage: Double = 10.0)
    {
        _cms.beltsStatus = MotorStatus.LAZY_FORWARD
        _beltMotor.power = _cms.collector.battery.voltageToPower(voltage)
    }
    fun lazyReverseBelts(voltage: Double = 10.0)
    {
        _cms.beltsStatus = MotorStatus.LAZY_REVERSE
        _beltMotor.power = _cms.collector.battery.voltageToPower(voltage)
    }
    fun forwardBelts(voltage: Double = 12.0)
    {
        _cms.beltsStatus = MotorStatus.FORWARD
        _beltMotor.power = _cms.collector.battery.voltageToPower(voltage)
    }
    fun reverseBelts(voltage: Double = 12.0)
    {
        _cms.beltsStatus = MotorStatus.REVERSE
        _beltMotor.power = _cms.collector.battery.voltageToPower(voltage)
    }
    fun stopBelts()
    {
        _cms.beltsStatus = MotorStatus.IDLE
        _beltMotor.power = 0.0
    }


    fun forwardBrush()
    {
        _brushMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BRUSH_FORWARD)
    }
    fun reverseBrush()
    {
        _brushMotor.power = _cms.collector.battery.voltageToPower(
            Hardware.MOTOR.BRUSH_REVERSE)
    }
}