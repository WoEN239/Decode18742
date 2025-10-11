package org.woen.modules.scoringSystem.brush

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice
import java.util.concurrent.atomic.AtomicReference

class brush_hard(private val _deviceName: String)  : IHardwareDevice {
    private lateinit var _motor: DcMotorEx
    public var IsSafe= AtomicReference(true);
    override fun update() {
        voltageSafe();
    }

    fun voltageSafe(){
        var volt=_motor.getCurrent(CurrentUnit.AMPS);
        if(volt >= Configs.BRUSH.BRUSH_TARGET_CURRENT) IsSafe.set(false); else IsSafe.set(true);


    }

    fun setDir(Motor: Int){
        when(Motor){
            Configs.BRUSH.BRUSH_MOTORS_FORWARD -> {
                _motor.setPower(1.0);
            }
            Configs.BRUSH.BRUSH_MOTORS_STOP -> {
                _motor.setPower(0.0);
            }
            Configs.BRUSH.BRUSH_MOTORS_BACK -> {
                _motor.setPower(-1.0);
            }
        }

    }
    override fun init(hardwareMap: HardwareMap) {
        _motor = hardwareMap.get(_deviceName) as DcMotorEx;

        _motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    }

    override fun dispose() {
    }
}