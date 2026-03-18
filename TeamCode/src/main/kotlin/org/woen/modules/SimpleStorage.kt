package org.woen.modules

import android.annotation.SuppressLint
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.Collector
import org.woen.utils.drivers.SoftServo

@Config
internal object SIMPLE_STORAGE_CONFIG {
    @JvmField
    var STOP_EATING_TIME = 0.1

    @JvmField
    var BRUSH_REVERS_TIME = 0.3

    @JvmField
    var SHOOTING_TIME = 0.4

    @JvmField
    var TURRET_GATE_SERVO_OPEN = 0.75

    @JvmField
    var TURRET_GATE_SERVO_CLOSE = 0.45

    @JvmField
    var PUSH_CLOSE = 0.25

    @JvmField
    var PUSH_OPEN = 0.025

    @JvmField
    var GATE_CLOSE = 0.29

    @JvmField
    var GATE_OPEN = 0.75

    @JvmField
    var SORTING_PUSH_TIME = 1.5

    @JvmField
    var SORTING_REVERSE_TIME = 0.1
}

enum class StorageState {
    STOP,
    SHOOTING,
    EATING,
    STOP_EATING,
    SORTING
}

enum class SortingState {
    PUSH_OPEN,
    PUSH_CLOSE,
    GATE_OPEN
}

@SuppressLint("DefaultLocale")
fun attachSimpleStorage(collector: Collector) {
    val beltMotor = collector.hardwareMap.get("beltMotor") as DcMotorEx
    val brushMotor = collector.hardwareMap.get("brushMotor") as DcMotorEx

    beltMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    beltMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    brushMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    brushMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

    val leftColor = collector.hardwareMap.get("leftColor") as RevColorSensorV3
    val rightColor = collector.hardwareMap.get("rightColor") as RevColorSensorV3

    val pushServo = SoftServo("pushServo", collector.hardwareMap, SIMPLE_STORAGE_CONFIG.PUSH_CLOSE)
    val gateServo = SoftServo("gateServo", collector.hardwareMap, SIMPLE_STORAGE_CONFIG.GATE_CLOSE)

    val turretGateServo = collector.hardwareMap.get("turretGateServo") as Servo

    var currentState = StorageState.STOP
    var sortingState = SortingState.GATE_OPEN
    val stateTimer = ElapsedTime()
    val sortingTimer = ElapsedTime()

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

            StorageState.SORTING -> {
                sortingState = SortingState.GATE_OPEN

                pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_CLOSE
                gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_OPEN
                beltMotor.power = -1.0

                sortingTimer.reset()

                beltMotor.power = 0.0
                brushMotor.power = 0.0
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

    collector.eventBus.invoke(AddGamepadListenerEvent(ClickGamepadListener({ it.right_bumper }, {
        if (currentState != StorageState.SHOOTING)
            switchState(StorageState.SHOOTING)
    })))

    collector.eventBus.invoke(AddGamepadListenerEvent(ClickGamepadListener({it.touchpad}, {
        switchState(StorageState.SORTING)
    })))

    collector.startEvent += {
        pushServo.start()
        gateServo.start()

        switchState(StorageState.STOP)
    }

    collector.updateEvent += {
        pushServo.update()
        gateServo.update()

        val left = leftColor.normalizedColors
        val right = rightColor.normalizedColors

        val leftR = left.red * 10240.0
        val leftG = left.green * 10240.0
        val leftB = left.blue * 10240.0

        val rightR = right.red * 10240.0
        val rightG = right.green * 10240.0
        val rightB = right.blue * 10240.0

        collector.telemetry.addLine(
            "left = ${String.format("%.1f", leftR)} ${
                String.format(
                    "%.1f",
                    leftG
                )
            } ${String.format("%.1f", leftB)}"
        )
        collector.telemetry.addLine(
            "right = ${
                String.format(
                    "%.1f",
                    rightR
                )
            } ${String.format("%.1f", rightG)} ${String.format("%.1f", rightB)}"
        )

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

            StorageState.SORTING -> {
                when(sortingState){
                    SortingState.GATE_OPEN -> {
                        if(sortingTimer.seconds() > SIMPLE_STORAGE_CONFIG.SORTING_REVERSE_TIME) {
                            beltMotor.power = 0.0

                            if (gateServo.atTarget && pushServo.atTarget) {
                                sortingState = SortingState.PUSH_OPEN

                                pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_OPEN
                            }
                        }
                    }

                    SortingState.PUSH_OPEN -> {
                        if(pushServo.atTarget){
                            sortingState = SortingState.PUSH_CLOSE

                            pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_CLOSE
                            gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_CLOSE

                            sortingTimer.reset()

                            beltMotor.power = 1.0
                        }
                    }

                    SortingState.PUSH_CLOSE -> {
                        if(stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.SORTING_PUSH_TIME){
                            beltMotor.power = 0.0

                            currentState = StorageState.STOP
                        }
                    }
                }
            }
        }
    }
}