package org.woen.modules.scoringSystem.simple

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.events.SimpleEvent
import org.woen.utils.motor.MotorOnly

class HardwareSimpleStorage : IHardwareDevice {
    enum class BeltState {
        STOP,
        RUN_REVERS,
        RUN_REVERS_FAST,
        RUN,
        RUN_FAST
    }

    private lateinit var _beltMotor: MotorOnly

    var beltState = BeltState.STOP
    var beltsCurrent = 0.0

    val currentTriggerEvent = SimpleEvent<Int>()

    private var _fullTriggerTimer = ElapsedTime()

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState != HotRun.RunState.RUN)
            return

        _beltMotor.power = when (beltState) {
            BeltState.STOP -> 0.0

            BeltState.RUN_REVERS -> -ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)

            BeltState.RUN -> ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_POWER)

            BeltState.RUN_REVERS_FAST -> -ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_FAST_POWER)

            BeltState.RUN_FAST -> ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.SIMPLE_STORAGE.BELTS_FAST_POWER)
        }

        beltsCurrent = _beltMotor.getCurrent(CurrentUnit.AMPS)

        if (beltsCurrent > Configs.SIMPLE_STORAGE.BELTS_FULL_CURRENT) {
            if (_fullTriggerTimer.seconds() > Configs.SIMPLE_STORAGE.BELTS_FULL_TIMER)
                currentTriggerEvent.invoke(0)
        } else
            _fullTriggerTimer.reset()
    }

    override fun init(hardwareMap: HardwareMap) {
        _beltMotor =
            MotorOnly(hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.SORTING_STORAGE_BELT_MOTORS) as DcMotorEx)

        val pushServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.PUSH_SERVO) as Servo
        val gateServo = hardwareMap.get(Configs.HARDWARE_DEVICES_NAMES.GATE_SERVO) as Servo

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

            _beltMotor.direction = Configs.STORAGE.SORTING_STORAGE_BELT_MOTORS_DIRECTION
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            pushServo.position = Configs.STORAGE.PUSH_SERVO_CLOSE_VALUE
            gateServo.position = Configs.STORAGE.GATE_SERVO_CLOSE_VALUE

            _fullTriggerTimer.reset()
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("beltsCurrent", beltsCurrent)
        }
    }

    override fun dispose() {

    }
}