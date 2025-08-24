package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.woen.hotRun.HotRun

@TeleOp
class TeleOpMode: LinearOpMode() {
    override fun runOpMode() {
        if(HotRun.INSTANCE == null)
            HotRun.init()

        HotRun.INSTANCE?.run(this)
    }
}