package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import org.woen.telemetry.configs.Configs

@TeleOp(group = "tests")
class PinpointTest : LinearOpMode() {
    override fun runOpMode() {
        val pinpoint = hardwareMap.get("odometry") as GoBildaPinpointDriver
        pinpoint.resetPosAndIMU()
        pinpoint.recalibrateIMU()

        pinpoint.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
        pinpoint.setEncoderDirections(
            GoBildaPinpointDriver.EncoderDirection.FORWARD,
            GoBildaPinpointDriver.EncoderDirection.FORWARD
        )
        pinpoint.setOffsets(Configs.ODOMETRY.X_ODOMETER_POSITION, Configs.ODOMETRY.Y_ODOMETER_POSITION,
            DistanceUnit.METER)

        waitForStart()
        resetRuntime()

        while (opModeIsActive()) {
            val telem = FtcDashboard.getInstance().telemetry

            pinpoint.update()

            val pos = pinpoint.position

            telem.addData("posX", pos.getX(DistanceUnit.METER))
            telem.addData("posY", pos.getY(DistanceUnit.METER))
            telem.addData("rotate", pos.getHeading(AngleUnit.DEGREES))
            telem.addData("velX", pinpoint.getVelX(DistanceUnit.METER))
            telem.addData("velY", pinpoint.getVelY(DistanceUnit.METER))
            telem.addData("velH", pinpoint.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS))
            telem.addData("xPos", pinpoint.encoderX)
            telem.addData("yPos", pinpoint.encoderY)
            telem.addData("hz", pinpoint.frequency)

            telem.update()
        }
    }
}