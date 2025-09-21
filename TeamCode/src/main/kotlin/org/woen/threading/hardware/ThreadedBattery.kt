package org.woen.threading.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.atomic.AtomicReference

class ThreadedBattery private constructor() : IHardwareDevice {
    companion object {
        private var _nullableInstance: ThreadedBattery? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: ThreadedBattery
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null) {
                    _nullableInstance = ThreadedBattery()
                    _nullableInstance?.initDevice()
                }

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance?.let { HardwareThreads.LAZY_INSTANCE.CONTROL.removeDevices(it) }
                _nullableInstance = null
            }
        }
    }

    private fun initDevice() {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(this)
    }

    var currentVoltage: Double
        get() = _atomicVoltage.get()
        private set(value) {
            _atomicVoltage.set(value)
        }

    private var _atomicVoltage = AtomicReference(1.0)

    fun voltageToPower(voltage: Double) = voltage / currentVoltage

    private lateinit var _battery: VoltageSensor

    override fun update() {
        currentVoltage = _battery.voltage
    }

    override fun init(hardwareMap: HardwareMap) {
        _battery = hardwareMap.get(VoltageSensor::class.java, "Control Hub")
    }

    override fun dispose() {

    }
}