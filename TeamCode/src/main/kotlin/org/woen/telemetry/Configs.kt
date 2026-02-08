package org.woen.telemetry


import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.woen.enumerators.Shooting
import org.woen.enumerators.StorageSlot
import org.woen.utils.process.RunStatus
import org.woen.utils.regulator.RegulatorParameters
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Triangle
import org.woen.utils.units.Vec2
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max


/*
 *    Это файл со всеми конфигами робота
 *    В нём, и только в нём РАЗРЕШАЕТСЯ использовать ЭДЖОВСКИЕ D: технологии
 *    (на значениях которые не требуют больших точностей)
 *    > подставление красивых значений вроде 33333
 */


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
        var BRUSH_TARGET_CURRENT = 3.0

        @JvmField
        var BRUSH_BIG_TARGET_CURRENT = 1000.0

        @JvmField
        var BRUSH_STOP_TIME = 0.5

        @JvmField
        var TIME_FOR_BRUSH_REVERSING: Long = 999

        @JvmField
        var BRUSH_POWER = 11.5
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
        var START_RED_CLOSE_ORIENTATION =
            Orientation(Vec2(-(1.205 + 0.38 / 2.0 + 0.01), 0.93 + 0.38 / 2.0 - 0.01), Angle(PI))

        @JvmField
        var START_RED_FAR_ORIENTATION = Orientation(Vec2(1.63, 0.39), Angle(PI))

        @JvmField
        var START_BLUE_CLOSE_ORIENTATION =
            Orientation(Vec2(-(1.205 + 0.38 / 2.0 + 0.01), -0.93 - 0.38 / 2.0 + 0.01), Angle(PI))

        @JvmField
        var START_BLUE_FAR_ORIENTATION = Orientation(Vec2(1.631, -0.39), Angle(PI))

        @JvmField
        var X_ODOMETER_POSITION = -0.0995

        @JvmField
        var Y_ODOMETER_POSITION = -0.0895
    }

    @Config
    internal object DRIVE_TRAIN {
        @JvmField
        var DRIVE_TO_SHOOTING_ZONE = true

        @JvmField
        var DRIVE_SIDE_REGULATOR_PARAMS = RegulatorParameters(kF = 11.0, kI = 0.0, kP = 9.0)

        @JvmField
        var DRIVE_FORWARD_REGULATOR_PARAMS = RegulatorParameters(kF = 8.0, kI = 0.0, kP = 5.0)

        @JvmField
        var DRIVE_ROTATE_REGULATOR_PARAMS = RegulatorParameters(kF = 2.0, kP = 1.3, kI = 0.0)

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
        var H_REGULATOR_PARAMETERS =
            RegulatorParameters(kP = 6.5, limitU = MAX_DRIVE_ANGLE_VELOCITY)

        @JvmField
        var X_REGULATOR_PARAMETERS =
            RegulatorParameters(kP = 4.0, kD = 0.1, limitU = MAX_DRIVE_VELOCITY)

        @JvmField
        var Y_REGULATOR_PARAMETERS =
            RegulatorParameters(kP = 4.0, kD = 0.1, limitU = MAX_DRIVE_VELOCITY)

        @JvmField
        var SHOOTING_P = 4.0

        @JvmField
        var H_SENS = 0.3

        @JvmField
        var POS_SENS = 0.1

        @JvmField
        var TARGET_TIMER = 0.1

        @JvmField
        var RED_PARKING_ORIENTATION = Orientation(Vec2(0.948, -0.803), Angle.ofDeg(-180.0))

        @JvmField
        var BLUE_PARKING_ORIENTATION = Orientation(Vec2(0.948, 0.803), Angle.ofDeg(180.0))

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
        var ROAD_RUNNER_POS_X_P = 1.9

        @JvmField
        var ROAD_RUNNER_POS_Y_P = 1.9

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
        var CAMERA_ENABLE = false

        @JvmField
        var CAMERA_H_RED_DOWN = 4.0

        @JvmField
        var CAMERA_H_RED_UP = 30.0

        @JvmField
        var CAMERA_C_RED_DOWN = 127.7

        @JvmField
        var CAMERA_C_RED_UP = 255.0

        @JvmField
        var CAMERA_V_RED_DOWN = 154.5

        @JvmField
        var CAMERA_V_RED_UP = 255.0

        @JvmField
        var CAMERA_KSIZE = 22.0
    }

    @Config
    internal object TURRET {
        @JvmField
        var PULLEY_RADIUS = 0.0425

        @JvmField
        var PULLEY_TICKS_IN_REVOLUTION = 28.0

        @JvmField
        var PULLEY_REGULATOR = RegulatorParameters(kF = 0.0055, kP = 0.08, kI = 0.01 /*, limitU = 12.0*/)

        @JvmField
        var REGULATOR_SENS = 0.5

        @JvmField
        var TARGET_TIMER = 0.1

        @JvmField
        var BLUE_BASKET_POSITION = Vec2(-3.66 / 2.0 + 0.2, -3.66 / 2.0 + 0.2)

        @JvmField
        var RED_BASKET_POSITION = Vec2(-3.66 / 2.0 + 0.2, 3.66 / 2.0 - 0.2)

        @JvmField
        var MIN_TURRET_ANGLE_SERVO = 1.0 - 0.62

        @JvmField
        var MAX_TURRET_ANGLE_SERVO = 1.0 - 0.83

        @JvmField
        var MIN_TURRET_ANGLE = PI / 2.0 - toRadians(45.0)

        @JvmField
        var MAX_TURRET_ANGLE = PI / 2.0 - toRadians(25.0)

        @JvmField
        var TURRET_CENTER_POS = Vec2(0.0, -0.38 / 2.0 + 0.145)

        @JvmField
        var ZERO_ROTATE_POS = toRadians(172.54545454545456)

        @JvmField
        var ROTATE_SERVO_RATIO = (30.0 / 18.0) * (60.0 / 120.0)

        @JvmField
        var ROTATE_SERVO_REGULATOR =
            RegulatorParameters(kP = 0.2, kD = 0.00005, kPow = 0.0001, limitU = 1.0)

        @JvmField
        var MAX_ROTATE = PI / 2.0

        @JvmField
        var MIN_ROTATE = -PI / 2.0

        @JvmField
        var SHOOTING_RED_ORIENTATION = Orientation(Vec2(-0.630, 0.387), Angle.ofDeg(129.0))

        @JvmField
        var SHOOTING_BLUE_ORIENTATION = Orientation(Vec2(-0.630, -0.387), Angle.ofDeg(-129.0))

        @JvmField
        var OBELISK_POSITION = Vec2(-3.66 / 2.0, 0.0)

        @JvmField
        var GRAVITY_G = 9.80665

        @JvmField
        var SCORE_HEIGHT = 1.0

        @JvmField
        var TURRET_HEIGHT = 0.2

        @JvmField
        var SCORE_ANGLE = toRadians(-30.0)

        @JvmField
        var PULLEY_RATION = 30.0 / 40.0

        @JvmField
        var PULLEY_U = 0.275

        @JvmField
        var PULLEY_VELOCITY = 11.8 //(-0.927, -0.842)

        @JvmField
        var ANGLE_POSITION = MAX_TURRET_ANGLE
    }

    @Config
    internal object STORAGE_SENSORS {
        @JvmField
        var MAXIMUM_READING = (65535.coerceAtMost(
            1024 * (256 - max(
                0,
                256 - ceil((24 / 2.4f).toDouble()).toInt()
            ))
        )).toDouble()

        @JvmField
        var GREEN_THRESHOLD_RIGHT = 30.0

        @JvmField
        var GREEN_THRESHOLD_LEFT = 50.0

        @JvmField
        var MIN_PURPLE_H_RIGHT = 2.9

        @JvmField
        var MAX_PURPLE_H_RIGHT = 4.0

        @JvmField
        var MIN_PURPLE_H_LEFT = 2.9

        @JvmField
        var MAX_PURPLE_H_LEFT = 4.0

        @JvmField
        var DOUBLE_DETECT_TIMER = 0.3

        @JvmField
        var DOUBLE_DETECT_COUNT_MAX = 2
    }


    @Config
    internal object PROCESS_ID {

        @JvmField
        var PRIORITY_SETTING_FOR_SORTING_STORAGE = RunStatus.Priority.PRIORITIZE_HIGH_PROCESS_ID

        @JvmField
        var PRIORITY_SETTING_FOR_SCORING_CONNECTOR = RunStatus.Priority.PRIORITIZE_HIGH_PROCESS_ID


        @JvmField
        var UNDEFINED_PROCESS_ID = 0

        @JvmField
        var IDLE = 0


        @JvmField
        var INTAKE = 1

        @JvmField
        var LAZY_INTAKE = 2

        @JvmField
        var RUNNING_INTAKE_INSTANCE = 3


        @JvmField
        var DRUM_REQUEST = 4

        @JvmField
        var SINGLE_REQUEST = 5


        @JvmField
        var PREDICT_SORT = 6

        @JvmField
        var STORAGE_CALIBRATION = 7

        @JvmField
        var UPDATE_AFTER_LAZY_INTAKE = 8

        @JvmField
        var SORTING_TESTING = 9
    }


    object DEBUG_LEVELS {
        var SHOW_DEBUG_SUPPRESS_WARNINGS = true

        val HARDWARE_LOW = 0u
        val HARDWARE = 1u
        val HARDWARE_HIGH = 2u

        val GAMEPAD_FEEDBACK = 3u
        val EVENTS_FEEDBACK = 4u

        val RACE_CONDITION = 5u
        val ATTEMPTING_LOGIC = 6u
        val PROCESS_STARTING = 7u
        val PROCESS_ENDING = 8u

        val GENERIC_INFO = 9u
        val LOGIC_STEPS = 10u

        val PROCESS_NAME = 11u
        val TERMINATION = 12u


        //  Sorting cells
        var CELLS_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var CELLS_DEBUG_LEVELS = arrayListOf(
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
            TERMINATION
        )


        //  Sorting Storage Logic
        var SSL_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SSL_DEBUG_LEVELS = arrayListOf(
            HARDWARE,
            HARDWARE_HIGH,

            GAMEPAD_FEEDBACK,
            EVENTS_FEEDBACK,
            RACE_CONDITION,

            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION
        )


        //  Sorting storage module
        var SSM_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SSM_DEBUG_LEVELS = arrayListOf(
            HARDWARE,
            HARDWARE_HIGH,

            GAMEPAD_FEEDBACK,
            EVENTS_FEEDBACK,
            RACE_CONDITION,

            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION
        )


        //  Sorting Auto Logic
        var SAL_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SAL_DEBUG_LEVELS = arrayListOf(
            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION
        )


        //  Scoring modules connector
        var SMC_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SMC_DEBUG_LEVELS = arrayListOf(
            GAMEPAD_FEEDBACK,
            EVENTS_FEEDBACK,
            RACE_CONDITION,

            ATTEMPTING_LOGIC,
            PROCESS_STARTING,
            PROCESS_ENDING,

            GENERIC_INFO,
            LOGIC_STEPS,

            PROCESS_NAME,
            TERMINATION
        )


        //  Hardware sorting manager
        var HSM_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var HSM_DEBUG_LEVELS = arrayListOf(
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
            TERMINATION
        )


        //  Color sensors and optics - storage sensors
        var SENSORS_DEBUG_SETTING = LogManager.DebugSetting.SHOW_SELECTED_LEVELS
        var SENSORS_DEBUG_LEVELS = arrayListOf(HARDWARE, HARDWARE_HIGH)
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
        var FULL_BALL_PUSHING_MS: Long = 444

        @JvmField
        var PART_BALL_PUSHING_MS: Long = 333

        @JvmField
        var FIRE_3_BALLS_FOR_SHOOTING_MS: Long = 370        //  9.0V configuration

        //        var FIRE_3_BALLS_FOR_SHOOTING_MS: Long = 500      //  8.5V configuration
        @JvmField
        var FIRE_2_BALLS_FOR_SHOOTING_MS: Long = 400

        //        var FIRE_2_BALLS_FOR_SHOOTING_MS: Long = 444      //  8.5V configuration
        @JvmField
        var FIRE_1_BALLS_FOR_SHOOTING_MS: Long = 200
//        var FIRE_1_BALLS_FOR_SHOOTING_MS: Long = 222      //  8.5V configuration


        @JvmField
        var SHOOTING_REALIGNING_FORWARD_MS: Long = 222

        @JvmField
        var SORTING_REALIGNING_FORWARD_MS: Long = 444

        @JvmField
        var SORTING_REALIGNING_REVERSE_MS: Long = 75


        @JvmField
        var SMC_MAX_SHOT_AWAITING_MS: Long = 60

        @JvmField
//        var SSL_MAX_SHOT_AWAITING_MS: Long = 205     //  9.0V configuration
        var SSL_MAX_SHOT_AWAITING_MS: Long = 245       //  8.5V configuration

        @JvmField
        var SSL_MAX_ODOMETRY_REALIGNMENT_AWAITING_MS: Long = 33333 / 3


        @JvmField
        var IGNORE_BELTS_CURRENT_AFTER_START_MS: Long = 200


        @JvmField
        var BETWEEN_SHOTS_MS: Long = 100

        @JvmField
        var BETWEEN_INTAKES_MS: Long = 500
    }


    @Config
    internal object SORTING_SETTINGS {

        @JvmField
        var INITIAL_LOAD_FROM_TURRET_TO_BOTTOM = Shooting.StockPattern.Sequence.Storage.EMPTY


        @JvmField
        var ALWAYS_TRY_PREDICT_SORTING = true

        @JvmField
        var TRY_ADDITIONAl_PREDICT_SORTING_WHILE_SHOOTING = true

        @JvmField
        var MIN_SEQUENCE_SCORE_FOR_PREDICT_SORTING = 0.75

        @JvmField
        var START_WEIGHT_FOR_PREDICT_SORT = 0.0

        @JvmField
        var TRUE_MATCH_WEIGHT = 1.0

        @JvmField
        var PSEUDO_MATCH_WEIGHT = 0.75


        @JvmField
        var USE_LAZY_VERSION_OF_STREAM_REQUEST = true

        @JvmField
        var DO_WAIT_BEFORE_NEXT_SHOT = false


        @JvmField
        var INCLUDE_PREVIOUS_UNFINISHED_TO_REQUEST_ORDER  = false
        @JvmField
        var INCLUDE_PREVIOUS_UNFINISHED_TO_FAILSAFE_ORDER = false
        @JvmField
        var AUTO_UPDATE_UNFINISHED_FOR_NEXT_PATTERN = false
        @JvmField
        var IF_AUTO_UPDATE_UNFINISHED_USE_FAILSAFE_ORDER = false


        @JvmField
        var USE_CURRENT_PROTECTION_FOR_STORAGE_BELTS = false

        @JvmField
        var SMART_RECALIBRATE_STORAGE_WITH_CURRENT_PROTECTION = false

        @JvmField
        var TRY_RECALIBRATE_WITH_CURRENT_UNTIL_SUCCESS = false


        @JvmField
        var MAX_WAIT_DURATION_FOR_PATTERN_DETECTION_MS: Long = 1000

        @JvmField
        var MAX_ATTEMPTS_FOR_PATTERN_DETECTION = 0

        @JvmField
        var TRY_RECALIBRATE_IF_SOMETHING_FAILS = true


        @JvmField
        var TELEOP_PATTERN_SHOOTING_MODE = Shooting.Mode.FIRE_PATTERN_CAN_SKIP

        @JvmField
        var AUTO_DEFAULT_SHOOTING_MODE = Shooting.Mode.FIRE_PATTERN_CAN_SKIP

        @JvmField
        var AUTO_DEFAULT_PATTERN = Shooting.StockPattern.Name.USE_DETECTED_PATTERN

        @JvmField
        var AUTO_FAILSAFE_SHOOTING_MODE = Shooting.Mode.FIRE_EVERYTHING_YOU_HAVE

        @JvmField
        var AUTO_FAILSAFE_PATTERN = Shooting.StockPattern.Name.ANY


        @JvmField
        var USE_SECOND_DRIVER_FOR_PATTERN_CALIBRATION = false

        @JvmField
        var PREFERRED_INTAKE_SLOT_SEARCHING_ORDER = arrayOf(
            StorageSlot.TURRET,
            StorageSlot.CENTER,
            StorageSlot.BOTTOM
        )

        @JvmField
        var PREFERRED_REQUEST_SLOT_SEARCHING_ORDER = arrayOf(
            StorageSlot.TURRET,
            StorageSlot.CENTER,
            StorageSlot.BOTTOM,
            StorageSlot.MOBILE
        )
    }


    @Config
    internal object STORAGE {

        @JvmField
        var STORAGE_IS_FULL_BELTS_CURRENT = 8.1


        @JvmField
        var BELT_MOTORS_DIRECTION = DcMotorSimple.Direction.REVERSE


        @JvmField
        var GATE_SERVO_OPEN_VALUE = 0.78

        @JvmField
        var GATE_SERVO_CLOSE_VALUE = 0.355

        @JvmField
        var PUSH_SERVO_OPEN_VALUE = 0.31

        @JvmField
        var PUSH_SERVO_CLOSE_VALUE = 0.545

        @JvmField
        var LAUNCH_SERVO_OPEN_VALUE = 0.58

        @JvmField
        var LAUNCH_SERVO_CLOSE_VALUE = 0.96


        @JvmField
        var BELT_POWER_LAZY_MODE = 10.0

        @JvmField
        var BELT_POWER_FAST_MODE = 11.0

        @JvmField
        var BELT_POWER_SLOW_MODE = 10.0

        @JvmField
        var BELT_POWER_SHOOT_MODE = 11.0

        @JvmField
        var TURRET_GATE_SERVO_OPEN_VALUE = 0.7

        @JvmField
        var TURRET_GATE_SERVO_CLOSE_VALUE = 0.36
    }


    @Config
    internal object HARDWARE_DEVICES_NAMES {

        @JvmField
        var INTAKE_COLOR_SENSOR_L = "leftColorSensor"

        @JvmField
        var INTAKE_COLOR_SENSOR_R = "rightColorSensor"


        @JvmField
        var TURRET_OPTIC_1 = "optic1"

        @JvmField
        var TURRET_OPTIC_2 = "optic2"


        @JvmField
        var TURRET_GATE_SERVO = "turretGateServo"


        @JvmField
        var STORAGE_BELT_MOTOR = "beltMotor"


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
        var REVERS_TIME = 0.2

        @JvmField
        var BELTS_FULL_CURRENT = 8.9

        @JvmField
        var BELTS_FULL_TIMER = 0.1

        @JvmField
        var BELTS_POWER = 11.0

        @JvmField
        var BELTS_SHOOT_POWER = 11.0

        @JvmField
        var SHOOTING_TIME = 0.5

        @JvmField
        var PUSH_TIME = 0.5

        @JvmField
        var COLOR_THRESHOLD = 60.0
    }

    @Config
    internal object BATTERY {
        @JvmField
        var LOW_VOLTAGE = 9.5

        @JvmField
        var LOW_VOLTAGE_TRIGGER_TIME = 0.2
    }

    @Config
    internal object LIGHT {

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
        var ORANGE_G_POWER = 0.07

        @JvmField
        var ORANGE_B_POWER = 0.0


        @JvmField
        var RED_R_POWER = 1.0

        @JvmField
        var RED_G_POWER = 0.0

        @JvmField
        var RED_B_POWER = 0.0


        @JvmField
        var RED_FLASHING_SPEED = 4.0
    }
}