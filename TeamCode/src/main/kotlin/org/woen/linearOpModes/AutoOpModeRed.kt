package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun

@Autonomous
class AutoOpModeRed : LinearOpMode() {
    override fun runOpMode() {
        HotRun.LAZY_INSTANCE.currentRunColor = HotRun.RunColor.RED
        HotRun.LAZY_INSTANCE.run(this, HotRun.RunMode.AUTO)

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode("TeleOpMode")
    }
}