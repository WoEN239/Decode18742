package org.woen.linearOpModes

import androidx.appcompat.app.ActionBar
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun

@Autonomous
@Disabled
class EmptyBlueCloseAuto : LinearOpMode() {
    override fun runOpMode() {
        HotRun.LAZY_INSTANCE.currentStartPosition = HotRun.StartPosition.BLUE_CLOSE

        waitForStart()
        resetRuntime()

        while (opModeIsActive()) {
        }

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode(TeleOp::class.simpleName)
    }
}