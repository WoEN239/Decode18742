package org.woen.modules.light


import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.telemetry.Configs.BATTERY.LOW_VOLTAGE
import org.woen.telemetry.Configs.BATTERY.LOW_VOLTAGE_TRIGGER_TIME
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.ThreadedBattery



data class SetLightColorEvent(val color: Light.LightColor)
class RequireLedColorEvent(var color: Light.LightColor = Light.LightColor.BLUE)



class Light: IModule
{
    enum class LightColor
    {
        GREEN,
        BLUE,
        ORANGE,
        RED
    }

    private val _expansionLed = HardwareLight("expansionR", "expansionG", "expansionB")
    private val _controlLed = HardwareLight("controlR", "controlG", "controlB")

    private var _currentLightColor = LightColor.BLUE

    private var _lightJob: Job? = null

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_expansionLed)
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_controlLed)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SetLightColorEvent::class, {
            _currentLightColor = it.color
        })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequireLedColorEvent::class, {
            it.color = _currentLightColor
        })
    }

    private val _lowVoltageTimer = ElapsedTime()

    override suspend fun process()
    {
        _lightJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if (ThreadedBattery.LAZY_INSTANCE.currentVoltage < LOW_VOLTAGE)
            {
                if(_lowVoltageTimer.seconds() > LOW_VOLTAGE_TRIGGER_TIME)
                {
                    _expansionLed.color = LightColor.RED
                    _controlLed.color   = LightColor.RED
                }
                else
                {
                    _expansionLed.color = _currentLightColor
                    _controlLed.color   = _currentLightColor
                }
            }
            else
            {
                _expansionLed.color = _currentLightColor
                _controlLed.color = _currentLightColor
                _lowVoltageTimer.reset()
            }
        }
    }

    override val isBusy: Boolean
        get() = _lightJob != null && !_lightJob!!.isCompleted

    override fun opModeStart()
    {
        _expansionLed.color = _currentLightColor
        _controlLed.color   = _currentLightColor
    }

    override fun opModeStop() { }

    override fun dispose() {
        _lightJob?.cancel()
    }
}