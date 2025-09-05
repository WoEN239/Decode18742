package org.woen.telemetry

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
}