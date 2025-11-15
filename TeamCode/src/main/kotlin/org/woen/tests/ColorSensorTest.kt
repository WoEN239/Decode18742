package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor
import com.qualcomm.hardware.ams.AMSColorSensor
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.util.ElapsedTime
import woen239.FixColorSensor.fixSensor

@TeleOp
class ColorSensorTest: LinearOpMode() {
    override fun runOpMode() {
        var sensor = fixSensor(hardwareMap.get("color1") as AdafruitI2cColorSensor)

        val maximumReading =
            (65535.coerceAtMost(1024 * (256 - AMSColorSensor.Parameters.atimeFromMs(24f)))).toDouble()

        waitForStart()
        resetRuntime()

        val deltaTime = ElapsedTime()

        while(opModeIsActive()){
            val telem = FtcDashboard.getInstance().telemetry

            val color = sensor.normalizedColors

            val red = color.red * maximumReading
            val green = color.green * maximumReading
            val blue = color.blue * maximumReading

            telem.addLine("${red.toInt()} ${green.toInt()} ${blue.toInt()}")
            telem.addData("updates", 1.0 / deltaTime.seconds())
            telem.addLine(maximumReading.toString())

            telem.update()
            deltaTime.reset()
        }
    }
}