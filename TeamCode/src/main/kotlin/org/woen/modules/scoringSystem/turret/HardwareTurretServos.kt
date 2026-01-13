package org.woen.modules.scoringSystem.turret

import androidx.core.math.MathUtils.clamp
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice

class HardwareTurretServos: IHardwareDevice {
//    private lateinit var _angleSevo: Servo
//    private lateinit var _rotateServo: Servo

    var rawAnglePosition = 0.0

    var anglePosition: Double
        get() = (rawAnglePosition - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) /
                (Configs.TURRET.MAX_TURRET_ANGLE_SERVO - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) *
                (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) + Configs.TURRET.MIN_TURRET_ANGLE
        set(value) {
            rawAnglePosition = (clamp(
                value, Configs.TURRET.MIN_TURRET_ANGLE, Configs.TURRET.MAX_TURRET_ANGLE
            )
                    - Configs.TURRET.MIN_TURRET_ANGLE) /
                    (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) *
                    (Configs.TURRET.MAX_TURRET_ANGLE_SERVO - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) +
                    Configs.TURRET.MIN_TURRET_ANGLE_SERVO
        }

    var rawRotatePosition = 0.0

    var targetRotatePosition: Double
        get() =
            (rawRotatePosition - Configs.TURRET.ZERO_ROTATE_POS) * Configs.TURRET.ROTATE_SERVO_TURNS * Configs.TURRET.ROTATE_SERVO_RATIO
        set(value) {
            rawRotatePosition = clamp(
                value, Configs.TURRET.MIN_ROTATE,
                Configs.TURRET.MAX_ROTATE
            ) / Configs.TURRET.ROTATE_SERVO_RATIO / Configs.TURRET.ROTATE_SERVO_TURNS +
                    Configs.TURRET.ZERO_ROTATE_POS
        }

    override fun update() {
//        _angleSevo.position = rawAnglePosition
//        _rotateServo.position = rawRotatePosition
    }

    override fun init(hardwareMap: HardwareMap) {
//        _angleSevo = hardwareMap.get("turretAngleServo") as Servo

//        _rotateServo = hardwareMap.get("turretRotateServo") as Servo

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
//            (_rotateServo as PwmControl).pwmRange = PwmControl.PwmRange(500.0, 2500.0)

//            _rotateServo.direction = Servo.Direction.REVERSE

//            _angleSevo.direction = Servo.Direction.REVERSE
        }

        targetRotatePosition = 0.0
    }

    override fun opModeStart() {
        
    }

    override fun opModeStop() {
        
    }

    override fun dispose() {
        
    }
}