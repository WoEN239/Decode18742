package org.woen.modules

import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.ThreadManager
import org.woen.threading.hardware.ThreadedServo
import kotlin.concurrent.thread

data class A(var a: Int = 5)

class TestModule : IModule<A> {
    private var _servo1 = ThreadedServo("testServo1", maxAngle = 180.0)
    private var _servo2 = ThreadedServo("testServo2", maxAngle = 180.0)
    private var _servo3 = ThreadedServo("testServo3", maxAngle = 180.0)
    private var _servo4 = ThreadedServo("testServo4", maxAngle = 180.0)
    private var _servo5 = ThreadedServo("testServo5", maxAngle = 180.0)
    private var _servo6 = ThreadedServo("testServo6", maxAngle = 180.0)

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_servo1)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_servo2)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_servo3)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_servo4)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_servo5)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_servo6)
    }

    override fun process(data: A) {

    }

    val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = true) {
        while (!Thread.currentThread().isInterrupted){
            _servo1.targetAngle = 180.0
            _servo2.targetAngle = 180.0
            _servo3.targetAngle = 180.0
            _servo4.targetAngle = 180.0
            _servo5.targetAngle = 180.0
            _servo6.targetAngle = 180.0
            Thread.sleep(4000)
            _servo1.targetAngle = 0.0
            _servo2.targetAngle = 0.0
            _servo3.targetAngle = 0.0
            _servo4.targetAngle = 0.0
            _servo5.targetAngle = 0.0
            _servo6.targetAngle = 0.0
            Thread.sleep(4000)
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