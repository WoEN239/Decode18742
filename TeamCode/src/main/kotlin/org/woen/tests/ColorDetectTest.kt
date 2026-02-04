package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.util.ElapsedTime
import kotlin.math.max
import kotlin.math.min

@Config
internal object COLOR_DETECT_TEST{
    @JvmField
    var GREEN_THRESHOLD = 30.0

    @JvmField
    var MIN_PURPLE_H = 3.1

    @JvmField
    var MAX_PURPLE_H = 4.0
}

@TeleOp(group = "tests")
class ColorDetectTest : LinearOpMode() {
    override fun runOpMode() {
        val leftSensor = hardwareMap.get("leftColorSensor") as RevColorSensorV3
        val rightSensor = hardwareMap.get("rightColorSensor") as RevColorSensorV3

        leftSensor.gain = 4.0f
        rightSensor.gain = 4.0f

        val convert = 10240.0

        val deltaTime = ElapsedTime()

        waitForStart()
        resetRuntime()

        deltaTime.reset()

        while (opModeIsActive()) {
            val telem = FtcDashboard.getInstance().telemetry

            val leftColor = leftSensor.normalizedColors
            val rightColor = rightSensor.normalizedColors

            val leftR = leftColor.red * convert
            val leftG = leftColor.green * convert
            val leftB = leftColor.blue * convert

            val rightR = rightColor.red * convert
            val rightG = rightColor.green * convert
            val rightB = rightColor.blue * convert

            telem.addLine("left = ${String.format("%.1f", leftR)} ${String.format("%.1f", leftG)} ${String.format("%.1f", leftB)}")
            telem.addLine("left = ${String.format("%.1f", rightR)} ${String.format("%.1f", rightG)} ${String.format("%.1f", rightB)}")

            val leftGreen = (leftG - max(leftR, leftB)) > COLOR_DETECT_TEST.GREEN_THRESHOLD
            val rightGreen = (rightG - max(rightR, rightB)) > COLOR_DETECT_TEST.GREEN_THRESHOLD

            val combinedGreen = leftGreen || rightGreen

            telem.addLine("left green $leftGreen")
            telem.addLine("right green $rightGreen")

            val maxRightC = max(max(rightR, rightG), rightB)
            val minRightC = min(min(rightR, rightG), rightB)

            val difRight = maxRightC - minRightC

            val rightH =
                when (maxRightC) {
                    rightR -> ((rightG - rightB) / difRight)
                    rightG -> (2.0 + (rightB - rightR) / difRight)
                    else -> (4.0 + (rightR - rightG) / difRight)
                }

            telem.addLine("hueRight ${String.format("%.1f", rightH)}")

            val maxLeftC = max(max(leftR, leftG), leftB)
            val minLeftC = min(min(leftR, leftG), leftB)

            val difLeft = maxLeftC - minLeftC

            val leftH =
                when (maxLeftC) {
                    leftR -> ((leftG - leftB) / difLeft)
                    leftG -> (2.0 + (leftB - leftR) / difLeft)
                    else -> (4.0 + (leftR - leftG) / difLeft)
                }

            telem.addLine("hueLeft ${String.format("%.1f", leftH)}")

            val leftPurple = leftH in (COLOR_DETECT_TEST.MIN_PURPLE_H..COLOR_DETECT_TEST.MAX_PURPLE_H)
            val rightPurple = rightH in (COLOR_DETECT_TEST.MIN_PURPLE_H..COLOR_DETECT_TEST.MAX_PURPLE_H)

            val combinedPurple = leftPurple || rightPurple

            telem.addLine("left purple $leftPurple")
            telem.addLine("right purple $rightPurple")

            telem.addLine("combined green $combinedGreen")
            telem.addLine("combined purple $combinedPurple")

            telem.addLine("updates ${String.format("%.1f", 1.0 / deltaTime.seconds())}")

            deltaTime.reset()

            telem.update()
        }
    }
}