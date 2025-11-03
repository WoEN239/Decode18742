package org.woen.telemetry


import kotlin.math.PI

import org.woen.utils.units.Vec2
import org.woen.utils.units.Triangle
import org.woen.utils.units.Orientation
import org.woen.utils.regulator.RegulatorParameters

import woen239.enumerators.StorageSlot
import woen239.enumerators.StorageType

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.DcMotorSimple



object Configs {

    @Config
    internal object BRUSH {
        var BRUSH_SAFE_TIME = 1.0
        var BRUSH_ERR_TIME = 1.0

        @JvmField
        var BRUSH_DEF_TIME = 1.0

        @JvmField
        var BRUSH_TARGET_CURRENT = 0.3


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
        var ODOMETRY_MERGE_COEF = ThreadedTelemetry.EventValueProvider(0.1)

        @JvmField
        var START_RED_ORIENTATION = Orientation.ZERO

        @JvmField
        var START_BLUE_ORIENTATION = Orientation.ZERO
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
        var LOOK_P = 0.3

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
    }

    @Config
    internal object CAMERA {
        @JvmField
        var CAMERA_ACCURACY = 300

        @JvmField
        var CAMERA_ENABLE = false

        @JvmField
        var CAMERA_TRIGGER_DISTANCE = 3.0

        @JvmField
        var CAMERA_POSITION = Vec2(0.17, 0.1)
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
        var PULLEY_TARGET_SENS = 50.0

        @JvmField
        var BLUE_BASKET_POSITION = Vec2(-1.8, -1.8)

        @JvmField
        var RED_BASKET_POSITION = Vec2(-1.8, 1.8)

        @JvmField
        var TURRET_HEIGHT = 1.0

        @JvmField
        var BALL_MASS = 0.075

        @JvmField
        var BASKET_TARGET_HEIGHT = 1.0

        @JvmField
        var WAIT_PULLEY_SPEED = 5.0

        @JvmField
        var TIME_STEP = 0.1

        @JvmField
        var AIR_FORCE_K = 0.0

        @JvmField
        var CALCULATING_G = 9.78

        @JvmField
        var MAX_MOTOR_RPS = 100.0

        @JvmField
        var MIN_APPROXIMATION = 1.0

        @JvmField
        var APPROXIMATION_MAX_ITERATIONS = 100

        @JvmField
        var MIN_TURRET_ANGLE = 0.0

        @JvmField
        var MAX_TURRET_ANGLE = PI / 2.0

        @JvmField
        var MIN_TURRET_SERVO_ANGLE = 0.0

        @JvmField
        var MAX_TURRET_SERVO_ANGLE = 1.0

        @JvmField
        var MAX_SHOOTING_DISTANCE = 5.0

        @JvmField
        var ROTATION_TICKS_IN_REVOLUTION = 8192.0 * (23.0 / 6.0)

        @JvmField
        var ROTATION_PID_CONFIG = RegulatorParameters()

        @JvmField
        var PULLEY_U = 0.5

        @JvmField
        var MAX_POSSIBLE_DELAY_FOR_BALL_SHOOTING_MS: Long = 300
    }


    @Config
    internal object COLOR_SENSORS_AND_OPTIC_PARE {
        @JvmField
        var OPTIC_PARE_SEES_NOT_BLACK = 3.4


        @JvmField
        var THRESHOLD_GREEN_MAX_C_RED = 75

        @JvmField
        var THRESHOLD_GREEN_MIN_C_GREEN = 150

        @JvmField
        var THRESHOLD_GREEN_MAX_C_BLUE = 75


        @JvmField
        var THRESHOLD_PURPLE_MIN_C_RED = 64

        @JvmField
        var THRESHOLD_PURPLE_MIN_C_BLUE = 64

        @JvmField
        var THRESHOLD_PURPLE_MIN_C_GREEN_DIFF = 50
    }



    @Config
    internal object STORAGE {
        @JvmField
        var STORAGE_SLOT_COUNT = 4

        @JvmField
        var MAX_BALL_COUNT = 3


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
        var DELAY_FOR_ONE_BALL_PUSHING_MS: Long = 500
        @JvmField
        var DELAY_FOR_MAX_SERVO_POSITION_CHANGE: Long = 1000


        @JvmField
        var MAX_WAITING_TIME_FOR_INTAKE_MS = 2000
        @JvmField
        var DELAY_FOR_EATING_INTAKE_IN_STREAM_STORAGE_MS: Long = 1500



        @JvmField
        var STREAM_STORAGE_MOTOR_DIRECTION = DcMotorSimple.Direction.REVERSE


        @JvmField
        var SORTING_STORAGE_BELT_MOTOR_1_DIRECTION = DcMotorSimple.Direction.REVERSE
        @JvmField
        var SORTING_STORAGE_BELT_MOTOR_2_DIRECTION = DcMotorSimple.Direction.REVERSE


        @JvmField
        var SORTING_GATE_SERVO_OPEN_VALUE  = 0.0
        @JvmField
        var SORTING_GATE_SERVO_CLOSE_VALUE = 0.5

        @JvmField
        var SORTING_PUSH_SERVO_OPEN_VALUE  = 0.0
        @JvmField
        var SORTING_PUSH_SERVO_CLOSE_VALUE = 0.5

        @JvmField
        var SORTING_FALL_SERVO_OPEN_VALUE  = 0.0
        @JvmField
        var SORTING_FALL_SERVO_CLOSE_VALUE = 0.5



        @JvmField
        var HW_SORTING_SERVO_GATE_OPEN_VALUE  = 0.9
        @JvmField
        var HW_SORTING_SERVO_GATE_CLOSE_VALUE = 0.6



        @JvmField
        var USED_STORAGE_TYPE = StorageType.STREAM_STORAGE
    }


    
    @Config
    internal object HARDWARE_DEVICES_NAMES {
        @JvmField
        var INTAKE_COLOR_SENSOR_1 = "intake color sensor 1"
        @JvmField
        var INTAKE_COLOR_SENSOR_2 = "intake color sensor 2"


        @JvmField
        var BOTTOM_OPTIC_PARE_1 = "bottom optic pare sensor 1"
        @JvmField
        var BOTTOM_OPTIC_PARE_2 = "bottom optic pare sensor 2"


        @JvmField
        var MOBILE_OUT_OPTIC_PARE_1 = "mobile out optic pare sensor 1"
        @JvmField
        var MOBILE_OUT_OPTIC_PARE_2 = "mobile out optic pare sensor 2"


        @JvmField
        var TURRET_GATE_SERVO = "turret gate servo"


        @JvmField
        var STREAM_STORAGE_BELT_MOTOR = "stream storage belt motor"



        @JvmField
        var SORTING_STORAGE_BELT_MOTOR_1 = "sorting storage belt motor 1"
        @JvmField
        var SORTING_STORAGE_BELT_MOTOR_2 = "sorting storage belt motor 2"


        @JvmField
        var MOBILE_GATE_SERVO = "mobile gate servo"
        @JvmField
        var MOBILE_PUSH_SERVO = "mobile push servo"
        @JvmField
        var MOBILE_FALL_SERVO = "mobile fall servo"
    }
}