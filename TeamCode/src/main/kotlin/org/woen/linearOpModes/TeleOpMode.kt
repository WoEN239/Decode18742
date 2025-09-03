package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.GamepadOpMode
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.woen.hotRun.HotRun
import org.woen.telemetry.ThreadedTelemetry

@TeleOp
class TeleOpMode: GamepadOpMode() {
    override fun runOpMode() {
        HotRun.LAZY_INSTANCE.run(this, HotRun.RunMode.MANUAL)
    }
}