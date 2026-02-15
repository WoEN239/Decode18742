package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlin.math.PI
import java.util.concurrent.atomic.AtomicReference

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.utils.motor.MotorOnly

import org.woen.threading.hardware.ThreadedServo
import org.woen.threading.hardware.ThreadedBattery
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.configs.Hardware
//import org.woen.telemetry.configs.Configs.DELAY
//import org.woen.telemetry.configs.Hardware.VALUES.BELTS.STORAGE_CURRENT_WHEN_FULL



class HwSorting : IHardwareDevice
{
    private lateinit var _beltMotors : MotorOnly
    private val _beltMotorsPower = AtomicReference(0.0)

//    val beltsCurrentPeakedEvent  = SimpleEmptyEvent()
//    private val _currentNoiseFilterTimer = ElapsedTime()



    val gateServo = ThreadedServo(
        Hardware.DEVICE_NAMES.GATE_SERVO,
        startAngle = 1.5 * PI * Hardware.VALUES.SERVO.GATE_CLOSE)

    val pushServo = ThreadedServo(
        Hardware.DEVICE_NAMES.PUSH_SERVO,
        startAngle = 1.5 * PI * Hardware.VALUES.SERVO.PUSH_CLOSE)

    val launchServo = ThreadedServo(
        Hardware.DEVICE_NAMES.LAUNCH_SERVO,
        startAngle = 1.5 * PI * Hardware.VALUES.SERVO.LAUNCH_CLOSE)

    val turretGateServo = ThreadedServo(
        Hardware.DEVICE_NAMES.TURRET_GATE_SERVO,
        startAngle = 1.5 * PI * Hardware.VALUES.SERVO.TURRET_GATE_CLOSE)



    constructor()
    {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(
            gateServo, pushServo, launchServo, turretGateServo)
    }

    override fun init(hardwareMap : HardwareMap)
    {
        _beltMotors = MotorOnly(
            hardwareMap.get(
                Hardware.DEVICE_NAMES.STORAGE_BELT_MOTOR)
                    as DcMotorEx)
    }





    override fun update()
    {
        _beltMotors.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_beltMotorsPower.get())

//        val beltsCurrent = _beltMotors.getCurrent(CurrentUnit.AMPS)
//        if (beltsCurrent > STORAGE_CURRENT_WHEN_FULL)
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
        _beltMotors.direction  = Hardware.VALUES.BELTS.MOTORS_DIRECTION

        fullCalibrate()
    }
    override fun opModeStop()  = stopBeltMotors()



    fun shootStartBeltMotors() = _beltMotorsPower.set(  Hardware.VALUES.BELTS.POWER.SHOOT)
    fun lazyStartBeltMotors()  = _beltMotorsPower.set(  Hardware.VALUES.BELTS.POWER.LAZY)
    fun slowStartBeltMotors()  = _beltMotorsPower.set(  Hardware.VALUES.BELTS.POWER.SLOW)
    fun fastStartBeltMotors()  = _beltMotorsPower.set(  Hardware.VALUES.BELTS.POWER.FAST)
    fun reverseBeltMotors()    = _beltMotorsPower.set( -Hardware.VALUES.BELTS.POWER.FAST)
    fun stopBeltMotors()       = _beltMotorsPower.set(0.0)



    fun openGate()
    {
        gateServo.targetPosition = Hardware.VALUES.SERVO.GATE_OPEN
    }
    fun closeGate()
    {
        gateServo.targetPosition = Hardware.VALUES.SERVO.GATE_CLOSE
    }

    fun openPush()
    {
        pushServo.targetPosition = Hardware.VALUES.SERVO.PUSH_OPEN
    }
    fun closePush()
    {
        pushServo.targetPosition = Hardware.VALUES.SERVO.PUSH_CLOSE
    }

    fun openLaunch()
    {
        launchServo.targetPosition = Hardware.VALUES.SERVO.LAUNCH_OPEN
    }
    fun closeLaunch()
    {
        launchServo.targetPosition = Hardware.VALUES.SERVO.LAUNCH_CLOSE
    }



    fun openTurretGate()
    {
        turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_OPEN
    }
    fun closeTurretGate()
    {
        turretGateServo.targetPosition = Hardware.VALUES.SERVO.TURRET_GATE_CLOSE
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