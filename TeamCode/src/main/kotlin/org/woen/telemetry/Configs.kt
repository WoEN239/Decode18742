package org.woen.telemetry


import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.woen.utils.regulator.RegulatorParameters
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Triangle
import org.woen.utils.units.Vec2
import woen239.enumerators.StorageSlot
import kotlin.math.PI


object Configs {

    @Config
    internal object BRUSH {
        @JvmField
        var BRUSH_SAFE_TIME = 0.1
        @JvmField
        var BRUSH_ERR_TIME = 0.4

        @JvmField
        var BRUSH_DEF_TIME = 1.0

        @JvmField
        var BRUSH_TARGET_CURRENT = 2.2


        @JvmField
        var TIME_FOR_BRUSH_REVERSING: Long = 1500
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
        var DEFAULT_SERVO_V_MAX = PI * 1.5 * 12.0

        @JvmField
        var DEFAULT_SERVO_A = PI * 1.5 * 12.0

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
        @EventConfig
        var VELOCITY_FILTER_K = ThreadedTelemetry.EventValueProvider(0.1)

        @JvmField
        var ODOMETRY_TICKS = 8192

        @JvmField
        var ODOMETRY_DIAMETER = 0.048

        @JvmField
        var ODOMETER_LEFT_RADIUS = 0.04

        @JvmField
        var ODOMETER_RIGHT_RADIUS = 0.04

        @JvmField
        var ODOMETER_SIDE_RADIUS = 0.0

        @JvmField
        var ODOMETER_ROTATE_SENS = 1e-8

        @EventConfig
        var ODOMETRY_MERGE_COEF = ThreadedTelemetry.EventValueProvider(0.2)

        @JvmField
        var START_RED_ORIENTATION = Orientation(Vec2(1.631, 0.39), Angle(PI * 2.0))

        @JvmField
        var START_BLUE_ORIENTATION = Orientation(Vec2(1.631, -0.39), Angle(PI * 2.0))

        @EventConfig
        var POSITION_VELOCITY_K = ThreadedTelemetry.EventValueProvider(0.1)

        @EventConfig
        var HEADING_VELOCITY_K = ThreadedTelemetry.EventValueProvider(0.1)
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
        var ROBOT_SIZE = Vec2(0.38, 0.36)

        @JvmField
        var SHOOT_TRIANGLES = arrayOf(
            Triangle(Vec2(-1.83, 1.83), Vec2(0.0, 0.0), Vec2(-1.83, -1.83)),
            Triangle(Vec2(1.83, 0.61), Vec2(1.22, 0.0), Vec2(1.83, -0.61))
        )

        @EventConfig
        var ENCODER_VELOCITY_FILTER_K = ThreadedTelemetry.EventValueProvider(0.1)

        @JvmField
        var ENCODER_TICKS = (1.0 + (46.0 / 17.0)) * (1.0 + (46.0 / 17.0)) * 28.0

        @JvmField
        var WHEEL_DIAMETER = 0.096

        @JvmField
        var LAG = Vec2(1.0, 1.0)

        @JvmField
        var H_LAG = 1.0

        @JvmField
        var LOOK_REGULATOR_PARAMETERS = RegulatorParameters()

        @JvmField
        var LOOK_SENS = 0.1

        @JvmField
        var WHEEL_CENTER_POS = Vec2(0.267 / 2.0, 0.28 / 2.0)
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
    internal object GYRO {
        @JvmField
        var GYRO_UPDATE_HZ = 5

        @EventConfig
        var GYRO_MERGE_COEF = ThreadedTelemetry.EventValueProvider(0.1)

        @JvmField
        var IS_USE_GYRO = true
    }

    @Config
    internal object CAMERA {
        @JvmField
        var CAMERA_ACCURACY = 300

        @JvmField
        var CAMERA_ENABLE = true

        @JvmField
        var CAMERA_TRIGGER_DISTANCE = 3.0

        @JvmField
        var CAMERA_POSITION = Vec2(0.17, 0.1)

        @JvmField
        var CAMERA_HEIGHT = 0.37
    }

    @Config
    internal object TURRET {
        @JvmField
        var PULLEY_RADIUS = 0.05

        @JvmField
        var PULLEY_TICKS_IN_REVOLUTION = 28.0

        @JvmField
        var PULLEY_REGULATOR = RegulatorParameters(kD = 0.0001, kI = 0.004, kP = 0.04)

        @EventConfig
        var PULLEY_VELOCITY_FILTER_COEF = ThreadedTelemetry.EventValueProvider(0.1)

        @JvmField
        var PULLEY_TARGET_SENS = 30.0

        @JvmField
        var BLUE_BASKET_POSITION = Vec2(-1.55, -1.55)

        @JvmField
        var RED_BASKET_POSITION = Vec2(-1.55, 1.55)

        @JvmField
        var TURRET_HEIGHT = 0.35

        @JvmField
        var BALL_MASS = 0.075

        @JvmField
        var BASKET_TARGET_HEIGHT = 1.1

        @JvmField
        var WAIT_PULLEY_SPEED = 5.0

        @JvmField
        var TIME_STEP = 0.1

        @JvmField
        var AIR_FORCE_K = 0.07

        @JvmField
        var CALCULATING_G = 9.78

        @JvmField
        var MAX_MOTOR_RPS = 100.0

        @JvmField
        var MIN_APPROXIMATION = 1.0

        @JvmField
        var APPROXIMATION_MAX_ITERATIONS = 100

        @JvmField
        var MIN_TURRET_ANGLE_SERVO = 0.0

        @JvmField
        var MAX_TURRET_ANGLE_SERVO = 0.9

        @JvmField
        var MIN_TURRET_ANGLE = 45.0 / 180 * PI

        @JvmField
        var MAX_TURRET_ANGLE = 55.0 / 180 * PI

        @JvmField
        var MAX_SHOOTING_DISTANCE = 5.0

        @JvmField
        var PULLEY_U = 0.7

        @JvmField
        var MAX_POSSIBLE_DELAY_FOR_BALL_SHOOTING_MS: Long = 1000

        @JvmField
        var TURRET_SHOOT_POS = Vec2.ZERO

        @JvmField
        var TURRET_CENTER_POS = Vec2.ZERO
    }


    @Config
    internal object COLOR_SENSORS_AND_OPTIC_PARE {
        @JvmField
        var OPTIC_PARE_SEES_NOT_BLACK = 3.4



        @JvmField
        var THRESHOLD_GREEN_BALL_MAX_R_S1 = 70
        @JvmField
        var THRESHOLD_GREEN_BALL_MIN_G_S1 = 40
        @JvmField
        var THRESHOLD_GREEN_BALL_MAX_B_S1 = 70

        @JvmField
        var THRESHOLD_GREEN_BALL_MAX_R_S2 = 70
        @JvmField
        var THRESHOLD_GREEN_BALL_MIN_G_S2 = 40
        @JvmField
        var THRESHOLD_GREEN_BALL_MAX_B_S2 = 70



        @JvmField
        var THRESHOLD_PURPLE_BALL_MIN_R_S1 = 40
        @JvmField
        var THRESHOLD_PURPLE_BALL_MAX_G_S1 = 70
        @JvmField
        var THRESHOLD_PURPLE_BALL_MIN_B_S1 = 20

        @JvmField
        var THRESHOLD_PURPLE_BALL_MIN_R_S2 = 40
        @JvmField
        var THRESHOLD_PURPLE_BALL_MAX_G_S2 = 70
        @JvmField
        var THRESHOLD_PURPLE_BALL_MIN_B_S2 = 20
    }



    @Config
    internal object STORAGE {
        @JvmField
        var MAX_BALL_COUNT = 3
        @JvmField
        var STORAGE_SLOT_COUNT = 4



        @JvmField
        var PREFERRED_INTAKE_SLOT_ORDER: Array<Int> = arrayOf(
            StorageSlot.BOTTOM,
            StorageSlot.CENTER,
            StorageSlot.MOBILE_OUT,
            StorageSlot.MOBILE_IN
        )

        @JvmField
        var PREFERRED_REQUEST_SLOT_ORDER: Array<Int> = arrayOf(
            StorageSlot.MOBILE_OUT,
            StorageSlot.CENTER,
            StorageSlot.BOTTOM,
            StorageSlot.MOBILE_IN
        )


        @JvmField
        var INTAKE_RACE_CONDITION_DELAY_MS: Long = 10
        @JvmField
        var REQUEST_RACE_CONDITION_DELAY_MS: Long = 5

        @JvmField
        var DELAY_FOR_EVENT_AWAITING_MS: Long = 5
        @JvmField
        var DELAY_FOR_HARDWARE_REQUEST_FREQUENCY: Long = 250

        @JvmField
        var DELAY_FOR_ONE_BALL_PUSHING_MS: Long = 333
        @JvmField
        var DELAY_BETWEEN_SHOTS: Long = 100
        @JvmField
        var DELAY_FOR_MAX_SERVO_POSITION_CHANGE: Long = 1333


        @JvmField
        var MAX_WAITING_TIME_FOR_INTAKE_MS = 400
        @JvmField
        var DELAY_FOR_BALL_TO_PUSHER_ALIGNMENT_MS: Long = 60



        @JvmField
        var SORTING_STORAGE_BELT_MOTOR_1_DIRECTION = DcMotorSimple.Direction.REVERSE
        @JvmField
        var SORTING_STORAGE_BELT_MOTOR_2_DIRECTION = DcMotorSimple.Direction.FORWARD


        @JvmField
        var MOBILE_GATE_SERVO_OPEN_VALUE  = 0.584
        @JvmField
        var MOBILE_GATE_SERVO_CLOSE_VALUE = 0.37

        @JvmField
        var MOBILE_PUSH_SERVO_OPEN_VALUE  = 0.7
        @JvmField
        var MOBILE_PUSH_SERVO_CLOSE_VALUE = 0.47

        @JvmField
        var MOBILE_FALL_SERVO_OPEN_VALUE  = 0.14
        @JvmField
        var MOBILE_FALL_SERVO_CLOSE_VALUE = 0.005

        @JvmField
        var MOBILE_LAUNCH_SERVO_OPEN_VALUE  = 0.225
        @JvmField
        var MOBILE_LAUNCH_SERVO_CLOSE_VALUE = 0.65



        @JvmField
        var TURRET_GATE_SERVO_OPEN_VALUE  = 0.9
        @JvmField
        var TURRET_GATE_SERVO_CLOSE_VALUE = 0.6
    }


    
    @Config
    internal object HARDWARE_DEVICES_NAMES {
        @JvmField
        var INTAKE_COLOR_SENSOR_1 = "color1"
        @JvmField
        var INTAKE_COLOR_SENSOR_2 = "color2"


        @JvmField
        var BOTTOM_OPTIC_PARE_1 = "bottom_optic_pare_1"
        @JvmField
        var BOTTOM_OPTIC_PARE_2 = "bottom_optic_pare_2"


        @JvmField
        var MOBILE_OUT_OPTIC_PARE_1 = "mobile_out_optic_pare_1"
        @JvmField
        var MOBILE_OUT_OPTIC_PARE_2 = "mobile_out_optic_pare_2"


        @JvmField
        var TURRET_GATE_SERVO = "turret_gate_servo"



        @JvmField
        var SORTING_STORAGE_BELT_MOTOR_1 = "sorting_storage_belt_motor_1"
        @JvmField
        var SORTING_STORAGE_BELT_MOTOR_2 = "sorting_storage_belt_motor_2"


        @JvmField
        var MOBILE_GATE_SERVO = "mobile_gate_servo"
        @JvmField
        var MOBILE_PUSH_SERVO = "mobile_push_servo"
        @JvmField
        var MOBILE_FALL_SERVO = "mobile_fall_servo"
        @JvmField
        var MOBILE_LAUNCH_SERVO = "mobile_launch_servo"
    }

    @Config
    internal object SIMPLE_STORAGE {
        @JvmField
        var BELT_PUSH_TIME = 0.2
    }
}