package org.woen.opModes

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.collector.Collector
import org.woen.collector.GameSettings
import org.woen.collector.RunMode
import org.woen.collector.StartOrientation

@Autonomous
class Auto : LinearOpMode() {
    override fun runOpMode() {
        var selectedOrientationIndex =
            StartOrientation.entries.indexOf(GameSettings.startOrientation)
        val orientations = StartOrientation.entries.toTypedArray()

        GameSettings.startOrientation = orientations[selectedOrientationIndex]

        var oldLeftBumperState = false
        var oldRightBumperState = false

        val dashboardTelemetry = FtcDashboard.getInstance().telemetry

        while (!isStarted() && !gamepad1.touchpad) {
            val leftBumper = gamepad1.left_bumper
            val rightBumper = gamepad1.right_bumper

            if (leftBumper && !oldLeftBumperState) {
                selectedOrientationIndex++
                selectedOrientationIndex %= orientations.size
            }

            if (rightBumper && !oldRightBumperState) {
                selectedOrientationIndex--

                if (selectedOrientationIndex < 0)
                    selectedOrientationIndex += orientations.size
            }

            oldLeftBumperState = leftBumper
            oldRightBumperState = rightBumper

            telemetry.addLine("selected orientation ${orientations[selectedOrientationIndex].name}")
            dashboardTelemetry.addLine("selected orientation ${orientations[selectedOrientationIndex].name}")

            telemetry.update()
            dashboardTelemetry.update()
        }

        GameSettings.startOrientation = orientations[selectedOrientationIndex]

        val collector = Collector(this, RunMode.AUTO)

        do {
            collector.initUpdateEvent.invoke()

            telemetry.addLine("selected orientation ${orientations[selectedOrientationIndex].name}")
            telemetry.addLine("selected!")
            dashboardTelemetry.addLine("selected orientation ${orientations[selectedOrientationIndex].name}")
            dashboardTelemetry.addLine("selected!")

            telemetry.update()
            dashboardTelemetry.update()
        } while (!isStarted())

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