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
@Disabled
internal object AXON_TEST {
    @JvmField
    var TARGET_POSITION = 0.0

    @JvmField
    var REGULATOR = RegulatorParameters()
}

@TeleOp(group = "tests")
@Disabled
class AxonTest : LinearOpMode() {
    override fun runOpMode() {
        val servo = InfinityAxon("turretRotateServo", "turretRotateEncoder", hardwareMap, regulator = AXON_TEST.REGULATOR)
        val pulleyMotor = hardwareMap.get("pulleyMotor") as DcMotorEx

        var state = false
        val timer = ElapsedTime()

        waitForStart()
        resetRuntime()

//        pulleyMotor.power = -0.8

        servo.start()

        while (opModeIsActive()) {
//            servo.targetPosition = (if(state) PI / 2.0 else -PI / 2.0) + Math.toRadians(164.5454545454546)
//
//            if(timer.seconds() > 1.2) {
//                state = !state
//                timer.reset()
//            }

            servo.targetPosition = ((AXON_TEST.TARGET_POSITION + 164.5454545454546) / 180.0 * PI) / Configs.TURRET.ROTATE_SERVO_RATIO

            val telem = FtcDashboard.getInstance().telemetry

            telem.addData("target", Math.toDegrees(servo.targetPosition) * Configs.TURRET.ROTATE_SERVO_RATIO)
            telem.addData("position", servo.position / PI * 180.0 * Configs.TURRET.ROTATE_SERVO_RATIO)

            telem.update()

            servo.update()
        }
    }
}