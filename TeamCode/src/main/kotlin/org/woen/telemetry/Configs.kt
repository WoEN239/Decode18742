package org.woen.telemetry


import kotlin.math.PI
import kotlin.math.max
import kotlin.math.ceil

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotorSimple

import org.woen.enumerators.Shooting
import org.woen.enumerators.StorageSlot

import org.woen.utils.units.Vec2
import org.woen.utils.units.Angle
import org.woen.utils.units.Triangle
import org.woen.utils.units.Orientation
import org.woen.utils.process.RunStatus

import org.woen.utils.regulator.RegulatorParameters



object Configs {

    @Config
    internal object BRUSH {
        @JvmField
        var BRUSH_SAFE_TIME = 0.5

        @JvmField
        var BRUSH_ERR_TIME = 0.5

        @JvmField
        var BRUSH_DEF_TIME = 1.0

        @JvmField
        var BRUSH_TARGET_CURRENT = 5.0

        @JvmField
        var BRUSH_BIG_TARGET_CURRENT = 7.0

        @JvmField
        var BRUSH_STOP_TIME =0.5
        @JvmField
        var TIME_FOR_BRUSH_REVERSING: Long = 1500

        @JvmField
        var BRUSH_POWER = 10.0
    }

    @Config
    internal object TELEMETRY {
        @EventConfig
        var TELEMETRY_UPDATE_HZ = ThreadedTelemetry.EventValueProvider(5)

        @JvmField
        var TELEMETRY_ENABLE = true
    }

    @Config
    internal object SERVO_ANGLE {
        @JvmField
        var DEFAULT_SERVO_ANGLE = PI * 1.5

        @JvmField
        var DEFAULT_SERVO_V_MAX = PI * 1.5 * 15.0

        @JvmField
        var DEFAULT_SERVO_A = PI * 1.5 * 15.0

        @JvmField
        var DEFAULT_SERVO_OFFSET = 0.1
    }

    @Config
    internal object THREAD_POOL {
        @JvmField
        var THREAD_POOL_THREADS_COUNT = 5
    }

    @Config
    internal object ODOMETRY {
        @JvmField
        var START_RED_ORIENTATION = Orientation(Vec2(1.631, 0.39), Angle(PI))

        @JvmField
        var START_BLUE_ORIENTATION = Orientation(Vec2(1.631, -0.39), Angle(PI))

        @JvmField
        var X_ODOMETER_POSITION = -0.09

        @JvmField
        var Y_ODOMETER_POSITION = 0.15
    }

    @Config
    internal object DRIVE_TRAIN {
        @JvmField
        var DRIVE_SIDE_REGULATOR_PARAMS = RegulatorParameters(kF = 5.6)

        @JvmField
        var DRIVE_FORWARD_REGULATOR_PARAMS = RegulatorParameters(kF = 5.1)

        @JvmField
        var DRIVE_ROTATE_REGULATOR_PARAMS = RegulatorParameters(kF = 1.1)

        @JvmField
        var DRIVE_VEC_MULTIPLIER = 2.0

        @JvmField
        var DRIVE_ANGLE_MULTIPLIER = 12.0

        @JvmField
        var POW_MOVE_ENABLED = true

        @JvmField
        var ROBOT_SIZE = Vec2(0.38, 0.38)

        @JvmField
        var SHOOT_SHORT_TRIANGLE = Triangle(Vec2(-1.83, 1.83), Vec2(0.0, 0.0), Vec2(-1.83, -1.83))

        @JvmField
        var SHOOT_LONG_TRIANGLE = Triangle(Vec2(1.83, 0.61), Vec2(1.22, 0.0), Vec2(1.83, -0.61))

        @JvmField
        var LOOK_REGULATOR_PARAMETERS = RegulatorParameters(kP = 14.0, kD = 1.0, limitU = 12.0)

        @JvmField
        var LOOK_SENS = 0.1

        @JvmField
        var LOOK_TARGET_TIMER = 0.1
    }

    @Config
    internal object ROAD_RUNNER {
        @JvmField
        var ROAD_RUNNER_POS_X_P = 5.0

        @JvmField
        var ROAD_RUNNER_POS_Y_P = 5.0

        @JvmField
        var ROAD_RUNNER_POS_H_P = 12.0

        @JvmField
        var ROAD_RUNNER_TRANSLATE_VELOCITY = 2.0

        @JvmField
        var ROAD_RUNNER_ROTATE_VELOCITY = 12.0

        @JvmField
        var ROAD_RUNNER_ROTATE_ACCEL = 6.0

        @JvmField
        var ROAD_RUNNER_MIN_TRANSLATION_ACCEL = -1.0

        @JvmField
        var ROAD_RUNNER_MAX_TRANSLATION_ACCEL = 1.0
    }

    @Config
    internal object CAMERA {
        @JvmField
        var CAMERA_ENABLE = true
    }

    @Config
    internal object TURRET {
        @JvmField
        var PULLEY_RADIUS = 0.05

        @JvmField
        var PULLEY_TICKS_IN_REVOLUTION = 28.0

        @JvmField
        var PULLEY_REGULATOR = RegulatorParameters(kP = 0.015, kF = 0.0042)

        @JvmField
        var PULLEY_TARGET_SENS = 0.4

        @JvmField
        var PULLEY_TARGET_TIMER = 0.1

        @EventConfig
        var PULLEY_VELOCITY_FILTER_COEF = ThreadedTelemetry.EventValueProvider(0.3)

        @JvmField
        var BLUE_BASKET_POSITION = Vec2(-3.66 / 2.0 - 0.22, -3.66 / 2.0)

        @JvmField
        var RED_BASKET_POSITION = Vec2(-3.66 / 2.0 - 0.22, 3.66 / 2.0)

        @JvmField
        var TURRET_HEIGHT = 0.35

        @JvmField
        var BALL_MASS = 0.075

        @JvmField
        var BASKET_TARGET_HEIGHT = 1.05

        @JvmField
        var TIME_STEP = 0.05

        @JvmField
        var CALCULATING_G = 9.78

        @JvmField
        var APPROXIMATION_MAX_ITERATIONS = 100

        @JvmField
        var MIN_TURRET_ANGLE_SERVO = 0.0

        @JvmField
        var MAX_TURRET_ANGLE_SERVO = 0.6

        @JvmField
        var MIN_TURRET_ANGLE = 45.0 / 180.0 * PI

        @JvmField
        var MAX_TURRET_ANGLE = 55.0 / 180.0 * PI

        @JvmField
        var PULLEY_U = 0.3395

        @JvmField
        var AIR_FORCE_K = 0.0

        @JvmField
        var TURRET_SHOOT_POS = Vec2(0.0, 0.06)

        @JvmField
        var TURRET_CENTER_POS = Vec2(0.0, -0.38 / 2.0 + 0.085)

        @JvmField
        var TURRET_SHOOT_DETECT_CURRENT: Double = 3.7

        @JvmField
        var SHOOT_TRIGGER_DELAY = 0.008

        @JvmField
        var SHORT_ANGLE_POSITION = 50.0 / 180.0 * PI

        @JvmField
        var LONG_ANGLE_POSITION = 45.0 / 180.0 * PI

        @JvmField
        var MINIMAL_PULLEY_VELOCITY = 5.0

        @JvmField
        var MAX_MOTOR_RPS = 85.0

        @JvmField
        var ZERO_ROTATE_POS = 0.51

        @JvmField
        var ZEROING_TIME = 0.1

        @JvmField
        var ROTATE_SERVO_RATIO = 36.0 / 110.0

        @JvmField
        var ROTATE_SERVO_TURNS = PI * 2.0 * 5.0

        @JvmField
        var ROTATE_ENCODER_RATIO = 17.0 / 110.0

        @JvmField
        var ENCODER_TICKS_IN_REVOLUTION = 8192.0

        @JvmField
        var MAX_ROTATE = (0.58 - ZERO_ROTATE_POS) * ROTATE_SERVO_TURNS * ROTATE_SERVO_RATIO

        @JvmField
        var MIN_ROTATE = (0.405 - ZERO_ROTATE_POS) * ROTATE_SERVO_TURNS * ROTATE_SERVO_RATIO

        @JvmField
        var ACCEL_K = 10.5

        @JvmField
        var ACCEL_THRESHOLD = 0.55
    }

    @Config
    internal object STORAGE_SENSORS {

        @JvmField
        var OPTIC_PARE_SEES_NOT_BLACK = 0.4


        @JvmField
        var THRESHOLD_GREEN_BALL_MAX_R_S1 = 100

        @JvmField
        var THRESHOLD_GREEN_BALL_MIN_G_S1 = 50

        @JvmField
        var THRESHOLD_GREEN_BALL_MAX_B_S1 = 220

        @JvmField
        var THRESHOLD_GREEN_BALL_MAX_R_S2 = 70

        @JvmField
        var THRESHOLD_GREEN_BALL_MIN_G_S2 = 40

        @JvmField
        var THRESHOLD_GREEN_BALL_MAX_B_S2 = 70


        @JvmField
        var THRESHOLD_PURPLE_BALL_MIN_R_S1 = 80

        @JvmField
        var THRESHOLD_PURPLE_BALL_MAX_G_S1 = 150

        @JvmField
        var THRESHOLD_PURPLE_BALL_MIN_B_S1 = 80

        @JvmField
        var THRESHOLD_PURPLE_BALL_MIN_R_S2 = 40

        @JvmField
        var THRESHOLD_PURPLE_BALL_MAX_G_S2 = 70

        @JvmField
        var THRESHOLD_PURPLE_BALL_MIN_B_S2 = 20


        @JvmField
        var CONST_MAXIMUM_READING = 10240.0

        @JvmField
        var ACCUMULATION_INTERVAL_MS = 24

        @JvmField
        var VAR_MAXIMUM_READING = (
                65535.coerceAtMost(
                    1024 * (256 - max(
                        0, 256 - ceil(
                            (
                                    ACCUMULATION_INTERVAL_MS
                                            / 2.4f
                                ).toDouble()
                        ).toInt()
                    ))
                )
                ).toDouble()
    }


    @Config
    internal object PROCESS_ID {

        @JvmField
        var PRIORITY_SETTING_FOR_SORTING_STORAGE = RunStatus.Priority.PRIORITIZE_HIGH_PROCESS_ID


        @JvmField
        var UNDEFINED_PROCESS_ID = 0


        @JvmField
        var INTAKE = 1

        @JvmField
        var LAZY_INTAKE = 2

        @JvmField
        var DRUM_REQUEST = 3

        @JvmField
        var SINGLE_REQUEST = 4
    }



    @Config
    internal object DELAY {

        @JvmField
        var INTAKE_RACE_CONDITION_MS: Long = 15

        @JvmField
        var REQUEST_RACE_CONDITION_MS: Long = 10

        @JvmField
        var EVENT_AWAITING_MS: Long = 5

        @JvmField
        var HARDWARE_REQUEST_FREQUENCY_MS: Long = 5


        @JvmField
        var ONE_BALL_PUSHING_MS: Long = 411

        @JvmField
        var SORTING_REALIGNING_FORWARD_MS: Long = 100

        @JvmField
        var SORTING_REALIGNING_REVERSE_MS: Long = 50

        @JvmField
        var MAX_SHOT_AWAITING_MS: Long = 160


        @JvmField
        var BETWEEN_INTAKES_MS: Long = 1111


    }



    @Config
    internal object STORAGE {

        @JvmField
        var MAX_BALL_COUNT = 3

        @JvmField
        var STORAGE_SLOT_COUNT = 4



        @JvmField
        var PREFERRED_INTAKE_SLOT_ORDER = arrayOf(
            StorageSlot.BOTTOM,
            StorageSlot.CENTER,
            StorageSlot.TURRET,
            StorageSlot.MOBILE)

        @JvmField
        var PREFERRED_REQUEST_SLOT_ORDER = arrayOf(
            StorageSlot.TURRET,
            StorageSlot.CENTER,
            StorageSlot.BOTTOM,
            StorageSlot.MOBILE)



        @JvmField
        var SORTING_STORAGE_BELT_MOTORS_DIRECTION = DcMotorSimple.Direction.REVERSE


        @JvmField
        var GATE_SERVO_OPEN_VALUE = 0.20

        @JvmField
        var GATE_SERVO_CLOSE_VALUE = 1.0

        @JvmField
        var PUSH_SERVO_OPEN_VALUE = 0.67

        @JvmField
        var PUSH_SERVO_CLOSE_VALUE = 0.45


        @JvmField
        var POWER_FOR_FAST_BELT_ROTATING = 12.0

        @JvmField
        var POWER_FOR_SLOW_BELT_ROTATING = 9.0

        @JvmField
        var TURRET_GATE_SERVO_OPEN_VALUE = 0.4

        @JvmField
        var TURRET_GATE_SERVO_CLOSE_VALUE = 0.535
    }


    @Config
    internal object SORTING_AUTO_OPMODE {

        @JvmField
        var IS_SORTING_MODULE_ACTIVE_AT_START_UP = true


        @JvmField
        var MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS: Long = 2000

        @JvmField
        var MAX_ATTEMPTS_FOR_PATTERN_DETECTION = 2

        @JvmField
        var TRY_RECALIBRATE_IF_SOMETHING_FAILS = true


        @JvmField
        var DEFAULT_SHOOTING_MODE = Shooting.Mode.FIRE_UNTIL_PATTERN_IS_BROKEN

        @JvmField
        var DEFAULT_PATTERN = Shooting.StockPattern.Name.USE_DETECTED_PATTERN

        @JvmField
        var FAILSAFE_SHOOTING_MODE = Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE

        @JvmField
        var FAILSAFE_PATTERN = Shooting.StockPattern.Name.ANY
    }


    @Config
    internal object HARDWARE_DEVICES_NAMES {
        @JvmField
        var INTAKE_COLOR_SENSOR_1 = "color1"

        @JvmField
        var INTAKE_COLOR_SENSOR_2 = "color2"


        @JvmField
        var TURRET_OPTIC_PARE_1 = "turret_optic_pare_1"

        @JvmField
        var TURRET_OPTIC_PARE_2 = "turret_optic_pare_2"


        @JvmField
        var TURRET_GATE_SERVO = "turret_gate_servo"


        @JvmField
        var SORTING_STORAGE_BELT_MOTORS = "beltMotors"


        @JvmField
        var GATE_SERVO = "gate_servo"

        @JvmField
        var PUSH_SERVO = "push_servo"
    }

    @Config
    internal object SIMPLE_STORAGE {
        @JvmField
        var BELT_PUSH_TIME = 0.15

        @JvmField
        var LOOK_DELAY_TIME = 0.09

        @JvmField
        var REVERS_TIME = 0.2

        @JvmField
        var BELTS_FULL_CURRENT = 8.1

        @JvmField
        var BELTS_FULL_TIMER = 0.1

        @JvmField
        var BELTS_POWER = 5.0

        @JvmField
        var BELTS_FAST_POWER = 7.3

        @JvmField
        var BELTS_FAST_FAST_POWER = 10.0
    }
}