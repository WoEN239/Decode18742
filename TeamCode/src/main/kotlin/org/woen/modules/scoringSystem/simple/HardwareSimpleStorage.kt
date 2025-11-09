package org.woen.modules.scoringSystem.simple

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.MOBILE_GATE_SERVO
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.motor.MotorOnly

class HardwareSimpleStorage(
    private val _beltMotor1Name: String,
    private val _beltMotor2Name: String
) : IHardwareDevice {
    enum class BeltState{
        STOP,
        RUN_REVERS,
        RUN
    }

    private lateinit var _beltMotor1: MotorOnly
    private lateinit var _beltMotor2: MotorOnly

    var beltState = BeltState.STOP

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
            return

        when(beltState){
            BeltState.STOP -> {
                _beltMotor1.power = 0.0
                _beltMotor2.power = 0.0
            }

            BeltState.RUN_REVERS -> {
                _beltMotor1.power = -1.0
                _beltMotor2.power = -1.0
            }

            BeltState.RUN -> {
                _beltMotor1.power = 1.0
                _beltMotor2.power = 1.0
            }
        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _beltMotor1 = MotorOnly(hardwareMap.get(_beltMotor1Name) as DcMotorEx)
        _beltMotor2 = MotorOnly(hardwareMap.get(_beltMotor2Name) as DcMotorEx)

        val pushServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.MOBILE_PUSH_SERVO) as Servo
        val fallServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.MOBILE_FALL_SERVO) as Servo
        val gateServo = hardwareMap.get(MOBILE_GATE_SERVO) as Servo

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _beltMotor1.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            _beltMotor2.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

            _beltMotor1.direction = Configs.STORAGE.SORTING_STORAGE_BELT_MOTOR_1_DIRECTION
            _beltMotor2.direction = Configs.STORAGE.SORTING_STORAGE_BELT_MOTOR_2_DIRECTION
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            pushServo.position = Configs.STORAGE.MOBILE_PUSH_SERVO_CLOSE_VALUE
            fallServo.position = Configs.STORAGE.MOBILE_FALL_SERVO_CLOSE_VALUE
            gateServo.position = Configs.STORAGE.MOBILE_GATE_SERVO_CLOSE_VALUE
        }
    }

    override fun dispose() {

    }
}