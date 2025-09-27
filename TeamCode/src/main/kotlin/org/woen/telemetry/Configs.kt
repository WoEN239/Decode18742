package org.woen.telemetry

import com.acmerobotics.dashboard.config.Config
import org.woen.utils.regulator.RegulatorParameters
import org.woen.utils.units.Vec2
import kotlin.math.PI

object Configs {
    @Config
    internal object BRUSH {
        @JvmField
        var BRUSH_DEF_TIME = 1.0

        @JvmField
        var BRUSH_MOTOR_NAME = "BrushMotor"

        @JvmField
        var BRUSH_TARGET_CURRENT = 0.3

        @JvmField
        var BRUSH_MOTORS_FORWARD = 1

        @JvmField
        var BRUSH_MOTORS_STOP = 0

        @JvmField
        var BRUSH_MOTORS_BACK = 2
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
        var DEFAULT_SERVO_V_MAX = 3.0

        @JvmField
        var DEFAULT_SERVO_A = 1.0

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
        var ODOMETER_LEFT_RADIUS = 1.0

        @JvmField
        var ODOMETER_RIGHT_RADIUS = 1.0

        @JvmField
        var ODOMETER_SIDE_RADIUS = 1.0

        @JvmField
        var ODOMETER_ROTATE_SENS = 1e-8

        @EventConfig
        var ODOMETRY_MERGE_COEF = ThreadedTelemetry.EventValueProvider(0.1)
    }

    @Config
    internal object DRIVE_TRAIN {
        @JvmField
        var DRIVE_SIDE_REGULATOR_PARAMS = RegulatorParameters()

        @JvmField
        var DRIVE_FORWARD_REGULATOR_PARAMS = RegulatorParameters()

        @JvmField
        var DRIVE_ROTATE_REGULATOR_PARAMS = RegulatorParameters()

        @JvmField
        var DRIVE_TRAIN_LOOK_P = 1.0

        @JvmField
        var DRIVE_VEC_MULTIPLIER = 1000.0

        @JvmField
        var DRIVE_ANGLE_MULTIPLIER = 10.0
    }

    @Config
    internal object ROAR_RUNNER {
        @JvmField
        var ROAD_RUNNER_POS_X_P = 0.0

        @JvmField
        var ROAD_RUNNER_POS_Y_P = 0.0

        @JvmField
        var ROAD_RUNNER_POS_H_P = 0.0

        @JvmField
        var ROAD_RUNNER_VEL_X_P = 0.0

        @JvmField
        var ROAD_RUNNER_VEL_Y_P = 0.0

        @JvmField
        var ROAD_RUNNER_VEL_H_P = 0.0

        @JvmField
        var ROAD_RUNNER_TRANSLATE_VELOCITY = 1.0

        @JvmField
        var ROAD_RUNNER_ROTATE_VELOCITY = 1.0

        @JvmField
        var ROAD_RUNNER_ROTATE_ACCEL = 1.0

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
        var CAMERA_ACCURACY = 150

        @JvmField
        var CAMERA_ENABLE = false
    }

    @Config
    internal object TURRET {
        @JvmField
        var PULLEY_RADIUS = 10.0 / 2.0

        @JvmField
        var PULLEY_TICKS_IN_REVOLUTION = 24.0

        @JvmField
        var PULLEY_REGULATOR = RegulatorParameters()

        @EventConfig
        var PULLEY_VELOCITY_FILTER_COEF = ThreadedTelemetry.EventValueProvider(0.1)

        @JvmField
        var PULLEY_TARGET_SENS = 50.0

        @JvmField
        var BLUE_BASKET_POSITION = Vec2(0.0, 0.0)

        @JvmField
        var RED_BASKET_POSITION = Vec2(0.0, 0.0)

        @JvmField
        var TURRET_ANGLE = PI / 2.0

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
    }

    @Config
    internal object BARREL {
        @JvmField
        var BARREL_REGULATOR_PARAMETERS = RegulatorParameters()
    }
}