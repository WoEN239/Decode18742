package org.woen.modules

import org.woen.telemetry.ThreadedTelemetry

data class A(var a: Int = 5)

class TestModule: IModule<A> {
    init {
        ThreadedTelemetry.LAZY_INSTANCE.addTelemetrySender {
            it.addData("b", 6)
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