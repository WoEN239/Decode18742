package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun


@TeleOp
class FullRestart : LinearOpMode() {
    override fun runOpMode() {
        HotRun.restart()

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .startActiveOpMode()

        waitForStart()

        while (opModeIsActive());
    }
}