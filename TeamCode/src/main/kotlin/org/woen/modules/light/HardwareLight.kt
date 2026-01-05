package org.woen.modules.light


import org.woen.hotRun.HotRun
import org.woen.utils.drivers.LEDLine
import org.woen.threading.hardware.IHardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap

import org.woen.telemetry.Configs.LIGHT.BLUE_B_POWER
import org.woen.telemetry.Configs.LIGHT.BLUE_G_POWER
import org.woen.telemetry.Configs.LIGHT.BLUE_R_POWER
import org.woen.telemetry.Configs.LIGHT.GREEN_B_POWER
import org.woen.telemetry.Configs.LIGHT.GREEN_G_POWER
import org.woen.telemetry.Configs.LIGHT.GREEN_R_POWER
import org.woen.telemetry.Configs.LIGHT.ORANGE_B_POWER
import org.woen.telemetry.Configs.LIGHT.ORANGE_G_POWER
import org.woen.telemetry.Configs.LIGHT.ORANGE_R_POWER
import org.woen.telemetry.Configs.LIGHT.RED_B_POWER
import org.woen.telemetry.Configs.LIGHT.RED_G_POWER
import org.woen.telemetry.Configs.LIGHT.RED_R_POWER



class HardwareLight(private val _rName: String, private val _gName: String, private val _bName: String): IHardwareDevice {
    private lateinit var _rPort: LEDLine
    private lateinit var _gPort: LEDLine
    private lateinit var _bPort: LEDLine

    var color = Light.LightColor.GREEN

    
    override fun update()
    {
        when(color)
        {
            Light.LightColor.GREEN ->
            {
                _rPort.power = GREEN_R_POWER
                _gPort.power = GREEN_G_POWER
                _bPort.power = GREEN_B_POWER
            }
            Light.LightColor.BLUE ->
            {
                _rPort.power = BLUE_R_POWER
                _gPort.power = BLUE_G_POWER
                _bPort.power = BLUE_B_POWER
            }

            Light.LightColor.ORANGE ->
            {
                _rPort.power = ORANGE_R_POWER
                _gPort.power = ORANGE_G_POWER
                _bPort.power = ORANGE_B_POWER
            }
            Light.LightColor.RED ->
            {
                _rPort.power = RED_R_POWER
                _gPort.power = RED_G_POWER
                _bPort.power = RED_B_POWER
            }
        }
    }

    override fun init(hardwareMap: HardwareMap)
    {
        _rPort = LEDLine(hardwareMap, _rName)
        _gPort = LEDLine(hardwareMap, _gName)
        _bPort = LEDLine(hardwareMap, _bName)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _rPort.init()
            _gPort.init()
            _bPort.init()
        }
    }

    override fun opModeStart() { }
    override fun opModeStop()  { }

    override fun dispose()     { }
}