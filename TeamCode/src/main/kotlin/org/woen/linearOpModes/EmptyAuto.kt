package org.woen.linearOpModes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode

@Autonomous
class EmptyAuto : LinearOpMode() {
    override fun runOpMode() {
        waitForStart()
        resetRuntime()

        while (opModeIsActive()) {
        }
    }
}