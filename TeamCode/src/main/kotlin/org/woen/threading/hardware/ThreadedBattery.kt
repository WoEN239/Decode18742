package org.woen.threading.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.VoltageSensor
import java.util.concurrent.atomic.AtomicReference

class ThreadedBattery private constructor(): IHardwareDevice {
    companion object{
        private var _nullableInstance: ThreadedBattery? = null

        @get:Synchronized
        val LAZY_INSTANCE: ThreadedBattery
            get() {
                if(_nullableInstance == null)
                    _nullableInstance = ThreadedBattery()

                return _nullableInstance!!
            }

        fun restart(){
            _nullableInstance?.let { HardwareThreads.LAZY_INSTANCE.CONTROL.removeDevices(it) }
            _nullableInstance = null
        }
    }

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(this)
    }

    var currentVoltage: Double
        get() = _atomicVoltage.get()
        private set(value){
            _atomicVoltage.set(value)
        }

    private var _atomicVoltage = AtomicReference(1.0)

    fun voltageToPower(voltage: Double) = currentVoltage / voltage

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