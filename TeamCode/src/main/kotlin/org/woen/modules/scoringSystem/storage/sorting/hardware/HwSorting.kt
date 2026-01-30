package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlin.math.PI
import java.util.concurrent.atomic.AtomicReference

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
//import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

import org.woen.utils.motor.MotorOnly
import org.woen.utils.events.SimpleEmptyEvent

import org.woen.threading.hardware.ThreadedServo
import org.woen.threading.hardware.ThreadedBattery
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.IHardwareDevice

//import org.woen.telemetry.Configs.DELAY
//import org.woen.telemetry.Configs.STORAGE.STORAGE_IS_FULL_BELTS_CURRENT

import org.woen.telemetry.Configs.STORAGE.GATE_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.GATE_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.PUSH_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.PUSH_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.LAUNCH_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.LAUNCH_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.BELT_MOTORS_DIRECTION

import org.woen.telemetry.Configs.STORAGE.BELT_POWER_SLOW_MODE
import org.woen.telemetry.Configs.STORAGE.BELT_POWER_FAST_MODE
import org.woen.telemetry.Configs.STORAGE.BELT_POWER_SHOOT_MODE

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.GATE_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.PUSH_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.LAUNCH_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.TURRET_GATE_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.STORAGE_BELT_MOTOR



class HwSorting : IHardwareDevice
{
    private lateinit var _beltMotors : MotorOnly
    private val _beltMotorsPower = AtomicReference(0.0)

    val beltsCurrentPeakedEvent  = SimpleEmptyEvent()
    private val _currentNoiseFilterTimer = ElapsedTime()



    val gateServo = ThreadedServo(
        GATE_SERVO,
        startAngle = 1.5 * PI * GATE_SERVO_CLOSE_VALUE)

    val pushServo = ThreadedServo(
        PUSH_SERVO,
        startAngle = 1.5 * PI * PUSH_SERVO_CLOSE_VALUE)

    val launchServo = ThreadedServo(
        LAUNCH_SERVO,
        startAngle = 1.5 * PI * LAUNCH_SERVO_CLOSE_VALUE)

    val turretGateServo = ThreadedServo(
        TURRET_GATE_SERVO,
        startAngle = 1.5 * PI * TURRET_GATE_SERVO_CLOSE_VALUE)



    constructor()
    {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(
            gateServo, pushServo, launchServo, turretGateServo)
    }

    override fun init(hardwareMap : HardwareMap)
    {
        _beltMotors = MotorOnly(hardwareMap.get(STORAGE_BELT_MOTOR) as DcMotorEx)
    }





    override fun update()
    {
        _beltMotors.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_beltMotorsPower.get())

//        val beltsCurrent = _beltMotors.getCurrent(CurrentUnit.AMPS)
//        if (beltsCurrent > STORAGE_IS_FULL_BELTS_CURRENT)
//        {
//            if (_currentNoiseFilterTimer.milliseconds()
//                > DELAY.IGNORE_BELTS_CURRENT_AFTER_START_MS)
//            {
//                beltsCurrentPeakedEvent.invoke()
//                _currentNoiseFilterTimer.reset()
//            }
//        }
//        else _currentNoiseFilterTimer.reset()
    }

    override fun opModeStart()
    {
        _beltMotors.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        _beltMotors.direction = BELT_MOTORS_DIRECTION

        fullCalibrate()
    }
    override fun opModeStop()  = stopBeltMotors()



    fun shootStartBeltMotors() = _beltMotorsPower.set(  BELT_POWER_SHOOT_MODE)
    fun slowStartBeltMotors()  = _beltMotorsPower.set(  BELT_POWER_SLOW_MODE)
    fun startBeltMotors()      = _beltMotorsPower.set(  BELT_POWER_FAST_MODE)
    fun reverseBeltMotors()    = _beltMotorsPower.set( -BELT_POWER_FAST_MODE)
    fun stopBeltMotors()       = _beltMotorsPower.set(0.0)



    fun openGate()
    {
        gateServo.targetPosition = GATE_SERVO_OPEN_VALUE
    }
    fun closeGate()
    {
        gateServo.targetPosition = GATE_SERVO_CLOSE_VALUE
    }

    fun openPush()
    {
        pushServo.targetPosition = PUSH_SERVO_OPEN_VALUE
    }
    fun closePush()
    {
        pushServo.targetPosition = PUSH_SERVO_CLOSE_VALUE
    }

    fun openLaunch()
    {
        launchServo.targetPosition = LAUNCH_SERVO_OPEN_VALUE
    }
    fun closeLaunch()
    {
        launchServo.targetPosition = LAUNCH_SERVO_CLOSE_VALUE
    }



    fun openTurretGate()
    {
        turretGateServo.targetPosition = TURRET_GATE_SERVO_OPEN_VALUE
    }
    fun closeTurretGate()
    {
        turretGateServo.targetPosition = TURRET_GATE_SERVO_CLOSE_VALUE
    }



    fun fullCalibrate()
    {
        stopBeltMotors()

        closeGate()
        closePush()
        closeLaunch()
        closeTurretGate()
    }



    override fun dispose() { }
}