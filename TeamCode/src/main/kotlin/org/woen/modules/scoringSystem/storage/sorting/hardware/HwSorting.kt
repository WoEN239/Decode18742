package org.woen.modules.scoringSystem.storage.sorting.hardware


import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import java.util.concurrent.atomic.AtomicReference

import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.hotRun.HotRun
import org.woen.threading.hardware.ThreadedBattery
import org.woen.threading.hardware.IHardwareDevice

import org.woen.telemetry.Configs.STORAGE.MOBILE_GATE_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.MOBILE_GATE_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.MOBILE_PUSH_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.MOBILE_PUSH_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.MOBILE_FALL_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.MOBILE_FALL_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.STORAGE.MOBILE_LAUNCH_SERVO_CLOSE_VALUE
import org.woen.telemetry.Configs.STORAGE.MOBILE_LAUNCH_SERVO_OPEN_VALUE

import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_OPEN_VALUE
import org.woen.telemetry.Configs.STORAGE.TURRET_GATE_SERVO_CLOSE_VALUE

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_GATE_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_PUSH_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_FALL_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_LAUNCH_SERVO
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.TURRET_GATE_SERVO

import org.woen.telemetry.Configs.STORAGE.SORTING_STORAGE_BELT_MOTOR_1_DIRECTION
import org.woen.telemetry.Configs.STORAGE.SORTING_STORAGE_BELT_MOTOR_2_DIRECTION

import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.SORTING_STORAGE_BELT_MOTOR_1
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.SORTING_STORAGE_BELT_MOTOR_2



class HwSorting : IHardwareDevice
{
    private lateinit var _beltMotor1 : DcMotorEx
    private lateinit var _beltMotor2 : DcMotorEx

    private val _motor1Power = AtomicReference(0.0)
    private val _motor2Power = AtomicReference(0.0)


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

        _beltMotor1 = hardwareMap.get(SORTING_STORAGE_BELT_MOTOR_1) as DcMotorEx
        _beltMotor2 = hardwareMap.get(SORTING_STORAGE_BELT_MOTOR_2) as DcMotorEx


        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _beltMotor1.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            _beltMotor1.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

            _beltMotor2.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            _beltMotor2.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER


            _beltMotor1.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            _beltMotor2.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

            _beltMotor1.direction = SORTING_STORAGE_BELT_MOTOR_1_DIRECTION
            _beltMotor2.direction = SORTING_STORAGE_BELT_MOTOR_2_DIRECTION

            fullCalibrate()
        }
    }
    override fun update()
    {
        _gateServo.position = _gatePosition.get()
        _pushServo.position = _pushPosition.get()
        _fallServo.position = _fallPosition.get()

        _launchServo.position = _launchPosition.get()
        _turretGateServo.position = _turretGatePosition.get()

        _beltMotor1.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_motor1Power.get())
        _beltMotor2.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(_motor2Power.get())
    }



    fun startBeltMotor()
    {
        _motor1Power.set(12.0)
        _motor2Power.set(12.0)
    }
    fun reverseBeltMotor()
    {
        _motor1Power.set(-12.0)
        _motor2Power.set(-12.0)
    }
    fun stopBeltMotor()
    {
        _motor1Power.set(0.0)
        _motor2Power.set(0.0)
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

        stopBeltMotor()
    }



    override fun dispose() { }
}