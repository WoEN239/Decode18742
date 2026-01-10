package org.woen.tests

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.utils.drivers.LEDLine


@Config
internal object LED_TEST
{
    @JvmField
    var SECTOR = 3.0

    @JvmField
    var POWER = 1.0
}



@TeleOp(name = "!ЖМИ СЮДА ЧТОБЫ СВЕТИТЬСЯ")
class LightTest: LinearOpMode() {
    override fun runOpMode() {
        val er = LEDLine(hardwareMap, "expansionR")
        val eg = LEDLine(hardwareMap, "expansionG")
        val eb = LEDLine(hardwareMap, "expansionB")
        val cr = LEDLine(hardwareMap, "controlR")
        val cg = LEDLine(hardwareMap, "controlG")
        val cb = LEDLine(hardwareMap, "controlB")

        val timer = ElapsedTime()

        waitForStart()
        resetRuntime()

        timer.reset()

        while (opModeIsActive()){
            val r: Double
            val g: Double
            val b: Double

            val time = timer.seconds() % (3.0 * LED_TEST.SECTOR)

            if(time < LED_TEST.SECTOR){
                r = time / LED_TEST.SECTOR * LED_TEST.POWER
                g = (LED_TEST.SECTOR - time) / LED_TEST.SECTOR * LED_TEST.POWER
                b = 0.0
            }
            else if(time < 2.0 * LED_TEST.SECTOR){
                r = (2.0 * LED_TEST.SECTOR - time) / LED_TEST.SECTOR * LED_TEST.POWER
                g = 0.0
                b = (time - LED_TEST.SECTOR) / LED_TEST.SECTOR * LED_TEST.POWER
            }
            else{
                r = 0.0
                g = (time - 2.0 * LED_TEST.SECTOR) / LED_TEST.SECTOR * LED_TEST.POWER
                b = (3.0 * LED_TEST.SECTOR - time) / LED_TEST.SECTOR * LED_TEST.POWER
            }

            cr.power = r
            cg.power = g
            cb.power = b

            er.power = r
            eg.power = g
            eb.power = b
        }
    }
}