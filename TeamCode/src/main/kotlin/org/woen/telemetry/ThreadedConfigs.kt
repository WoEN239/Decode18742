package org.woen.telemetry

object ThreadedConfigs {
    @ThreadedConfig(category = "aboba")
    var aboba = ThreadedTelemetry.AtomicProvider(5)
}