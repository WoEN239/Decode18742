package org.woen.modules

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.Collector

@Config
internal object SIMPLE_STORAGE_CONFIG {
    @JvmField
    var STOP_EATING_TIME = 0.1

    @JvmField
    var BRUSH_REVERS_TIME = 0.1

    @JvmField
    var SHOOTING_TIME = 0.4

    @JvmField
    var TURRET_GATE_SERVO_OPEN = 0.75

    @JvmField
    var TURRET_GATE_SERVO_CLOSE = 0.45
}

enum class StorageState {
    STOP,
    SHOOTING,
    EATING,
    STOP_EATING
}

fun attachSimpleStorage(collector: Collector) {
    val beltMotor = collector.hardwareMap.get("beltMotor") as DcMotorEx
    val brushMotor = collector.hardwareMap.get("brushMotor") as DcMotorEx

    beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    beltMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    brushMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    brushMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

    val pushServo = collector.hardwareMap.get("pushServo") as Servo
    val gateServo = collector.hardwareMap.get("gateServo") as Servo
    val turretGateServo = collector.hardwareMap.get("turretGateServo") as Servo

    var currentState = StorageState.STOP
    val stateTimer = ElapsedTime()

    fun switchState(state: StorageState) {
        currentState = state
        stateTimer.reset()

        when (currentState) {
            StorageState.STOP -> {
                turretGateServo.position = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_CLOSE

                beltMotor.power = 0.0
                brushMotor.power = 0.0
            }

            StorageState.SHOOTING -> {
                turretGateServo.position = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_OPEN

                beltMotor.power = 1.0
                brushMotor.power = 1.0
            }

            StorageState.EATING -> {
                turretGateServo.position = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_CLOSE

                beltMotor.power = 1.0
                brushMotor.power = 1.0
            }

            StorageState.STOP_EATING -> {
                turretGateServo.position = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_CLOSE

                beltMotor.power = 1.0
                brushMotor.power = 1.0

                stateTimer.reset()
            }
        }
    }

    collector.eventBus.invoke(AddGamepadListenerEvent(ClickGamepadListener({ it.left_bumper }, {
        if (currentState == StorageState.STOP)
            switchState(StorageState.EATING)
    })))

    collector.eventBus.invoke(AddGamepadListenerEvent(ClickGamepadListener({ it.left_bumper }, {
        if (currentState == StorageState.EATING)
            switchState(StorageState.STOP_EATING)
    }, false)))

    collector.eventBus.invoke(AddGamepadListenerEvent(ClickGamepadListener({ it.left_bumper }, {
        if (currentState != StorageState.SHOOTING)
            switchState(StorageState.SHOOTING)
    })))

    collector.startEvent += {
        pushServo.position = 0.275
        gateServo.position = 0.0

        switchState(StorageState.STOP)
    }

    collector.updateEvent += {
        when (currentState) {
            StorageState.SHOOTING -> {
                if (stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.SHOOTING_TIME)
                    switchState(StorageState.STOP)
            }

            StorageState.EATING, StorageState.STOP -> {}

            StorageState.STOP_EATING -> {
                if (stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.STOP_EATING_TIME) {
                    if (stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.STOP_EATING_TIME + SIMPLE_STORAGE_CONFIG.BRUSH_REVERS_TIME)
                        switchState(StorageState.STOP)
                    else {
                        beltMotor.power = 0.0
                        brushMotor.power = -1.0
                    }
                }
            }
        }
    }
}