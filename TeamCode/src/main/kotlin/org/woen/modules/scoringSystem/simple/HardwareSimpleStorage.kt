package org.woen.modules.scoringSystem.simple

import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_BALL_MAX_B_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_BALL_MAX_R_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_GREEN_BALL_MIN_G_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_BALL_MAX_G_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_BALL_MIN_B_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.THRESHOLD_PURPLE_BALL_MIN_R_S1
import org.woen.telemetry.Configs.COLOR_SENSORS_AND_OPTIC_PARE.VAR_MAXIMUM_READING
import org.woen.telemetry.Configs.HARDWARE_DEVICES_NAMES.INTAKE_COLOR_SENSOR_1
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.motor.MotorOnly
import woen239.FixColorSensor.fixSensor
import woen239.enumerators.Ball

class HardwareSimpleStorage : IHardwareDevice
{
    enum class BeltState{
        STOP,
        RUN_REVERS,
        RUN
    }

    private lateinit var _beltMotor: MotorOnly
//    private lateinit var _intakeColorSensor1: AdafruitI2cColorSensor
//    private lateinit var _optopar1: AnalogInput
//    private lateinit var _optopar2: AnalogInput
//
    var beltState = BeltState.STOP
//    var ballColor = Ball.Name.NONE
//    var isBallOnTurret = false

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
        _beltMotor = MotorOnly(hardwareMap.get("beltMotors") as DcMotorEx)

        val pushServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.PUSH_SERVO) as Servo
        val gateServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.GATE_SERVO) as Servo

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

            _beltMotor.direction = Configs.STORAGE.SORTING_STORAGE_BELT_MOTORS_DIRECTION
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            pushServo.position = Configs.STORAGE.PUSH_SERVO_CLOSE_VALUE
            gateServo.position = Configs.STORAGE.GATE_SERVO_CLOSE_VALUE
        }
    }

    override fun dispose() {

    }
}