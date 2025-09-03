package org.woen.modules

import com.qualcomm.robotcore.hardware.Servo
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.HardwareThreads
import org.woen.threading.ThreadManager
import org.woen.threading.hardware.ThreadedServo
import org.woen.utils.servoAngle.ServoAngle
import org.woen.utils.timers.ReversedElapsedTime
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2
import kotlin.concurrent.thread
import kotlin.math.PI

data class A(var a: Int = 5)

class TestModule : IModule<A> {
    private var _servo = ThreadedServo("testServo", maxAngle = 180.0)

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_servo)
    }

    override fun process(data: A) {
    }

    val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = true) {
        while (Thread.currentThread().isInterrupted){
            _servo.targetAngle = 180.0
            Thread.sleep(1000)
            _servo.targetAngle = 0.0
            Thread.sleep(1000)
        }
    })

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