package org.woen.modules

import android.annotation.SuppressLint
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.woen.collector.Collector
import org.woen.collector.RunMode
import org.woen.modules.drivetrain.GetRobotOdometry
import org.woen.utils.drivers.SOFT_SERVO_CONFIG
import org.woen.utils.drivers.SoftServo
import org.woen.utils.exponentialFilter.ExponentialFilter

@Config
internal object SIMPLE_STORAGE_CONFIG {
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
    var GATE_OPEN = 0.83

    @JvmField
    var SORTING_PUSH_TIME = 0.4

    @JvmField
    var SORTING_REVERSE_TIME = 0.1

    @JvmField
    var SLOW_SHOOTING_POWER = 8.5

    @JvmField
    var SLOW_SHOOTING_TIMER = 0.9

    @JvmField
    var SORTING_SLOW_BELTS_POWER = 9.0

    @JvmField
    var SORTING_BELT_POWER = 11.0

    @JvmField
    var FULL_R = 2.6

    @JvmField
    var TWO_R = 3.7

    @JvmField
    var ONE_R = 7.0

    @JvmField
    var LAUNCH_SERVO_OPEN = 0.57

    @JvmField
    var LAUNCH_SERVO_CLOSE = 0.98

    @JvmField
    var SHOOT_LAUNCH_BELT_POWER = 11.0

    @JvmField
    var SHOOT_LAUNCH_TIME = 0.5

    @JvmField
    var FAR_SHOOT_POWER = 7.0

    @JvmField
    var R_FILTER_K = 0.2
}

enum class StorageState {
    STOP,
    SHOOTING,
    EATING,
    SORTING,
    STOP_SHOOTING,
    SLOW_SHUTTING,
    LAUNCH_SHOOT
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
class SlowShootEvent()
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
    val gateServo = SoftServo(
        "gateServo", collector.hardwareMap, SIMPLE_STORAGE_CONFIG.GATE_CLOSE,
        SOFT_SERVO_CONFIG.SERVO_E * 2.0
    )

    val launchServo =
        SoftServo("launchServo", collector.hardwareMap, SIMPLE_STORAGE_CONFIG.LAUNCH_SERVO_CLOSE)

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
    var launchRevers = false
    var ballInStorage = 0
    val rFilter = ExponentialFilter(SIMPLE_STORAGE_CONFIG.R_FILTER_K)
    var beltR = 10.0

    fun switchState(state: StorageState) {
        currentState = state
        stateTimer.reset()

        when (currentState) {
            StorageState.STOP -> {
                turretGateServo.targetPosition = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_CLOSE
                pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_CLOSE
                gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_CLOSE
                launchServo.targetPosition = SIMPLE_STORAGE_CONFIG.LAUNCH_SERVO_CLOSE

                beltMotor.power = 0.0
                brushMotor.power = 0.0
            }

            StorageState.SHOOTING -> {
                turretGateServo.targetPosition = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_OPEN

                beltMotor.power = 1.0
                brushMotor.power = -1.0

                stateTimer.reset()
                launchRevers = false
            }

            StorageState.EATING -> {
                beltMotor.power = 1.0
                brushMotor.power = 1.0
            }

            StorageState.SORTING -> {
                sortingState = SortingState.GATE_OPEN

                pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_CLOSE
                gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_OPEN

                sortingTimer.reset()

                brushMotor.power = 0.0
            }

            StorageState.STOP_SHOOTING -> {
                brushMotor.power = 0.0
                beltMotor.power = -1.0

                stateTimer.reset()
            }

            StorageState.SLOW_SHUTTING -> {
                stateTimer.reset()
                turretGateServo.targetPosition = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_OPEN
                brushMotor.power = 0.0
            }

            StorageState.LAUNCH_SHOOT -> {
                stateTimer.reset()
                turretGateServo.targetPosition = SIMPLE_STORAGE_CONFIG.TURRET_GATE_SERVO_OPEN
                brushMotor.power = 0.0
            }
        }
    }

    collector.eventBus.subscribe(StartSortingEvent::class) {
        if (currentState == StorageState.STOP || currentState == StorageState.EATING) {
            val pattern = collector.eventBus.invoke(GetCurrentPatternEvent()).pattern

            val patternGreenPosition = if (pattern == null) 1 else
                when (BallColor.GREEN) {
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

            requiredSwaps = storageGreenPosition - patternGreenPosition

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
            StorageState.SORTING -> StorageState.SORTING
            StorageState.STOP_SHOOTING -> StorageState.SHOOTING
            StorageState.SLOW_SHUTTING -> StorageState.SHOOTING
            StorageState.LAUNCH_SHOOT -> StorageState.SHOOTING
        }
    }

    collector.eventBus.subscribe(StartEatEvent::class) {
        if (currentState == StorageState.STOP)
            switchState(StorageState.EATING)
    }

    collector.eventBus.subscribe(StopEatEvent::class) {
        if (currentState == StorageState.EATING)
            switchState(StorageState.STOP)
    }

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.left_bumper }, {
        collector.eventBus.invoke(StartEatEvent())
    })))

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.left_bumper }, {
        collector.eventBus.invoke(StopEatEvent())
    }, false)))

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.right_bumper }, {
        switchState(StorageState.SHOOTING)
        ballInStorage = 0
        collector.eventBus.invoke(BallCountUpdateEvent(ballInStorage))
    })))

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.right_bumper }, {
        if (currentState == StorageState.SHOOTING)
            switchState(StorageState.STOP_SHOOTING)
    }, false)))

    collector.eventBus.subscribe(ShootEvent::class) {
        if (currentState == StorageState.STOP || currentState == StorageState.EATING || currentState == StorageState.STOP_SHOOTING) {
            switchState(StorageState.LAUNCH_SHOOT)
            ballInStorage = 0
            collector.eventBus.invoke(BallCountUpdateEvent(ballInStorage))
        }
    }

    collector.eventBus.subscribe(SlowShootEvent::class) {
        if (currentState == StorageState.STOP || currentState == StorageState.EATING || currentState == StorageState.STOP_SHOOTING) {
            switchState(StorageState.SLOW_SHUTTING)
            ballInStorage = 0
            collector.eventBus.invoke(BallCountUpdateEvent(ballInStorage))
        }
    }

    collector.eventBus.invoke(AddGamepad1ListenerEvent(ClickGamepadListener({ it.touchpad }, {
        if (currentState == StorageState.STOP || currentState == StorageState.EATING) {
            requiredSwaps = 1
            switchState(StorageState.SORTING)
        }
    })))

    collector.startEvent += {
        pushServo.start()
        gateServo.start()
        turretGateServo.start()
        launchServo.start()
        rFilter.start()

        switchState(StorageState.STOP)
    }

    collector.updateEvent += {
        pushServo.update()
        gateServo.update()
        turretGateServo.update()
        launchServo.update()
        rFilter.coef = SIMPLE_STORAGE_CONFIG.R_FILTER_K

        val current = beltMotor.getCurrent(CurrentUnit.AMPS)

        if (current > 1.0) {
            val rawR = collector.battery.currentVoltage / beltMotor.getCurrent(CurrentUnit.AMPS)

            beltR = rFilter.updateRaw(beltR, rawR - beltR)
        } else {
            rFilter.updateRaw(beltR, 0.0)
            beltR = 10.0
        }

        if (currentState == StorageState.EATING && collector.runMode == RunMode.MANUAL) {
            if (beltR < SIMPLE_STORAGE_CONFIG.FULL_R) {
                if (ballInStorage <= 2) {
                    ballInStorage = 3

                    collector.opMode.gamepad1.rumble(200)

                    collector.eventBus.invoke(BallCountUpdateEvent(ballInStorage))
                }
            } else if (beltR < SIMPLE_STORAGE_CONFIG.TWO_R) {
                if (ballInStorage <= 1) {
                    ballInStorage = 2

                    collector.eventBus.invoke(BallCountUpdateEvent(ballInStorage))
                }
            } else if (beltR < SIMPLE_STORAGE_CONFIG.ONE_R) {
                if (ballInStorage == 0) {
                    ballInStorage = 1

                    collector.eventBus.invoke(BallCountUpdateEvent(ballInStorage))
                }
            }
        }

        collector.telemetry.addData("ball count", ballInStorage)
        collector.telemetry.addData("belt r", beltR)

        when (currentState) {
            StorageState.SHOOTING -> {
                if (launchRevers)
                    beltMotor.power = -1.0
                else {
                    if (collector.eventBus.invoke(GetRobotOdometry()).orientation.x > 0.5)
                        beltMotor.power =
                            battery.voltageToPower(SIMPLE_STORAGE_CONFIG.FAR_SHOOT_POWER)
                    else
                        beltMotor.power = 1.0
                }

                if (stateTimer.seconds() > (if (collector.eventBus.invoke(GetRobotOdometry()).orientation.x > 0.5) 1.2 else 0.9) && !launchRevers) {
                    launchServo.targetPosition = SIMPLE_STORAGE_CONFIG.LAUNCH_SERVO_OPEN

                    if (launchServo.atTarget) {
                        launchServo.targetPosition = SIMPLE_STORAGE_CONFIG.LAUNCH_SERVO_CLOSE

                        launchRevers = true

                        stateTimer.reset()
                    }
                }

                if (launchRevers && launchServo.atTarget) {
                    launchRevers = false
                    stateTimer.reset()
                }
            }

            StorageState.EATING, StorageState.STOP -> {}

            StorageState.SLOW_SHUTTING -> {
                beltMotor.power = battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SLOW_SHOOTING_POWER)

                if (stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.SLOW_SHOOTING_TIMER) {
                    launchServo.targetPosition = SIMPLE_STORAGE_CONFIG.LAUNCH_SERVO_OPEN

                    if (launchServo.atTarget) {
                        launchServo.targetPosition = SIMPLE_STORAGE_CONFIG.LAUNCH_SERVO_CLOSE

                        switchState(StorageState.STOP_SHOOTING)
                    }
                }
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
                        } else
                            beltMotor.power =
                                -battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SORTING_BELT_POWER)
                    }

                    SortingState.PUSH_OPEN -> {
                        if (pushServo.atTarget) {
                            sortingState = SortingState.WAIT_PUSH

                            pushServo.targetPosition = SIMPLE_STORAGE_CONFIG.PUSH_CLOSE
                        }
                    }

                    SortingState.WAIT_PUSH -> {
                        if (pushServo.atTarget) {
                            beltMotor.power =
                                battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SORTING_BELT_POWER)

                            if (requiredSwaps == 0)
                                gateServo.targetPosition = SIMPLE_STORAGE_CONFIG.GATE_CLOSE
                        } else
                            beltMotor.power =
                                battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SORTING_SLOW_BELTS_POWER)

                        if (gateServo.atTarget && pushServo.atTarget) {
                            if (sortingTimer.seconds() > SIMPLE_STORAGE_CONFIG.SORTING_PUSH_TIME) {
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
                        if (sortingTimer.seconds() > SIMPLE_STORAGE_CONFIG.SORTING_REVERSE_TIME) {
                            if (collector.runMode == RunMode.MANUAL)
                                collector.opMode.gamepad1.rumble(200)

                            switchState(StorageState.STOP)
                        } else
                            beltMotor.power =
                                -battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SORTING_BELT_POWER)
                    }
                }
            }

            StorageState.LAUNCH_SHOOT -> {
                beltMotor.power =
                    battery.voltageToPower(SIMPLE_STORAGE_CONFIG.SHOOT_LAUNCH_BELT_POWER)

                if (stateTimer.seconds() > SIMPLE_STORAGE_CONFIG.SHOOT_LAUNCH_TIME) {
                    launchServo.targetPosition = SIMPLE_STORAGE_CONFIG.LAUNCH_SERVO_OPEN

                    if (launchServo.atTarget) {
                        launchServo.targetPosition = SIMPLE_STORAGE_CONFIG.LAUNCH_SERVO_CLOSE

                        switchState(StorageState.STOP_SHOOTING)
                    }
                }
            }
        }
    }
}