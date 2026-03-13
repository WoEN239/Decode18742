package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.telemetry.configs.Configs
import org.woen.utils.drivers.InfinityAxon
import org.woen.utils.regulator.RegulatorParameters
import kotlin.math.PI

@Config
internal object AXON_TEST {
    @JvmField
    var TARGET_POSITION = 0.0

    @JvmField
    var REGULATOR = RegulatorParameters(kP = 1.2, kD = 0.01, limitU = 1.0)
}

@TeleOp(group = "tests")
class AxonTest : LinearOpMode() {
    override fun runOpMode() {
        val servo = InfinityAxon("turretRotateServo", "turretRotateEncoder", hardwareMap, regulator = AXON_TEST.REGULATOR)

        var state = false
        val timer = ElapsedTime()

        waitForStart()
        resetRuntime()

//        pulleyMotor.power = -0.8

        servo.start()

        while (opModeIsActive()) {
            servo.targetPosition = ((if(state) PI / 2.0 else -PI / 2.0) + Math.toRadians(85.69090909090909)) / Configs.TURRET.ROTATE_SERVO_RATIO

            if(timer.seconds() > 2.0) {
                state = !state
                timer.reset()
            }

//            servo.targetPosition = ((AXON_TEST.TARGET_POSITION + 85.69090909090909) / Configs.TURRET.ROTATE_SERVO_RATIO / 180.0 * PI)

            val telem = FtcDashboard.getInstance().telemetry

            telem.addData("target", Math.toDegrees(servo.targetPosition) * Configs.TURRET.ROTATE_SERVO_RATIO - 85.69090909090909)
            telem.addData("position", servo.position / PI * 180.0 * Configs.TURRET.ROTATE_SERVO_RATIO - 85.69090909090909)

            telem.update()

            servo.update()
        }
    }
}