package org.woen.modules.scoringSystem.brush

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.motor.MotorOnly

class HardwareBrush : IHardwareDevice {
    private lateinit var _motor: DcMotorEx
    var isSafe = true
    var volt = 0.0
    private var motorPower = 0.0

    override fun update() {
        if (HotRun.LAZY_INSTANCE.currentRunState != HotRun.RunState.RUN)
            return

        voltageSafe()
        _motor.power = motorPower
    }

    enum class BrushState {
        FORWARD,
        STOP,
        REVERS,
    }

    fun voltageSafe() {
        volt = _motor.getCurrent(CurrentUnit.AMPS)
        isSafe = volt < Configs.BRUSH.BRUSH_TARGET_CURRENT
    }

    fun setDir(dir: BrushState) {
        motorPower = when (dir) {
            BrushState.FORWARD -> 1.0

            BrushState.STOP -> 0.0

            BrushState.REVERS -> -1.0
        }

    }

    override fun init(hardwareMap: HardwareMap) {
        _motor = MotorOnly(hardwareMap.get("brushMotor") as DcMotorEx)

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        }
    }

    override fun dispose() {
    }
}