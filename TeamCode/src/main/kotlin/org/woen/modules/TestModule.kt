package org.woen.modules

import org.woen.telemetry.ThreadedTelemetry
import org.woen.utils.timers.ReversedElapsedTime

data class A(var a: Int = 5)

class TestModule : IModule<A> {
    init {
        ThreadedTelemetry.LAZY_INSTANCE.startTemporarySender(ReversedElapsedTime(10.0), {
            it.addLine("b")
        })

        ThreadedTelemetry.LAZY_INSTANCE.addSender {
            it.addData("c", 4)
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