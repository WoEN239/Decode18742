package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlin.math.PI
import java.util.concurrent.atomic.AtomicReference

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.hotRun.HotRun
import org.woen.utils.motor.MotorOnly

import org.woen.threading.hardware.ThreadedServo
import org.woen.threading.hardware.ThreadedBattery
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.Configs.STORAGE.GATE_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.GATE_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.PUSH_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.PUSH_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.POWER_FOR_SLOW_BELT_ROTATING
import org.woen.telemetry.Configs.STORAGE.POWER_FOR_FAST_BELT_ROTATING

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.GATE_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.PUSH_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.TURRET_GATE_SERVO

import org.woen.telemetry.Configs.STORAGE.SORTING_STORAGE_BELT_MOTORS_DIRECTION
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.SORTING_STORAGE_BELT_MOTORS



class HwSorting : IHardwareDevice
{
    private lateinit var _beltMotors : MotorOnly

    private val _beltMotorsPower = AtomicReference(0.0)


    val gateServo = ThreadedServo(
        GATE_SERVO,
        startAngle = 1.5 * PI * GATE_SERVO_CLOSE_VALUE)

    val pushServo = ThreadedServo(
        PUSH_SERVO,
        startAngle = 1.5 * PI * PUSH_SERVO_CLOSE_VALUE)

    val turretGateServo = ThreadedServo(
        TURRET_GATE_SERVO,
        startAngle = 1.5 * PI * TURRET_GATE_SERVO_CLOSE_VALUE)



    override fun init(hardwareMap : HardwareMap)
    {
        _beltMotors = MotorOnly(hardwareMap.get(SORTING_STORAGE_BELT_MOTORS) as DcMotorEx)

        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(
            gateServo, pushServo, turretGateServo)

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _beltMotors.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            _beltMotors.direction = SORTING_STORAGE_BELT_MOTORS_DIRECTION

            fullCalibrate()
        }
    }
    override fun update()
    {
        _beltMotors.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_beltMotorsPower.get())
    }



    fun slowStartBeltMotors() = _beltMotorsPower.set(  POWER_FOR_SLOW_BELT_ROTATING)
    fun startBeltMotors()     = _beltMotorsPower.set(  POWER_FOR_FAST_BELT_ROTATING)
    fun reverseBeltMotors()   = _beltMotorsPower.set( -POWER_FOR_FAST_BELT_ROTATING)
    fun stopBeltMotors()      = _beltMotorsPower.set(0.0)



    fun openGate()
    {
        gateServo.targetAngle = 1.5 * PI * GATE_SERVO_OPEN_VALUE
    }
    fun closeGate()
    {
        gateServo.targetAngle = 1.5 * PI * GATE_SERVO_CLOSE_VALUE
    }

    fun openPush()
    {
        pushServo.targetAngle = 1.5 * PI * PUSH_SERVO_OPEN_VALUE
    }
    fun closePush()
    {
        pushServo.targetAngle = 1.5 * PI * PUSH_SERVO_CLOSE_VALUE
    }



    fun openTurretGate()
    {
        turretGateServo.targetAngle = 1.5 * PI * TURRET_GATE_SERVO_OPEN_VALUE
    }
    fun closeTurretGate()
    {
        turretGateServo.targetAngle = 1.5 * PI * TURRET_GATE_SERVO_CLOSE_VALUE
    }



    fun fullCalibrate()
    {
        closeGate()
        closePush()

        closeTurretGate()

        stopBeltMotors()
    }



    override fun dispose() { }
}