package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.woen.hotRun.HotRun

@TeleOp
class TeleOpMode : LinearOpMode() {
    override fun runOpMode() {
        HotRun.LAZY_INSTANCE.run(this, HotRun.RunMode.MANUAL, true)
    }
}