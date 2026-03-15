package org.woen.opModes

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.collector.Collector
import org.woen.collector.GameSettings
import org.woen.collector.GameState
import org.woen.collector.RunMode
import org.woen.collector.StartOrientation

@Autonomous
class Auto : LinearOpMode() {
    override fun runOpMode() {
        GameState.runMode = RunMode.AUTO

        val collector = Collector(this)

        var selectedOrientationIndex =
            StartOrientation.entries.indexOf(GameSettings.startOrientation)
        val orientations = StartOrientation.entries.toTypedArray()

        GameSettings.startOrientation = orientations[selectedOrientationIndex]

        var _oldLeftBumperState = false
        var _oldRightBumperState = false

        while (!isStarted()) {
            collector.initUpdateEvent.invoke()

            val leftBumper = gamepad1.left_bumper
            val rightBumper = gamepad1.right_bumper

            if (leftBumper && !_oldLeftBumperState) {
                selectedOrientationIndex++
                selectedOrientationIndex %= orientations.size
            }

            if (rightBumper && !_oldRightBumperState) {
                selectedOrientationIndex--

                if (selectedOrientationIndex < 0)
                    selectedOrientationIndex += orientations.size
            }

            _oldLeftBumperState = leftBumper
            _oldRightBumperState = rightBumper

            val dashboardTelemetry = FtcDashboard.getInstance().telemetry

            telemetry.addLine("selected orientation ${orientations[selectedOrientationIndex].name}")
            dashboardTelemetry.addLine("selected orientation ${orientations[selectedOrientationIndex].name}")

            telemetry.update()
            dashboardTelemetry.update()
        }

        GameSettings.startOrientation = orientations[selectedOrientationIndex]

        resetRuntime()

        collector.startEvent.invoke()

        while (opModeIsActive()) {
            collector.updateEvent.invoke()

            telemetry.update()
        }

        collector.stopEvent.invoke()
        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode(TeleOp::class.simpleName)
    }
}