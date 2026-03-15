package org.woen.opModes

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.collector.Collector
import org.woen.collector.GameState
import org.woen.collector.RunMode
import kotlin.math.abs

@TeleOp
class TeleOp: LinearOpMode() {
    override fun runOpMode() {
        GameState.runMode = RunMode.MANUAL

        val collector = Collector(this)

        while (!isStarted()) {
            if (abs(gamepad1.left_stick_x) > 0.01 || abs(gamepad1.left_stick_y) > 0.01 ||
                abs(gamepad1.right_stick_x) > 0.01 || abs(gamepad1.right_stick_y) > 0.01 ||
                gamepad1.left_trigger > 0.01 || gamepad1.right_trigger > 0.01 || gamepad1.left_bumper ||
                gamepad1.right_bumper || gamepad1.ps || gamepad1.touchpad || gamepad1.dpad_up ||
                gamepad1.dpad_down || gamepad1.dpad_left || gamepad1.dpad_right ||
                gamepad1.circle || gamepad1.square || gamepad1.triangle || gamepad1.cross)
                OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).startActiveOpMode()
        }

        resetRuntime()

        collector.startEvent.invoke()

        while (opModeIsActive()){
            collector.updateEvent.invoke()

            telemetry.update()
        }

        collector.stopEvent.invoke()
    }
}