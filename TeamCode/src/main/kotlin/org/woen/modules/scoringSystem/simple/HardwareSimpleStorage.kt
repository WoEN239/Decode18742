package org.woen.modules.scoringSystem.simple

import android.graphics.Bitmap
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.motor.MotorOnly

class HardwareSimpleStorage : IHardwareDevice
{
    enum class BeltState{
        STOP,
        RUN_REVERS,
        RUN
    }

    private lateinit var _beltMotor: MotorOnly

    var beltState = BeltState.STOP

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
            return

        when(beltState){
            BeltState.STOP -> {
                _beltMotor.power = 0.0
            }

            BeltState.RUN_REVERS -> {
                _beltMotor.power = -1.0
            }

            BeltState.RUN -> {
                _beltMotor.power = 1.0
            }
        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _beltMotor = MotorOnly(hardwareMap.get("belt_motors") as DcMotorEx)

        val pushServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.PUSH_SERVO) as Servo
        val gateServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.GATE_SERVO) as Servo
        val rotateServo = hardwareMap.get("turretRotateServo") as Servo

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

            _beltMotor.direction = Configs.STORAGE.SORTING_STORAGE_BELT_MOTORS_DIRECTION
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            pushServo.position = Configs.STORAGE.PUSH_SERVO_CLOSE_VALUE
            gateServo.position = Configs.STORAGE.GATE_SERVO_CLOSE_VALUE
            rotateServo.position = 0.55
        }
    }

    override fun dispose() {

    }
}