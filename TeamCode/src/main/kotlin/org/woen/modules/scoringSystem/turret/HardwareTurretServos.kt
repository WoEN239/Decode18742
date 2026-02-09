package org.woen.modules.scoringSystem.turret

import androidx.core.math.MathUtils.clamp
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import org.woen.hotRun.HotRun
import org.woen.telemetry.configs.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.drivers.InfinityAxon

class HardwareTurretServos : IHardwareDevice {
    private lateinit var _angleSevo: Servo
    private lateinit var _rotateServo: InfinityAxon

    private var _rawAnglePosition = 0.0

    var anglePosition: Double
        get() = (_rawAnglePosition - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) /
                (Configs.TURRET.MAX_TURRET_ANGLE_SERVO - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) *
                (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) + Configs.TURRET.MIN_TURRET_ANGLE
        set(value) {
            _rawAnglePosition = (clamp(
                value, Configs.TURRET.MIN_TURRET_ANGLE, Configs.TURRET.MAX_TURRET_ANGLE
            )
                    - Configs.TURRET.MIN_TURRET_ANGLE) /
                    (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) *
                    (Configs.TURRET.MAX_TURRET_ANGLE_SERVO - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) +
                    Configs.TURRET.MIN_TURRET_ANGLE_SERVO
        }

    var targetRotatePosition
        get() = (_rotateServo.targetPosition - Configs.TURRET.ZERO_ROTATE_POS) * Configs.TURRET.ROTATE_SERVO_RATIO
        set(value) {
            _rotateServo.targetPosition =
                (clamp(value, Configs.TURRET.MIN_ROTATE, Configs.TURRET.MAX_ROTATE)
                        + Configs.TURRET.ZERO_ROTATE_POS) / Configs.TURRET.ROTATE_SERVO_RATIO
        }

    val rotateAtTarget
        get() = _rotateServo.atTarget

    val currentRotatePosition
        get() = (_rotateServo.position - Configs.TURRET.ZERO_ROTATE_POS) * Configs.TURRET.ROTATE_SERVO_RATIO

    override fun update() {
        _angleSevo.position = _rawAnglePosition

        _rotateServo.update()
    }

    override fun init(hardwareMap: HardwareMap) {
        _angleSevo = hardwareMap.get("turretAngleServo") as Servo
        _rotateServo = InfinityAxon(
            "turretRotateServo",
            "turretRotateEncoder",
            hardwareMap,
            regulator = Configs.TURRET.ROTATE_SERVO_REGULATOR,
            startPosition = Configs.TURRET.ZERO_ROTATE_POS / Configs.TURRET.ROTATE_SERVO_RATIO
        )

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _angleSevo.direction = Servo.Direction.REVERSE
            _rotateServo.init()
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("turret rotation", currentRotatePosition)
        }
    }

    override fun opModeStart() {
        _rotateServo.start()
    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }
}