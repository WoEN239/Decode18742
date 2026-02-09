package org.woen.modules.scoringSystem.brush


import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.hotRun.HotRun
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.motor.MotorOnly



class HardwareBrush : IHardwareDevice {
    private lateinit var _motor: DcMotorEx
    var isSafe = true
    var isSafe1 = true
    var volt = 0.0
    private var motorPower = 0.0

    override fun update() {
        voltageSafe()
        _motor.power = motorPower
    }

    enum class BrushDirection {
        FORWARD,
        STOP,
        REVERSE,
    }

    fun voltageSafe() {
//        volt = _motor.getCurrent(CurrentUnit.AMPS)
//        isSafe = volt < Configs.BRUSH.BRUSH_TARGET_CURRENT
//        isSafe1 = volt < Configs.BRUSH.BRUSH_BIG_TARGET_CURRENT
    }

    fun setDir(dir: BrushDirection) {
        motorPower = when (dir) {
            BrushDirection.FORWARD ->
                1.0 //ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.BRUSH.BRUSH_POWER)

            BrushDirection.STOP -> 0.0

            BrushDirection.REVERSE ->
                -1.0 //-ThreadedBattery.LAZY_INSTANCE.voltageToPower(Configs.BRUSH.BRUSH_POWER)
        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _motor = MotorOnly(hardwareMap.get("brushMotor") as DcMotorEx)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        }
    }

    override fun opModeStart() {

    }

    override fun opModeStop() {

    }

    override fun dispose() {
    }
}