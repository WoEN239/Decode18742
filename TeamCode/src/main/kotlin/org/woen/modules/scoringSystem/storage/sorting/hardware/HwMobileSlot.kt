package org.woen.modules.scoringSystem.storage.sorting.hardware


import java.util.concurrent.atomic.AtomicReference

import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.Configs.STORAGE.SORTING_GATE_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.SORTING_GATE_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.SORTING_PUSH_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.SORTING_PUSH_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.SORTING_FALL_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.SORTING_FALL_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_FALL_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_GATE_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_PUSH_SERVO



class HwMobileSlot : IHardwareDevice
{
    private lateinit var _gateServo : Servo
    private var _gatePosition = AtomicReference(SORTING_GATE_SERVO_CLOSE_VALUE)

    private lateinit var _pushServo : Servo
    private var _pushPosition = AtomicReference(SORTING_PUSH_SERVO_CLOSE_VALUE)

    private lateinit var _fallServo : Servo
    private var _fallPosition = AtomicReference(SORTING_FALL_SERVO_CLOSE_VALUE)



    override fun init(hardwareMap : HardwareMap)
    {
        _gateServo = hardwareMap.get(MOBILE_GATE_SERVO) as Servo
        _pushServo = hardwareMap.get(MOBILE_PUSH_SERVO) as Servo
        _fallServo = hardwareMap.get(MOBILE_FALL_SERVO) as Servo

        fullCalibrate()
    }
    override fun update()
    {
        _gateServo.position = _gatePosition.get()
        _pushServo.position = _pushPosition.get()
        _fallServo.position = _fallPosition.get()
    }



    fun openGate()  = _gatePosition.set(SORTING_GATE_SERVO_OPEN_VALUE)
    fun closeGate() = _gatePosition.set(SORTING_GATE_SERVO_CLOSE_VALUE)

    fun openPush()  = _pushPosition.set(SORTING_PUSH_SERVO_OPEN_VALUE)
    fun closePush() = _pushPosition.set(SORTING_PUSH_SERVO_CLOSE_VALUE)

    fun openFall()  = _fallPosition.set(SORTING_FALL_SERVO_OPEN_VALUE)
    fun closeFall() = _fallPosition.set(SORTING_FALL_SERVO_CLOSE_VALUE)


    fun fullCalibrate()
    {
        closeGate()
        closePush()
        closeFall()
    }



    override fun dispose() { }
}