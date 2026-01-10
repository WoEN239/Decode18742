package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun

@Autonomous
@Disabled
class EmptyRedFarAuto : LinearOpMode() {
    override fun runOpMode() {
        HotRun.LAZY_INSTANCE.currentStartPosition = HotRun.StartPosition.RED_FAR

        waitForStart()
        resetRuntime()

        while (opModeIsActive()) {
        }

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode(TeleOp::class.simpleName)
    }
}