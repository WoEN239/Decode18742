package org.woen.telemetry

import org.woen.utils.regulator.RegulatorParameters
import org.woen.utils.units.Vec2
import kotlin.math.PI

object ThreadedConfigs {
    @ThreadedConfig(category = "BRUSH")
    var BRUSH_DEF_TIME = ThreadedTelemetry.AtomicEventProvider(1.0)

    @ThreadedConfig(category = "BRUSH")
    var BRUSH_MOTOR_NAME = ThreadedTelemetry.AtomicEventProvider("BrushMotor")

    @ThreadedConfig(category = "BRUSH")
    var BRUSH_TARGET_CURRENT = ThreadedTelemetry.AtomicEventProvider(0.3)

    @ThreadedConfig(category = "BRUSH")
    var BRUSH_MOTORS_FORWARD = ThreadedTelemetry.AtomicEventProvider(1)

    @ThreadedConfig(category = "BRUSH")
    var BRUSH_MOTORS_STOP = ThreadedTelemetry.AtomicEventProvider(0)

    @ThreadedConfig(category = "BRUSH")
    var BRUSH_MOTORS_BACK = ThreadedTelemetry.AtomicEventProvider(2)

    @ThreadedConfig(category = "TELEMETRY")
    var TELEMETRY_UPDATE_HZ = ThreadedTelemetry.AtomicEventProvider(5)

    @ThreadedConfig(category = "TELEMETRY")
    var TELEMETRY_ENABLE = ThreadedTelemetry.AtomicValueProvider(true)

    @ThreadedConfig(category = "SERVO_ANGLE")
    var DEFAULT_SERVO_ANGLE = ThreadedTelemetry.AtomicValueProvider(PI * 1.5)

    @ThreadedConfig(category = "SERVO_ANGLE")
    var DEFAULT_SERVO_V_MAX = ThreadedTelemetry.AtomicValueProvider(3.0)

    @ThreadedConfig(category = "SERVO_ANGLE")
    var DEFAULT_SERVO_A = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "SERVO_ANGLE")
    var DEFAULT_SERVO_OFFSET = ThreadedTelemetry.AtomicValueProvider(0.1)

    @ThreadedConfig(category = "THREAD_POOL")
    var THREAD_POOL_THREADS_COUNT = ThreadedTelemetry.AtomicValueProvider(5)

    @ThreadedConfig(category = "ODOMETRY")
    var VELOCITY_FILTER_K = ThreadedTelemetry.AtomicEventProvider(0.1)

    @ThreadedConfig(category = "ODOMETRY")
    var ODOMETRY_TICKS = ThreadedTelemetry.AtomicValueProvider(8192)

    @ThreadedConfig(category = "ODOMETRY")
    var ODOMETRY_DIAMETER = ThreadedTelemetry.AtomicValueProvider(4.8)

    @ThreadedConfig(category = "ODOMETRY")
    var ODOMETER_LEFT_RADIUS = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "ODOMETRY")
    var ODOMETER_RIGHT_RADIUS = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "ODOMETRY")
    var ODOMETER_SIDE_RADIUS = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "ODOMETRY")
    var ODOMETER_ROTATE_SENS = ThreadedTelemetry.AtomicValueProvider(1e-8)


    @ThreadedConfig(category = "ODOMETRY")
    var ODOMETRY_MERGE_COEF = ThreadedTelemetry.AtomicEventProvider(0.1)

    @ThreadedConfig(category = "DRIVE_TRAIN")
    var DRIVE_SIDE_REGULATOR_PARAMS = ThreadedTelemetry.AtomicValueProvider(RegulatorParameters())

    @ThreadedConfig(category = "DRIVE_TRAIN")
    var DRIVE_FORWARD_REGULATOR_PARAMS = ThreadedTelemetry.AtomicValueProvider(RegulatorParameters())

    @ThreadedConfig(category = "DRIVE_TRAIN")
    var DRIVE_ROTATE_REGULATOR_PARAMS = ThreadedTelemetry.AtomicValueProvider(RegulatorParameters())

    @ThreadedConfig(category = "DRIVE_TRAIN")
    var DRIVE_TRAIN_LOOK_P = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_POS_X_P = ThreadedTelemetry.AtomicValueProvider(0.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_POS_Y_P = ThreadedTelemetry.AtomicValueProvider(0.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_POS_H_P = ThreadedTelemetry.AtomicValueProvider(0.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_VEL_X_P = ThreadedTelemetry.AtomicValueProvider(0.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_VEL_Y_P = ThreadedTelemetry.AtomicValueProvider(0.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_VEL_H_P = ThreadedTelemetry.AtomicValueProvider(0.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_TRANSLATE_VELOCITY = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_ROTATE_VELOCITY = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_ROTATE_ACCEL = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_MIN_TRANSLATION_ACCEL = ThreadedTelemetry.AtomicValueProvider(-1.0)

    @ThreadedConfig(category = "ROAR_RUNNER")
    var ROAD_RUNNER_MAX_TRANSLATION_ACCEL = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "GYRO")
    var GYRO_UPDATE_HZ = ThreadedTelemetry.AtomicValueProvider(5)

    @ThreadedConfig(category = "GYRO")
    var GYRO_MERGE_COEF = ThreadedTelemetry.AtomicEventProvider(0.1)

    @ThreadedConfig(category = "CAMERA")
    var CAMERA_ACCURACY = ThreadedTelemetry.AtomicValueProvider(150)

    @ThreadedConfig(category = "CAMERA")
    var CAMERA_ENABLE = ThreadedTelemetry.AtomicValueProvider(false)

    @ThreadedConfig(category = "TURRET")
    var PULLEY_RADIUS = ThreadedTelemetry.AtomicValueProvider(10.0 / 2.0)

    @ThreadedConfig(category = "TURRET")
    var PULLEY_TICKS_IN_REVOLUTION = ThreadedTelemetry.AtomicValueProvider(24.0)

    @ThreadedConfig(category = "TURRET")
    var PULLEY_REGULATOR = ThreadedTelemetry.AtomicValueProvider(RegulatorParameters())

    @ThreadedConfig(category = "TURRET")
    var PULLEY_VELOCITY_FILTER_COEF = ThreadedTelemetry.AtomicEventProvider(0.1)

    @ThreadedConfig(category = "TURRET")
    var PULLEY_TARGET_SENS = ThreadedTelemetry.AtomicValueProvider(50.0)

    @ThreadedConfig(category = "TURRET")
    var BLUE_BASKET_POSITION = ThreadedTelemetry.AtomicValueProvider(Vec2(0.0, 0.0))

    @ThreadedConfig(category = "TURRET")
    var RED_BASKET_POSITION = ThreadedTelemetry.AtomicValueProvider(Vec2(0.0, 0.0))

    @ThreadedConfig(category = "TURRET")
    var TURRET_ANGLE = ThreadedTelemetry.AtomicValueProvider(45)

    @ThreadedConfig(category = "TURRET")
    var TURRET_HEIGHT = ThreadedTelemetry.AtomicValueProvider(1.0)

    @ThreadedConfig(category = "TURRET")
    var BALL_MASS = ThreadedTelemetry.AtomicValueProvider(0.075)

    @ThreadedConfig(category = "TURRET")
    var BASKET_TARGET_HEIGHT = ThreadedTelemetry.AtomicValueProvider(10.0)

    @ThreadedConfig(category = "TURRET")
    var QUIET_PULLEY_SPEED = ThreadedTelemetry.AtomicValueProvider(5.0)

    @ThreadedConfig(category = "BARREL")
    var BARREL_REGULATOR_PARAMETERS = ThreadedTelemetry.AtomicValueProvider(RegulatorParameters())
}