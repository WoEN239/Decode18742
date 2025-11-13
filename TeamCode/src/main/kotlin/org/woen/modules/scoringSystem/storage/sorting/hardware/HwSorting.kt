package org.woen.modules.scoringSystem.storage.sorting.hardware


import kotlin.math.PI
import java.util.concurrent.atomic.AtomicReference

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import org.woen.threading.hardware.ThreadedServo
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.hotRun.HotRun
import org.woen.threading.hardware.HardwareThreads
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


    val gateServo = ThreadedServo(
        MOBILE_GATE_SERVO,
        _startAngle = 1.5 * PI * MOBILE_GATE_SERVO_CLOSE_VALUE
    )
    val pushServo = ThreadedServo(
        MOBILE_PUSH_SERVO,
        _startAngle = 1.5 * PI * MOBILE_PUSH_SERVO_CLOSE_VALUE
    )
    val fallServo = ThreadedServo(
        MOBILE_FALL_SERVO,
        _startAngle = 1.5 * PI * MOBILE_FALL_SERVO_CLOSE_VALUE
    )

    
//    val launchServo = ThreadedServo(
//        MOBILE_LAUNCH_SERVO,
//        _startAngle = 1.5 * PI * MOBILE_LAUNCH_SERVO_CLOSE_VALUE
//    )
    val turretGateServo = ThreadedServo(
        TURRET_GATE_SERVO,
        _startAngle = 1.5 * PI * TURRET_GATE_SERVO_CLOSE_VALUE
    )



    override fun init(hardwareMap : HardwareMap)
    {
        _beltMotor1 = hardwareMap.get(SORTING_STORAGE_BELT_MOTOR_1) as DcMotorEx
        _beltMotor2 = hardwareMap.get(SORTING_STORAGE_BELT_MOTOR_2) as DcMotorEx

        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(
            gateServo, pushServo, fallServo, /*launchServo,*/ turretGateServo
        )

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



    fun openGate()
    {
        gateServo.targetAngle = 1.5 * PI * MOBILE_GATE_SERVO_OPEN_VALUE
    }
    fun closeGate()
    {
        gateServo.targetAngle = 1.5 * PI * MOBILE_GATE_SERVO_CLOSE_VALUE
    }

    fun openPush()
    {
        pushServo.targetAngle = 1.5 * PI * MOBILE_PUSH_SERVO_OPEN_VALUE
    }
    fun closePush()
    {
        pushServo.targetAngle = 1.5 * PI * MOBILE_PUSH_SERVO_CLOSE_VALUE
    }

    fun openFall()
    {
        fallServo.targetAngle = 1.5 * PI * MOBILE_FALL_SERVO_OPEN_VALUE
    }
    fun closeFall()
    {
        fallServo.targetAngle = 1.5 * PI * MOBILE_FALL_SERVO_CLOSE_VALUE
    }


//    fun openLaunch()
//    {
//        launchServo.targetAngle = 1.5 * PI * MOBILE_LAUNCH_SERVO_OPEN_VALUE
//    }
//    fun closeLaunch()
//    {
//        launchServo.targetAngle = 1.5 * PI * MOBILE_LAUNCH_SERVO_CLOSE_VALUE
//    }

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
        closeFall()

//        closeLaunch()
        closeTurretGate()

        stopBeltMotor()
    }



    override fun dispose() { }
}