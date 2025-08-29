package org.woen.modules

import org.woen.telemetry.ThreadedTelemetry
import org.woen.utils.timers.ReversedElapsedTime
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2
import kotlin.concurrent.thread
import kotlin.math.PI

data class A(var a: Int = 5)

class TestModule : IModule<A> {
    init {
        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("time", System.currentTimeMillis())
        }
    }

    override fun process(data: A) {
    }

    override fun getData(): A {
        val data = A()

        return data
    }

    override fun isBusy(): Boolean {
        return false
    }

    override fun dispose() {

    }
}