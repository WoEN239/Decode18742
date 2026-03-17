package org.woen.opModes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.collector.RunMode

@Autonomous
class EmptyAuto: LinearOpMode() {
    override fun runOpMode() {
        waitForStart()
        resetRuntime()

        while (opModeIsActive()){

        }

        OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
            .initOpMode(TeleOp::class.simpleName)
    }
}