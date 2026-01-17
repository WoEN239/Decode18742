package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.utils.drivers.InfinityAxon
import org.woen.utils.regulator.RegulatorParameters
import kotlin.math.PI

@Config
internal object AXON_TEST {
    @JvmField
    var TARGET_POSITION = 0.0
}

@TeleOp
class AxonTest : LinearOpMode() {
    override fun runOpMode() {
        val servo = InfinityAxon("axon", "axonEncoder", hardwareMap)

        var state = false
        val timer = ElapsedTime()

        waitForStart()
        resetRuntime()

        servo.start()

        while (opModeIsActive()) {
//            servo.targetPosition = if(state) 0.0  else 2.0 * PI
//
//            if(timer.seconds() > 1.0) {
//                state = !state
//                timer.reset()
//            }

            servo.targetPosition = AXON_TEST.TARGET_POSITION / 180.0 * PI

            servo.update()
        }
    }
}