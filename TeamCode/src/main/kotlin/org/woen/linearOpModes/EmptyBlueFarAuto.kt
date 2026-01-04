package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun

@Autonomous
class EmptyBlueFarAuto : LinearOpMode() {
    override fun runOpMode() {
        HotRun.LAZY_INSTANCE.currentStartPosition = HotRun.StartPosition.BLUE_FAR

        waitForStart()
        resetRuntime()

        while (opModeIsActive()) {
        }

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode(TeleOp::class.simpleName)
    }
}