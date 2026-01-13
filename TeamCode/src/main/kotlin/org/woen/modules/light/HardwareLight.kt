package org.woen.modules.light


import kotlin.math.abs

import org.woen.hotRun.HotRun
import org.woen.utils.drivers.LEDLine
import org.woen.threading.hardware.IHardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.modules.light.Light.LightColor.RED
import org.woen.modules.light.Light.LightColor.BLUE
import org.woen.modules.light.Light.LightColor.GREEN
import org.woen.modules.light.Light.LightColor.ORANGE

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
import org.woen.telemetry.Configs.LIGHT.RED_FLASHING_SPEED



class HardwareLight(private val _rName: String,
                    private val _gName: String,
                    private val _bName: String)
    : IHardwareDevice
{
//    private lateinit var _rPort: LEDLine
//    private lateinit var _gPort: LEDLine
//    private lateinit var _bPort: LEDLine

    private val _shimmerTimer = ElapsedTime()
    private var _shimmerResetColor = GREEN

    var lightColor = BLUE


    
    override fun update()
    {
        when (lightColor)
        {
            GREEN ->
            {
                _shimmerResetColor = GREEN

//                _rPort.power = GREEN_R_POWER
//                _gPort.power = GREEN_G_POWER
//                _bPort.power = GREEN_B_POWER
            }
            BLUE ->
            {
                _shimmerResetColor = BLUE

//                _rPort.power = BLUE_R_POWER
//                _gPort.power = BLUE_G_POWER
//                _bPort.power = BLUE_B_POWER
            }

            ORANGE ->
            {
                _shimmerResetColor = ORANGE

//                _rPort.power = ORANGE_R_POWER
//                _gPort.power = ORANGE_G_POWER
//                _bPort.power = ORANGE_B_POWER
            }
            RED ->
            {
                if (_shimmerResetColor != RED) _shimmerTimer.reset()
                _shimmerResetColor = RED

//                _rPort.power = abs(
//                    RED_R_POWER -
//                        (_shimmerTimer.seconds() * RED_FLASHING_SPEED)
//                            % (2 * RED_R_POWER)
//                )
//                _gPort.power = RED_G_POWER
//                _bPort.power = RED_B_POWER
            }
        }
    }



    override fun init(hardwareMap: HardwareMap)
    {
//        _rPort = LEDLine(hardwareMap, _rName)
//        _gPort = LEDLine(hardwareMap, _gName)
//        _bPort = LEDLine(hardwareMap, _bName)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
//            _rPort.init()
//            _gPort.init()
//            _bPort.init()

            _shimmerTimer.reset()
        }
    }

    override fun opModeStart() { }
    override fun opModeStop()  { }

    override fun dispose()     { }
}