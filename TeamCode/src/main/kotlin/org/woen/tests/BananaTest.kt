package org.woen.tests

import androidx.core.math.MathUtils.clamp
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.Servo
import org.woen.telemetry.configs.Configs
import java.lang.Math.toRadians

@Config
internal object BANAN_TEST_CONFIG{
    @JvmField
    var POSITION = 45.0
}

@TeleOp(group = "tests")
class BananaTest: LinearOpMode() {
    override fun runOpMode() {
        val banan = hardwareMap.get("turretAngleServo") as Servo

        banan.direction = Servo.Direction.REVERSE

        waitForStart()
        resetRuntime()

        while (opModeIsActive()){
            banan.position = (clamp(
               toRadians(BANAN_TEST_CONFIG.POSITION) , Configs.TURRET.MIN_TURRET_ANGLE, Configs.TURRET.MAX_TURRET_ANGLE
            )
                    - Configs.TURRET.MIN_TURRET_ANGLE) /
                    (Configs.TURRET.MAX_TURRET_ANGLE - Configs.TURRET.MIN_TURRET_ANGLE) *
                    (Configs.TURRET.MAX_TURRET_ANGLE_SERVO - Configs.TURRET.MIN_TURRET_ANGLE_SERVO) +
                    Configs.TURRET.MIN_TURRET_ANGLE_SERVO
        }
    }
}