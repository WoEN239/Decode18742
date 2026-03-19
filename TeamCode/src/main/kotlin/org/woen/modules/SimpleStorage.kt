package org.woen.modules

import android.annotation.SuppressLint
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.Collector
import org.woen.modules.drivetrain.GetLocateInShootingAreaEvent
import org.woen.utils.drivers.SOFT_SERVO_CONFIG
import org.woen.utils.drivers.SoftServo

@Config
internal object SIMPLE_STORAGE_CONFIG {
    @JvmField
    var STOP_EATING_TIME = 0.1

    @JvmField
    var BRUSH_REVERS_TIME = 0.3

    @JvmField
    var SHOOTING_TIME = 0.6

    @JvmField
    var SHOOTING_REVERSE_TIME = 0.1

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
    var SORTING_PUSH_TIME = 0.5

    @JvmField
    var SORTING_REVERSE_TIME = 0.1

    @JvmField
    var SINGLE_SHOOT_PUSH_TIME = 0.05
}

enum class StorageState {
    STOP,
    SHOOTING,
    EATING,
    STOP_EATING,
    SORTING,
    START_SHOOTING,
    STOP_SHOOTING,
    START_SINGLE_SHOOTING,
    SINGLE_SHOOTING
}

enum class SortingState {
    PUSH_OPEN,
    PUSH_CLOSE,
    GATE_OPEN,
    WAIT_PUSH,
    WAIT_FALL
}

enum class BallColor {
    PURPLE,
    GREEN
}

class StartSortingEvent(val ball1: BallColor, val ball2: BallColor, val ball3: BallColor)
class GetCurrentStorageStateEvent(var state: StorageState = StorageState.STOP)
class ShootEvent()
class StartEatEvent()
class StopEatEvent()

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

    val pushServo = SoftServo("pushServo", collector.hardwareMap, SIMPLE_STORAGE_CONFIG.PUSH_CLOSE, SOFT_SERVO_CONFIG.SERVO_E * 2.0)
    val gateServo = SoftServo("gateServo", collector.hardwareMap, SIMPLE_STORAGE_CONFIG.GATE_CLOSE)

    val turretGateServo = SoftServo(
        "turretGateServo",
        collector.hardwareMap,
        SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_CLOSE
    )

    var currentState = StorageState.STOP
    var sortingState = SortingState.GATE_OPEN
    val stateTimer = ElapsedTime()
    val sortingTimer = ElapsedTime()
    var requiredSwaps = 3

    fun switchState(state: StorageState) {
        currentState = state
        stateTimer.reset()

        when (currentState) {
            StorageState.STOP -> {
                turretGateServo.targetPosition = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_CLOSE
                pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_CLOSE
                gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_CLOSE

                beltMotor.power = 0.0
                brushMotor.power = 0.0
            }

            StorageState.SHOOTING -> {
                beltMotor.power = 1.0
                brushMotor.power = 1.0
            }

            StorageState.EATING -> {
                beltMotor.power = 1.0
                brushMotor.power = 1.0
            }

            StorageState.STOP_EATING -> {
                stateTimer.reset()
            }

            StorageState.SORTING -> {
                sortingState = SortingState.GATE_OPEN

                pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_CLOSE
                gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_OPEN

                sortingTimer.reset()

                beltMotor.power = -1.0
                brushMotor.power = 0.0
            }

            StorageState.START_SHOOTING -> {
                turretGateServo.targetPosition = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_OPEN

                brushMotor.power = 0.0
                beltMotor.power = 0.0
            }

            StorageState.STOP_SHOOTING -> {
                brushMotor.power = 0.0
                beltMotor.power = -1.0

                stateTimer.reset()
            }

            StorageState.START_SINGLE_SHOOTING -> {
                turretGateServo.targetPosition = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_OPEN

                brushMotor.power = 0.0
                beltMotor.power = 0.0
            }

            StorageState.SINGLE_SHOOTING -> {
                beltMotor.power = 1.0
                stateTimer.reset()
            }
        }
    }

    collector.eventBus.subscribe(StartSortingEvent::class) {
        if (currentState == StorageState.STOP || currentState == StorageState.EATING || currentState == StorageState.STOP_EATING) {
            switchState(StorageState.SORTING)

            requiredSwaps = 2
        }
    }

    collector.eventBus.subscribe(GetCurrentStorageStateEvent::class) {
        it.state = when (currentState) {
            StorageState.STOP -> StorageState.STOP
            StorageState.SHOOTING -> StorageState.SHOOTING
            StorageState.EATING -> StorageState.EATING
            StorageState.STOP_EATING -> StorageState.EATING
            StorageState.SORTING -> StorageState.SORTING
            StorageState.START_SHOOTING -> StorageState.SHOOTING
            StorageState.STOP_SHOOTING -> StorageState.SHOOTING
            StorageState.START_SINGLE_SHOOTING -> StorageState.SHOOTING
            StorageState.SINGLE_SHOOTING -> StorageState.SHOOTING
        }
    }

    collector.eventBus.subscribe(StartEatEvent::class){
        if (currentState == StorageState.STOP)
            switchState(StorageState.EATING)
    }

    collector.eventBus.subscribe(StopEatEvent::class){
        if (currentState == StorageState.EATING)
            switchState(StorageState.STOP_EATING)
    }

    collector.eventBus.invoke(AddGamepadListenerEvent(ClickGamepadListener({ it.left_bumper }, {
        collector.eventBus.invoke(StartEatEvent())
    })))

    collector.eventBus.invoke(AddGamepadListenerEvent(ClickGamepadListener({ it.left_bumper }, {
        collector.eventBus.invoke(StopEatEvent())
    }, false)))

    collector.eventBus.subscribe(ShootEvent::class) {
        if ((currentState == StorageState.STOP || currentState == StorageState.EATING || currentState == StorageState.STOP_EATING) &&
            collector.eventBus.invoke(GetLocateInShootingAreaEvent()).locate &&
            collector.eventBus.invoke(GetTurretHeadingIsNormalEvent()).normal
        )
            switchState(StorageState.START_SHOOTING)
    }

    collector.eventBus.invoke(AddGamepadListenerEvent(ClickGamepadListener({ it.right_bumper }, {
        collector.eventBus.invoke(ShootEvent())
    })))

    collector.eventBus.invoke(AddGamepadListenerEvent(ClickGamepadListener({ it.touchpad }, {
        collector.eventBus.invoke(
            StartSortingEvent(
                BallColor.PURPLE,
                BallColor.GREEN,
                BallColor.PURPLE
            )
        )
    })))

    collector.startEvent += {
        pushServo.start()
        gateServo.start()
        turretGateServo.start()

        switchState(StorageState.STOP)
    }

    collector.updateEvent += {
        pushServo.update()
        gateServo.update()
        turretGateServo.update()

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
                    switchState(StorageState.STOP_SHOOTING)
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

            StorageState.START_SHOOTING -> {
                if (turretGateServo.atTarget)
                    switchState(StorageState.SHOOTING)
            }

            StorageState.STOP_SHOOTING -> {
                if (stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.SHOOTING_REVERSE_TIME)
                    switchState(StorageState.STOP)
            }

            StorageState.SORTING -> {
                when (sortingState) {
                    SortingState.GATE_OPEN -> {
                        if (sortingTimer.seconds() > SIMPLE_STORAGE_CONFIG.SORTING_REVERSE_TIME) {
                            beltMotor.power = 0.0

                            if (gateServo.atTarget && pushServo.atTarget) {
                                sortingState = SortingState.PUSH_OPEN

                                pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_OPEN
                            }
                        }
                    }

                    SortingState.PUSH_OPEN -> {
                        if (pushServo.atTarget) {
                            sortingState = SortingState.WAIT_PUSH

                            gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_CLOSE
                        }
                    }

                    SortingState.WAIT_PUSH -> {
                        if (gateServo.atTarget) {
//                            sortingTimer.reset()
//                            gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_OPEN

                            pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_CLOSE
                            sortingState = SortingState.WAIT_FALL
                        }
                    }

                    SortingState.WAIT_FALL -> {
                        if(pushServo.atTarget )//&&sortingTimer.seconds() > SIMPLE_STORAGE_CONFIG.SORING_FALL_WAIT_TIME){
                        {
                            beltMotor.power = 1.0
                            sortingState = SortingState.PUSH_CLOSE
//                            gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_CLOSE
                            sortingTimer.reset()
                        }
                    }

                    SortingState.PUSH_CLOSE -> {
                        if (sortingTimer.seconds() > SIMPLE_STORAGE_CONFIG.SORTING_PUSH_TIME) {
                            beltMotor.power = 0.0

                            requiredSwaps--

                            if (requiredSwaps > 0)
                                switchState(StorageState.SORTING)
                            else
                                switchState(StorageState.STOP)
                        }
                    }
                }
            }

            StorageState.START_SINGLE_SHOOTING -> {
                if (turretGateServo.atTarget)
                    switchState(StorageState.SINGLE_SHOOTING)
            }

            StorageState.SINGLE_SHOOTING -> {
                if(stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.SINGLE_SHOOT_PUSH_TIME) {
                    switchState(StorageState.STOP_SHOOTING)
                    stateTimer.reset()
                }
            }
        }
    }
}