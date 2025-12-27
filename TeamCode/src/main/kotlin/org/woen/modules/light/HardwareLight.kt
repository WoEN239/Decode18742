package org.woen.modules.light

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice

class HardwareLight(private val _rName: String, private val _gName: String, private val _bName: String): IHardwareDevice {
    private lateinit var _rPort: Servo
    private lateinit var _gPort: Servo
    private lateinit var _bPort: Servo

    var color = Light.LightColor.GREEN

    override fun update() {
        when(color){
            Light.LightColor.GREEN -> {
                _rPort.position = Configs.LIGHT.GREEN_R_POWER
                _gPort.position = Configs.LIGHT.GREEN_G_POWER
                _bPort.position = Configs.LIGHT.GREEN_B_POWER
            }
            Light.LightColor.BLUE -> {
                _rPort.position = Configs.LIGHT.BLUE_R_POWER
                _gPort.position = Configs.LIGHT.BLUE_G_POWER
                _bPort.position = Configs.LIGHT.BLUE_B_POWER
            }

            Light.LightColor.ORANGE -> {
                _rPort.position = Configs.LIGHT.ORANGE_R_POWER
                _gPort.position = Configs.LIGHT.ORANGE_G_POWER
                _bPort.position = Configs.LIGHT.ORANGE_B_POWER
            }
        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _rPort = hardwareMap.get(_rName) as Servo
        _gPort = hardwareMap.get(_gName) as Servo
        _bPort = hardwareMap.get(_bName) as Servo

        (_rPort as PwmControl).pwmRange = PwmControl.PwmRange(0.0, 20000.0)
        (_gPort as PwmControl).pwmRange = PwmControl.PwmRange(0.0, 20000.0)
        (_bPort as PwmControl).pwmRange = PwmControl.PwmRange(0.0, 20000.0)
    }

    override fun opModeStart() {

    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}