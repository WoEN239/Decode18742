package org.woen.tests

import org.woen.collector.Collector
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.woen.utils.drivers.LEDLine


@Config
internal object LED_TEST{
    @JvmField
    var POWER = 0.0
}

@TeleOp
class LEDTest: LinearOpMode() {
    override fun runOpMode() {
        val ledLeftR = LEDLine("leftR",hardwareMap)
        val ledLeftG = LEDLine("leftG",hardwareMap)
        val ledLeftB = LEDLine("leftB",hardwareMap)

        ledLeftR.power = 0.0
        ledLeftG.power = 0.0
        ledLeftB.power = 0.0

        val ledRightR = LEDLine("rightR",hardwareMap)
        val ledRightG = LEDLine("rightG",hardwareMap)
        val ledRightB = LEDLine("rightB",hardwareMap)

        ledRightR.power = 0.0
        ledRightG.power = 0.0
        ledRightB.power = 0.0


        waitForStart()
        resetRuntime()

        var hueR = 60;
        var hueL = 0;

        while (opModeIsActive()) {
            if (hueR in 0..120) {
                ledRightR.power = 1-(hueR.toDouble() / 120)
                ledRightG.power = hueR.toDouble() / 120
                ledRightB.power = 0.0
            }
            else if (hueR in 120.. 240) {
                ledRightR.power = 0.0
                ledRightG.power = 1-((hueR - 120).toDouble() / 120)
                ledRightB.power = (hueR - 120).toDouble() / 120
            }
            else if (hueR in 240..360) {
                ledRightR.power = ((hueR - 240).toDouble() / 120)
                ledRightG.power = 0.0
                ledRightB.power = 1-((hueR - 240).toDouble() / 120)
            }

            if (hueL in 0..120) {
                ledLeftR.power = 1-(hueL.toDouble() / 120)
                ledLeftG.power = hueL.toDouble() / 120
                ledLeftB.power = 0.0
            }
            else if (hueL in 120..240) {
                ledLeftR.power = 0.0
                ledLeftG.power = 1-((hueL - 120).toDouble() / 120)
                ledLeftB.power = (hueL - 120).toDouble() / 120
            }
            else if (hueL in 240..360) {
                ledLeftR.power = ((hueL - 240).toDouble() / 120)
                ledLeftG.power = 0.0
                ledLeftB.power = 1-((hueL - 240).toDouble() / 120)
            }

            if (hueR < 360) {
                hueR+=1;
            }
            else {
                hueR = 0
            }

            if (hueL < 360) {
                hueL+=1;
            }
            else {
                hueL = 0
            }
            sleep(1)
        }
    }

}