package org.woen.telemetry

import org.woen.utils.regulator.RegulatorParameters
import kotlin.math.PI

object ThreadedConfigs {
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

    @ThreadedConfig(category = "DRIVE_TRAIN")
    var DRIVE_SIDE_REGULATOR_PARAMS = RegulatorParameters()

    @ThreadedConfig(category = "DRIVE_TRAIN")
    var DRIVE_FORWARD_REGULATOR_PARAMS = RegulatorParameters()

    @ThreadedConfig(category = "DRIVE_TRAIN")
    var DRIVE_ROTATE_REGULATOR_PARAMS = RegulatorParameters()

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
}