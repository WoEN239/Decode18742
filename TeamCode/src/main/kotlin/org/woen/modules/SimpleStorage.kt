package org.woen.modules

import android.annotation.SuppressLint
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.woen.collector.Collector
import org.woen.collector.RunMode
import org.woen.modules.drivetrain.GetRobotOdometry
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
    var GATE_CLOSE = 0.2

    @JvmField
    var GATE_OPEN = 0.76

    @JvmField
    var SORTING_PUSH_TIME = 0.4

    @JvmField
    var SORTING_REVERSE_TIME = 0.1

    @JvmField
    var SLOW_SHOOTING_POWER = 9.0

    @JvmField
    var SLOW_SHOOTING_TIMER = 0.9

    @JvmField
    var SORTING_SLOW_BELTS_POWER = 9.0

    @JvmField
    var SORTING_BELT_POWER = 11.0

    @JvmField
    var EATING_TIME = 0.15

    @JvmField
    var CURRENT_TRIGGER_TIME = 0.15

    @JvmField
    var TRIGGER_CURRENT = 2.0
}

enum class StorageState {
    STOP,
    SHOOTING,
    EATING,
    STOP_EATING,
    SORTING,
    START_SHOOTING,
    STOP_SHOOTING,
    START_SLOW_SHUTTING,
    SLOW_SHUTTING,
}

enum class SortingState {
    PUSH_OPEN,
    GATE_OPEN,
    WAIT_PUSH,
    REVERSE_BELTS
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

    val battery = collector.battery

    val pushServo = SoftServo(
        "pushServo",
        collector.hardwareMap,
        SIMPLE_STORAGE_CONFIG.PUSH_CLOSE,
        SOFT_SERVO_CONFIG.SERVO_E * 2.0
    )
    val gateServo = SoftServo("gateServo", collector.hardwareMap, SIMPLE_STORAGE_CONFIG.GATE_CLOSE,
        SOFT_SERVO_CONFIG.SERVO_E * 2.0)

    val launchServo = collector.hardwareMap.get("launchServo") as Servo

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
    val eatingTimer = ElapsedTime()
    val currentTriggerTimer = ElapsedTime()

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
                brushMotor.power = 1.0

                stateTimer.reset()
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

            StorageState.START_SLOW_SHUTTING -> {
                turretGateServo.targetPosition = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_OPEN

                brushMotor.power = 0.0
                beltMotor.power = 0.0
            }

            StorageState.SLOW_SHUTTING -> {
                stateTimer.reset()
            }
        }
    }

    collector.eventBus.subscribe(StartSortingEvent::class) {
        val pattern = collector.eventBus.invoke(GetCurrentPatternEvent()).pattern

        if ((currentState == StorageState.STOP || currentState == StorageState.EATING || currentState == StorageState.STOP_EATING) && pattern != null) {
            val patternGreenPosition = when (BallColor.GREEN) {
                pattern[0] -> 1
                pattern[1] -> 2
                pattern[2] -> 3
                else -> 0
            }

            val storageGreenPosition = when (BallColor.GREEN) {
                it.ball1 -> 1
                it.ball2 -> 2
                it.ball3 -> 3
                else -> 0
            }

            requiredSwaps = patternGreenPosition - storageGreenPosition

            if (requiredSwaps < 0)
                requiredSwaps += 3

            if (requiredSwaps != 0)
                switchState(StorageState.SORTING)
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
            StorageState.SLOW_SHUTTING -> StorageState.SHOOTING
            StorageState.START_SLOW_SHUTTING -> StorageState.SHOOTING
        }
    }

    collector.eventBus.subscribe(StartEatEvent::class) {
        if (currentState == StorageState.STOP || currentState == StorageState.STOP_EATING)
            switchState(StorageState.EATING)
    }

    collector.eventBus.subscribe(StopEatEvent::class) {
        if (currentState == StorageState.EATING)
            switchState(StorageState.STOP_EATING)
    }

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.left_bumper }, {
        collector.eventBus.invoke(StartEatEvent())
    })))

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.left_bumper }, {
        collector.eventBus.invoke(StopEatEvent())
    }, false)))

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.right_bumper }, {
        collector.eventBus.invoke(ShootEvent())
    })))

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.right_bumper }, {
        if (currentState == StorageState.SHOOTING)
            switchState(StorageState.STOP_SHOOTING)
    }, false)))

    collector.eventBus.subscribe(ShootEvent::class) {
        if (currentState == StorageState.STOP || currentState == StorageState.EATING || currentState == StorageState.STOP_EATING ||
            currentState == StorageState.STOP_SHOOTING
        )
            switchState(StorageState.START_SHOOTING)
        else
            collector.opMode.gamepad1.rumble(200)
    }

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.touchpad }, {
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

        launchServo.position = 0.98

        switchState(StorageState.STOP)
    }

    collector.updateEvent += {
        pushServo.update()
        gateServo.update()
        turretGateServo.update()

        if (currentState == StorageState.EATING) {
            if (eatingTimer.seconds() > SIMPLE_STORAGE_CONFIG.EATING_TIME) {
                if (brushMotor.getCurrent(CurrentUnit.AMPS) > SIMPLE_STORAGE_CONFIG.TRIGGER_CURRENT) {
                    if (currentTriggerTimer.seconds() > SIMPLE_STORAGE_CONFIG.CURRENT_TRIGGER_TIME)
                        collector.opMode.gamepad1.rumble(200)
                } else
                    currentTriggerTimer.reset()
            }
        } else {
            eatingTimer.reset()
            currentTriggerTimer.reset()
        }

        val timer = ElapsedTime()

        when (currentState) {
            StorageState.SHOOTING -> {
                if(collector.eventBus.invoke(GetRobotOdometry()).locateInLongShootingArea)
                    beltMotor.power = battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SLOW_SHOOTING_POWER)
                else
                    beltMotor.power = 1.0

                if (stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.SHOOTING_TIME && collector.runMode == RunMode.AUTO)
                    switchState(StorageState.STOP_SHOOTING)
            }

            StorageState.EATING, StorageState.STOP -> {}

            StorageState.STOP_EATING -> {
                switchState(StorageState.STOP)
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
//                if (turretGateServo.atTarget)
                    switchState(StorageState.SHOOTING)
            }

            StorageState.START_SLOW_SHUTTING -> {
                if (turretGateServo.atTarget)
                    switchState(StorageState.SLOW_SHUTTING)
            }

            StorageState.SLOW_SHUTTING -> {
                if (stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.SLOW_SHOOTING_TIMER)
                    switchState(StorageState.STOP)
                else
                    beltMotor.power =
                        battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SLOW_SHOOTING_POWER)
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

                                timer.reset()
                            }
                        } else
                            beltMotor.power =
                                -battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SORTING_BELT_POWER)
                    }

                    SortingState.PUSH_OPEN -> {
                        if (pushServo.atTarget) {
                            sortingState = SortingState.WAIT_PUSH

                            pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_CLOSE
//                            gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_CLOSE
                        }
                    }

                    SortingState.WAIT_PUSH -> {
                        if (pushServo.atTarget) {
                            beltMotor.power =
                                battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SORTING_BELT_POWER)

                            if(requiredSwaps == 0)
                                gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_CLOSE
                        }
                        else
                            beltMotor.power =
                                battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SORTING_SLOW_BELTS_POWER)

                        if (gateServo.atTarget && pushServo.atTarget) {

//                            gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_CLOSE
                            if (sortingTimer.seconds() > SIMPLE_STORAGE_CONFIG.SORTING_PUSH_TIME) {
//                                beltMotor.power = 0.0

                                requiredSwaps--

                                if (requiredSwaps > 0)
                                    switchState(StorageState.SORTING)
                                else {
                                    sortingState = SortingState.REVERSE_BELTS
                                    sortingTimer.reset()
                                }
                            }
                        } else
                            sortingTimer.reset()
                    }

                    SortingState.REVERSE_BELTS -> {
                        if(sortingTimer.seconds() > SIMPLE_STORAGE_CONFIG.SORTING_REVERSE_TIME)
                            switchState(StorageState.STOP)
                        else
                            beltMotor.power = -battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SORTING_BELT_POWER)
                    }
                }
            }
        }
    }
}