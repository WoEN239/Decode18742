package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.GamepadOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun

@TeleOp
class TeleOpMode : GamepadOpMode() {
    override fun runOpMode() {
        HotRun.LAZY_INSTANCE.run(this, HotRun.RunMode.MANUAL)
    }
}