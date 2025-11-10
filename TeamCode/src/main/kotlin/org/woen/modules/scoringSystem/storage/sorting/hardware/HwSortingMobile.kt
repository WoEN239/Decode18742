package org.woen.modules.scoringSystem.storage.sorting.hardware


import java.util.concurrent.atomic.AtomicReference

import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.Configs.STORAGE.MOBILE_GATE_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.MOBILE_GATE_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.MOBILE_PUSH_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.MOBILE_PUSH_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.MOBILE_FALL_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.MOBILE_FALL_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.MOBILE_LAUNCH_SERVO_CLOSE_VALUE
import org.woen.telemetry.Configs.STORAGE.MOBILE_LAUNCH_SERVO_OPEN_VALUE

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_GATE_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_PUSH_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_FALL_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_LAUNCH_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.TURRET_GATE_SERVO
import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_CLOSE_VALUE
import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_OPEN_VALUE



class HwSortingMobile : IHardwareDevice
{
    private lateinit var _gateServo : Servo
    private var _gatePosition = AtomicReference(MOBILE_GATE_SERVO_CLOSE_VALUE)

    private lateinit var _pushServo : Servo
    private var _pushPosition = AtomicReference(MOBILE_PUSH_SERVO_CLOSE_VALUE)

    private lateinit var _fallServo : Servo
    private var _fallPosition = AtomicReference(MOBILE_FALL_SERVO_CLOSE_VALUE)


    private lateinit var _launchServo : Servo
    private var _launchPosition = AtomicReference(MOBILE_FALL_SERVO_CLOSE_VALUE)

    private lateinit var _turretGateServo : Servo
    private var _turretGatePosition = AtomicReference(TURRET_GATE_SERVO_CLOSE_VALUE)



    override fun init(hardwareMap : HardwareMap)
    {
        _gateServo = hardwareMap.get(MOBILE_GATE_SERVO) as Servo
        _pushServo = hardwareMap.get(MOBILE_PUSH_SERVO) as Servo
        _fallServo = hardwareMap.get(MOBILE_FALL_SERVO) as Servo

        _launchServo = hardwareMap.get(MOBILE_LAUNCH_SERVO) as Servo
        _turretGateServo = hardwareMap.get(TURRET_GATE_SERVO) as Servo

        fullCalibrate()
    }
    override fun update()
    {
        _gateServo.position = _gatePosition.get()
        _pushServo.position = _pushPosition.get()
        _fallServo.position = _fallPosition.get()

        _launchServo.position = _launchPosition.get()
        _turretGateServo.position = _turretGatePosition.get()
    }



    fun openGate()  = _gatePosition.set(MOBILE_GATE_SERVO_OPEN_VALUE)
    fun closeGate() = _gatePosition.set(MOBILE_GATE_SERVO_CLOSE_VALUE)

    fun openPush()  = _pushPosition.set(MOBILE_PUSH_SERVO_OPEN_VALUE)
    fun closePush() = _pushPosition.set(MOBILE_PUSH_SERVO_CLOSE_VALUE)

    fun openFall()  = _fallPosition.set(MOBILE_FALL_SERVO_OPEN_VALUE)
    fun closeFall() = _fallPosition.set(MOBILE_FALL_SERVO_CLOSE_VALUE)


    fun openLaunch()  = _launchPosition.set(MOBILE_LAUNCH_SERVO_OPEN_VALUE)
    fun closeLaunch() = _launchPosition.set(MOBILE_LAUNCH_SERVO_CLOSE_VALUE)

    fun openTurretGate()  = _turretGatePosition.set(TURRET_GATE_SERVO_OPEN_VALUE)
    fun closeTurretGate() = _turretGatePosition.set(TURRET_GATE_SERVO_CLOSE_VALUE)



    fun fullCalibrate()
    {
        closeGate()
        closePush()
        closeFall()

        closeLaunch()
        closeTurretGate()
    }



    override fun dispose() { }
}