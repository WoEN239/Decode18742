package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun

@Autonomous
class EmptyBlueAuto : LinearOpMode() {
    override fun runOpMode() {
        HotRun.LAZY_INSTANCE.currentStartPosition = HotRun.StartPosition.BLUE

        waitForStart()
        resetRuntime()

        while (opModeIsActive()) {
        }

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode("TeleOp")
    }
}