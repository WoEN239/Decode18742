package org.woen.telemetry

object ThreadedConfigs {
    @ThreadedConfig(category = "TELEMETRY")
    var UPDATE_HZ = ThreadedTelemetry.AtomicValueProvider(5)
}