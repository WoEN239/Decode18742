package org.woen.tests

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.utils.drivers.LEDLine

@Config
internal object LED_TEST{
    @JvmField
    var SECTR = 6.0

    @JvmField
    var POWER = 1.0
}

@TeleOp
class LightTest: LinearOpMode() {
    override fun runOpMode() {
        val r = LEDLine(hardwareMap, "expansionR")
        val g = LEDLine(hardwareMap, "expansionG")
        val b = LEDLine(hardwareMap, "expansionB")
        val timer = ElapsedTime()

        waitForStart()
        resetRuntime()

        timer.reset()

        while (opModeIsActive()){
            val time = timer.seconds() % (3.0 * LED_TEST.SECTR)

            if(time < LED_TEST.SECTR){
                r.power = time / LED_TEST.SECTR * LED_TEST.POWER
                g.power = (LED_TEST.SECTR - time) / LED_TEST.SECTR * LED_TEST.POWER
                b.power = 0.0
            }
            else if(time < 2.0 * LED_TEST.SECTR){
                r.power = (2.0 * LED_TEST.SECTR - time) / LED_TEST.SECTR * LED_TEST.POWER
                g.power = 0.0
                b.power = (time - LED_TEST.SECTR) / LED_TEST.SECTR * LED_TEST.POWER
            }
            else{
                r.power = 0.0
                g.power = (time - 2.0 * LED_TEST.SECTR) / LED_TEST.SECTR * LED_TEST.POWER
                b.power = (3.0 * LED_TEST.SECTR - time) / LED_TEST.SECTR * LED_TEST.POWER
            }
        }
    }
}