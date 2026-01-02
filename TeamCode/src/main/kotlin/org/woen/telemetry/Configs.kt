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
import kotlin.jvm.JvmField


object Configs {

    @Config
    internal object BRUSH {
        @JvmField
        var BRUSH_SAFE_TIME = 0.5

        @JvmField
        var BRUSH_ERR_TIME = 0.5

        @JvmField
        var BRUSH_DEF_TIME = 1.5

        @JvmField
        var BRUSH_TARGET_CURRENT = 2.5

        @JvmField
        var BRUSH_BIG_TARGET_CURRENT = 1000.0

        @JvmField
        var BRUSH_STOP_TIME =0.5
        @JvmField
        var TIME_FOR_BRUSH_REVERSING: Long = 1500

        @JvmField
        var BRUSH_POWER = 11.0
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
        var START_RED_ORIENTATION = Orientation(Vec2(-(1.215 + 0.38 / 2.0), 0.91 + 0.38 / 2.0 - 0.045), Angle(PI))
        //1.63, 0.39, PI

        @JvmField
        var START_BLUE_ORIENTATION = Orientation(Vec2(-(1.215 + 0.38 / 2.0), -0.91 - 0.38 / 2.0 + 0.045), Angle(PI))
        //1.631, -0.39, PI

        @JvmField
        var X_ODOMETER_POSITION = -0.09

        @JvmField
        var Y_ODOMETER_POSITION = 0.15
    }

    @Config
    internal object DRIVE_TRAIN {
        @JvmField
        var DRIVE_SIDE_REGULATOR_PARAMS = RegulatorParameters(kF = 11.0, kI = 1.0, kP = 12.0)

        @JvmField
        var DRIVE_FORWARD_REGULATOR_PARAMS = RegulatorParameters(kF = 8.0, kI = 0.5, kP = 8.0)

        @JvmField
        var DRIVE_ROTATE_REGULATOR_PARAMS = RegulatorParameters(kF = 2.0, kP = 2.0, kI = 0.25)

        @JvmField
        var MAX_DRIVE_VELOCITY = 2.0

        @JvmField
        var MAX_DRIVE_ANGLE_VELOCITY = 7.0

        @JvmField
        var POW_MOVE_ENABLED = true

        @JvmField
        var ROBOT_SIZE = Vec2(0.38, 0.38)

        @JvmField
        var SHOOT_SHORT_TRIANGLE = Triangle(Vec2(-1.83, 1.83), Vec2(0.0, 0.0), Vec2(-1.83, -1.83))

        @JvmField
        var SHOOT_LONG_TRIANGLE = Triangle(Vec2(1.83, 0.61), Vec2(1.22, 0.0), Vec2(1.83, -0.61))

        @JvmField
        var H_REGULATOR_PARAMETERS = RegulatorParameters(kP = 5.5, limitU = MAX_DRIVE_ANGLE_VELOCITY)

        @JvmField
        var X_REGULATOR_PARAMETERS = RegulatorParameters(kP = 3.0, limitU = MAX_DRIVE_VELOCITY)

        @JvmField
        var Y_REGULATOR_PARAMETERS = RegulatorParameters(kP = 3.0, limitU = MAX_DRIVE_VELOCITY)

        @JvmField
        var H_SENS = 0.1

        @JvmField
        var POS_SENS = 0.1

        @JvmField
        var TARGET_TIMER = 0.1

        @JvmField
        var RED_PARKING_ORIENTATION = Orientation(Vec2(0.9, -0.93), Angle.ofDeg(90.0))

        @JvmField
        var BLUE_PARKING_ORIENTATION = Orientation(Vec2(0.9, 0.93), Angle.ofDeg(-90.0))

        @JvmField
        var X_DEATH_ZONE = 0.05

        @JvmField
        var Y_DEATH_ZONE = 0.05

        @JvmField
        var H_DEATH_ZONE = 0.05
    }

    @Config
    internal object ROAD_RUNNER {
        @JvmField
        var ROAD_RUNNER_POS_X_P = 1.0

        @JvmField
        var ROAD_RUNNER_POS_Y_P = 1.0

        @JvmField
        var ROAD_RUNNER_POS_H_P = 5.0

        @JvmField
        var ROAD_RUNNER_VEL_X_P = 0.0

        @JvmField
        var ROAD_RUNNER_VEL_Y_P = 0.0

        @JvmField
        var ROAD_RUNNER_VEL_H_P = 0.0

        @JvmField
        var ROAD_RUNNER_TRANSLATE_VELOCITY = 1.8

        @JvmField
        var ROAD_RUNNER_ROTATE_VELOCITY = 7.0

        @JvmField
        var ROAD_RUNNER_ROTATE_ACCEL = 14.0

        @JvmField
        var ROAD_RUNNER_MIN_TRANSLATION_ACCEL = -2.0

        @JvmField
        var ROAD_RUNNER_MAX_TRANSLATION_ACCEL = 2.0
    }

    @Config
    internal object CAMERA {
        @JvmField
        var CAMERA_ENABLE = true
        @JvmField
        var CAMERA_H_RED_DOWN=4.0
        @JvmField
        var CAMERA_H_RED_UP=30.0
        @JvmField
        var CAMERA_C_RED_DOWN=127.7
        @JvmField
        var CAMERA_C_RED_UP=255.0
        @JvmField
        var CAMERA_V_RED_DOWN=154.5
        @JvmField
        var CAMERA_V_RED_UP=255.0
        @JvmField
        var CAMERA_KSIZE=22.0

        @JvmField
        var CRASH_TIME = 1.0

        @JvmField
        var CRASH_FPS_THRESHOLD = 0.00001
    }

    @Config
    internal object TURRET {
        @JvmField
        var PULLEY_RADIUS = 0.0425

        @JvmField
        var PULLEY_TICKS_IN_REVOLUTION = 28.0

        @JvmField
        var PULLEY_REGULATOR = RegulatorParameters(kP = 0.013, kF = 0.0035, kI = 0.0007)

        @JvmField
        var PULLEY_TARGET_SENS = 0.4

        @JvmField
        var PULLEY_TARGET_TIMER = 0.1

        @EventConfig
        var PULLEY_VELOCITY_FILTER_COEF = ThreadedTelemetry.EventValueProvider(0.3)

        @JvmField
        var BLUE_BASKET_POSITION = Vec2(-3.66 / 2.0, -3.66 / 2.0)

        @JvmField
        var RED_BASKET_POSITION = Vec2(-3.66 / 2.0, 3.66 / 2.0)

        @JvmField
        var TURRET_HEIGHT = 0.35

        @JvmField
        var BALL_MASS = 0.075

        @JvmField
        var BASKET_TARGET_HEIGHT = 1.08

        @JvmField
        var TIME_STEP = 0.05

        @JvmField
        var CALCULATING_G = 9.78

        @JvmField
        var APPROXIMATION_MAX_ITERATIONS = 100

        @JvmField
        var MIN_TURRET_ANGLE_SERVO = 0.3

        @JvmField
        var MAX_TURRET_ANGLE_SERVO = 0.85

        @JvmField
        var MIN_TURRET_ANGLE = 30.0 / 180.0 * PI

        @JvmField
        var MAX_TURRET_ANGLE = 45.0 / 180.0 * PI

        @JvmField
        var PULLEY_U = 0.302

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
        var ZERO_ROTATE_POS = 1.0 - 0.68

        @JvmField
        var ZEROING_TIME = 0.1

        @JvmField
        var ROTATE_SERVO_RATIO = (25.0 / 20.0) * (40.0 / 110.0)

        @JvmField
        var ROTATE_SERVO_TURNS = PI * 2.0 * 5.0

        @JvmField
        var ROTATE_ENCODER_RATIO = 40.0 / 110.0

        @JvmField
        var ENCODER_TICKS_IN_REVOLUTION = 8192.0

        @JvmField
        var MAX_ROTATE = ((1.0 - 0.54) - ZERO_ROTATE_POS) * ROTATE_SERVO_TURNS * ROTATE_SERVO_RATIO

        @JvmField
        var MIN_ROTATE = ((1.0 - 0.74) - ZERO_ROTATE_POS) * ROTATE_SERVO_TURNS * ROTATE_SERVO_RATIO

        @JvmField
        var ACCEL_K = 10.0

        @JvmField
        var ACCEL_THRESHOLD = 0.3
    }

    @Config
    internal object STORAGE_SENSORS {

        @JvmField
        var OPTIC_SEES_NOT_BLACK = 0.4


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


        @JvmField
        var PREDICT_SORT = 5

        @JvmField
        var STORAGE_CALIBRATION = 6

        @JvmField
        var UPDATE_AFTER_LAZY_INTAKE = 7
    }


    object DEBUG_LEVELS
    {
        var SHOW_DEBUG_SUPPRESS_WARNINGS = true

        val HARDWARE_LOW  = 0u
        val HARDWARE      = 1u
        val HARDWARE_HIGH = 2u

        val GAMEPAD_FEEDBACK = 3u
        val EVENTS_FEEDBACK  = 4u

        val ATTEMPTING_LOGIC = 5u
        val PROCESS_STARTING = 6u
        val PROCESS_ENDING   = 7u

        val GENERIC_INFO = 8u
        val LOGIC_STEPS  = 9u

        val PROCESS_NAME = 10u
        val TERMINATION  = 11u



        //  Sorting cells
        var CELLS_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var CELLS_DEBUG_LEVELS  = arrayListOf(
            HARDWARE,
            HARDWARE_HIGH,

            GAMEPAD_FEEDBACK,
            EVENTS_FEEDBACK,

            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION)


        //  Sorting Storage Logic
        var SSL_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SSL_DEBUG_LEVELS  = arrayListOf(
            HARDWARE,
            HARDWARE_HIGH,

            GAMEPAD_FEEDBACK,
            EVENTS_FEEDBACK,

            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION)


        //  Sorting storage module
        var SSM_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SSM_DEBUG_LEVELS  = arrayListOf(
            HARDWARE,
            HARDWARE_HIGH,

            GAMEPAD_FEEDBACK,
            EVENTS_FEEDBACK,

            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION)


        //  Sorting Auto Logic
        var SAL_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SAL_DEBUG_LEVELS  = arrayListOf(
            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION)


        //  Scoring modules connector
        var SMC_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SMC_DEBUG_LEVELS  = arrayListOf(
            GAMEPAD_FEEDBACK,
            EVENTS_FEEDBACK,

            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION)


        //  Hardware sorting manager
        var HSM_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var HSM_DEBUG_LEVELS  = arrayListOf(
            HARDWARE,
            HARDWARE_HIGH,

            GAMEPAD_FEEDBACK,
            EVENTS_FEEDBACK,

            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION)


        //  Color sensors and optics - storage sensors
        var SENSORS_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SENSORS_DEBUG_LEVELS  = arrayListOf(HARDWARE, HARDWARE_HIGH)
    }



    @Config
    internal object DELAY {

        @JvmField
        var INTAKE_RACE_CONDITION_MS: Long = 15
        @JvmField
        var LAZY_INTAKE_RACE_CONDITION_MS: Long = 15

        @JvmField
        var REQUEST_RACE_CONDITION_MS: Long = 10


        @JvmField
        var PREDICT_SORT_RACE_CONDITION_MS: Long = 10

        @JvmField
        var STORAGE_CALIBRATION_RACE_CONDITION_MS: Long = 10



        @JvmField
        var EVENT_AWAITING_MS: Long = 5

        @JvmField
        var HARDWARE_REQUEST_FREQUENCY_MS: Long = 5


        @JvmField
        var ONE_BALL_PUSHING_MS: Long = 444
        @JvmField
        var FIRE_THREE_BALLS_FOR_SHOOTING_MS: Long = 1000


        @JvmField
        var SORTING_REALIGNING_FORWARD_MS: Long = 35
        @JvmField
        var SORTING_REALIGNING_REVERSE_MS: Long = 65


        @JvmField
        var SMC_MAX_SHOT_AWAITING_MS: Long = 350
        @JvmField
        var SSM_MAX_SHOT_AWAITING_MS: Long = 700



        @JvmField
        var IGNORE_BELTS_CURRENT_AFTER_START_MS: Long = 200


        @JvmField
        var BETWEEN_SHOTS_MS:   Long = 300
        @JvmField
        var BETWEEN_INTAKES_MS: Long = 500
    }



    @Config
    internal object SORTING_SETTINGS {

        @JvmField
        var ALWAYS_TRY_PREDICT_SORTING = true

        @JvmField
        var MIN_SEQUENCE_SCORE_FOR_PREDICT_SORTING = 0.75

        @JvmField
        var START_WEIGHT_FOR_PREDICT_SORT = 0.0

        @JvmField
        var TRUE_MATCH_WEIGHT   = 1.0

        @JvmField
        var PSEUDO_MATCH_WEIGHT = 0.75



        @JvmField
        var USE_LAZY_VERSION_OF_STREAM_REQUEST = true
        @JvmField
        var USE_EASY_VERSION_OF_STREAM_REQUEST = true


        @JvmField
        var SMART_AUTO_ADJUST_PATTERN_FOR_FAILED_SHOTS = false



        @JvmField
        var USE_CURRENT_PROTECTION_FOR_STORAGE_BELTS = true

        @JvmField
        var SMART_RECALIBRATE_STORAGE_WITH_CURRENT_PROTECTION = false
        @JvmField
        var TRY_RECALIBRATE_WITH_CURRENT_UNTIL_SUCCESS = false


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



        @JvmField
        var USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION = false

        @JvmField
        var PREFERRED_INTAKE_SLOT_SEARCHING_ORDER = arrayOf(
            StorageSlot.BOTTOM,
            StorageSlot.CENTER,
            StorageSlot.TURRET,
            StorageSlot.MOBILE)

        @JvmField
        var PREFERRED_REQUEST_SLOT_SEARCHING_ORDER = arrayOf(
            StorageSlot.TURRET,
            StorageSlot.CENTER,
            StorageSlot.BOTTOM,
            StorageSlot.MOBILE)
    }



    @Config
    internal object STORAGE {

        @JvmField
        var STORAGE_IS_FULL_BELTS_CURRENT = 8.1


        @JvmField
        var BELT_MOTORS_DIRECTION = DcMotorSimple.Direction.REVERSE


        @JvmField
        var GATE_SERVO_OPEN_VALUE = 0.38
        @JvmField
        var GATE_SERVO_CLOSE_VALUE = 0.765

        @JvmField
        var PUSH_SERVO_OPEN_VALUE = 0.58
        @JvmField
        var PUSH_SERVO_CLOSE_VALUE = 0.84

        @JvmField
        var LAUNCH_SERVO_OPEN_VALUE = 0.37
        @JvmField
        var LAUNCH_SERVO_CLOSE_VALUE = 0.75



        @JvmField
        var POWER_FOR_FAST_BELT_ROTATING = 11.0

        @JvmField
        var POWER_FOR_SLOW_BELT_ROTATING = 9.0

        @JvmField
        var TURRET_GATE_SERVO_OPEN_VALUE = 0.775

        @JvmField
        var TURRET_GATE_SERVO_CLOSE_VALUE = 0.601
    }



    @Config
    internal object HARDWARE_DEVICES_NAMES {

        @JvmField
        var INTAKE_COLOR_SENSOR_1 = "color1"
        @JvmField
        var INTAKE_COLOR_SENSOR_2 = "color2"


        @JvmField
        var TURRET_OPTIC_1 = "optic1"
        @JvmField
        var TURRET_OPTIC_2 = "optic2"


        @JvmField
        var TURRET_GATE_SERVO = "turretGateServo"


        @JvmField
        var SORTING_STORAGE_BELT_MOTORS = "beltMotors"


        @JvmField
        var GATE_SERVO = "gateServo"
        @JvmField
        var PUSH_SERVO = "pushServo"
        @JvmField
        var LAUNCH_SERVO = "launchServo"
    }

    @Config
    internal object SIMPLE_STORAGE {
        @JvmField
        var LOOK_DELAY_TIME = 0.09

        @JvmField
        var REVERS_TIME = 0.2

        @JvmField
        var BELTS_FULL_CURRENT = 8.9

        @JvmField
        var BELTS_FULL_TIMER = 0.1

        @JvmField
        var BELTS_POWER = 5.0

        @JvmField
        var BELTS_FAST_POWER = 9.0

        @JvmField
        var BELTS_FAST_FAST_POWER = 10.0
    }

    @Config
    internal object BATTERY{
        @JvmField
        var LOW_VOLTAGE = 8.5
    }

    @Config
    internal object LIGHT{
        @JvmField
        var BLUE_R_POWER = 0.0

        @JvmField
        var BLUE_G_POWER = 1.0

        @JvmField
        var BLUE_B_POWER = 1.0

        @JvmField
        var GREEN_R_POWER = 0.0

        @JvmField
        var GREEN_G_POWER = 1.0

        @JvmField
        var GREEN_B_POWER = 0.0

        @JvmField
        var ORANGE_R_POWER = 1.0

        @JvmField
        var ORANGE_G_POWER = 0.5

        @JvmField
        var ORANGE_B_POWER = 0.0
    }
}