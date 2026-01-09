package org.woen.modules.scoringSystem.simple


import com.qualcomm.hardware.rev.RevColorSensorV3
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.hotRun.HotRun
import org.woen.utils.motor.MotorOnly
import org.woen.utils.events.SimpleEvent

import org.woen.telemetry.Configs
import org.woen.telemetry.Configs.STORAGE_SENSORS.CONST_MAXIMUM_READING
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_GREEN_BALL_B_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_GREEN_BALL_G_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_GREEN_BALL_R_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_GREEN_BALL_THRESHOLD
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_PURPLE_BALL_B_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_PURPLE_BALL_G_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_PURPLE_BALL_R_K
import org.woen.telemetry.Configs.STORAGE_SENSORS.LCS_PURPLE_BALL_THRESHOLD
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import kotlin.math.max


class HardwareSimpleStorage : IHardwareDevice {
    enum class BeltState {
        STOP,
        RUN_REVERSE,
        RUN,
        SHOOT
    }

    private lateinit var _beltMotor: MotorOnly

    var beltState = BeltState.STOP
    var beltsCurrent = 0.0

    val currentTriggerEvent = SimpleEvent<Int>()

    private var _fullTriggerTimer = ElapsedTime()

    var isBall = false
        private set

    private lateinit var _pushServo: Servo
    private lateinit var _gateServo: Servo
    private lateinit var _colorSensor: RevColorSensorV3

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState != HotRun.RunState.RUN)
            return

        _beltMotor.power = when (beltState) {
            BeltState.STOP -> 0.0

            BeltState.RUN_REVERSE -> -ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)

            BeltState.RUN -> ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)

            BeltState.SHOOT -> ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_SHOOT_POWER)
        }

        beltsCurrent = _beltMotor.getCurrent(CurrentUnit.AMPS)

        if (beltsCurrent > Configs.SIMPLE_STORAGE.BELTS_FULL_CURRENT)
            if (_fullTriggerTimer.seconds() > Configs.SIMPLE_STORAGE.BELTS_FULL_TIMER)
                currentTriggerEvent.invoke(0)
        else _fullTriggerTimer.reset()

        val argb1 = _colorSensor.normalizedColors
        val r1 = argb1.red   * CONST_MAXIMUM_READING
        val g1 = argb1.green * CONST_MAXIMUM_READING
        val b1 = argb1.blue  * CONST_MAXIMUM_READING

        isBall = (
                (LCS_GREEN_BALL_R_K * r1 + LCS_GREEN_BALL_B_K * b1 + LCS_GREEN_BALL_G_K * g1) -
                        max(r1 * (1 - LCS_GREEN_BALL_R_K),
                            max(
                                b1 * (1 - LCS_GREEN_BALL_B_K),
                                g1 * (1 - LCS_GREEN_BALL_G_K)
                            )
                        ) > LCS_GREEN_BALL_THRESHOLD) ||
                ((LCS_PURPLE_BALL_R_K * r1 + LCS_PURPLE_BALL_B_K * b1 + LCS_PURPLE_BALL_G_K * g1) -
                max(r1 * (1 - LCS_PURPLE_BALL_R_K),
                    max(
                        b1 * (1 - LCS_PURPLE_BALL_B_K),
                        g1 * (1 - LCS_PURPLE_BALL_G_K)
                    )
                ) > LCS_PURPLE_BALL_THRESHOLD)
    }

    override fun init(hardwareMap: HardwareMap) {
        _beltMotor =
            MotorOnly(hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.SORTING_STORAGE_BELT_MOTORS) as DcMotorEx)

        _pushServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.PUSH_SERVO) as Servo
        _gateServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.GATE_SERVO) as Servo

        _colorSensor = hardwareMap.get("rightColorSensor") as RevColorSensorV3

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

            _beltMotor.direction = Configs.STORAGE.BELT_MOTORS_DIRECTION
        }


        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("beltsCurrent", beltsCurrent)
            it.addData("isBall", isBall)
        }
    }

    override fun opModeStart() {
        _pushServo.position = Configs.STORAGE.PUSH_SERVO_CLOSE_VALUE
        _gateServo.position = Configs.STORAGE.GATE_SERVO_CLOSE_VALUE

        _fullTriggerTimer.reset()
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}