package org.woen.modules.light

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.drivers.LEDLine

class HardwareLight(private val _rName: String, private val _gName: String, private val _bName: String): IHardwareDevice {
    private lateinit var _rPort: LEDLine
    private lateinit var _gPort: LEDLine
    private lateinit var _bPort: LEDLine

    var color = Light.LightColor.GREEN

    override fun update() {
        when(color){
            Light.LightColor.GREEN -> {
                _rPort.power = Configs.LIGHT.GREEN_R_POWER
                _gPort.power = Configs.LIGHT.GREEN_G_POWER
                _bPort.power = Configs.LIGHT.GREEN_B_POWER
            }
            Light.LightColor.BLUE -> {
                _rPort.power = Configs.LIGHT.BLUE_R_POWER
                _gPort.power = Configs.LIGHT.BLUE_G_POWER
                _bPort.power = Configs.LIGHT.BLUE_B_POWER
            }

            Light.LightColor.ORANGE -> {
                _rPort.power = Configs.LIGHT.ORANGE_R_POWER
                _gPort.power = Configs.LIGHT.ORANGE_G_POWER
                _bPort.power = Configs.LIGHT.ORANGE_B_POWER
            }
        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _rPort = LEDLine(hardwareMap, _rName)
        _gPort = LEDLine(hardwareMap, _gName)
        _bPort = LEDLine(hardwareMap, _bName)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _rPort.init()
            _gPort.init()
            _bPort.init()
        }
    }

    override fun opModeStart() {

    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}