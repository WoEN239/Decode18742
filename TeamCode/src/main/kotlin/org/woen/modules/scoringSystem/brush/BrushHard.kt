package org.woen.modules.scoringSystem.brush

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice
import java.util.concurrent.atomic.AtomicReference

class BrushHard(private val _deviceName: String)  : IHardwareDevice {
    private lateinit var _motor: DcMotorEx
    public var IsSafe= AtomicReference(true);
    private var motor_power=0.0;
    override fun update() {
        voltageSafe();
        _motor.setPower(motor_power);
    }
    enum class motor_state {
        ACKT,
        NOT_ACKT,
        REVERS,
    }


    fun voltageSafe(){
        var volt=_motor.getCurrent(CurrentUnit.AMPS);
        if(volt >= Configs.BRUSH.BRUSH_TARGET_CURRENT) IsSafe.set(false); else IsSafe.set(true);


    }

    fun setDir(Motor: motor_state){
        when(Motor){
            motor_state.ACKT  -> {
                motor_power=1.0;
            }
            motor_state.NOT_ACKT-> {
                motor_power=0.0;
            }
            motor_state.REVERS -> {
               motor_power=-1.0;
            }
        }

    }
    override fun init(hardwareMap: HardwareMap) {
        _motor = hardwareMap.get(_deviceName) as DcMotorEx;

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

            _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        }
    }

    override fun dispose() {
    }
}