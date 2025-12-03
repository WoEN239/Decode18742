package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import woen239.odometry.OdometryComputer

@TeleOp
class PinpointTest: LinearOpMode() {
    override fun runOpMode() {
        val pinpoint = hardwareMap.get("odometry") as OdometryComputer
        pinpoint.resetPosAndIMU()
        pinpoint.recalibrateIMU()

        pinpoint.setEncoderResolution(OdometryComputer.GoBildaOdometryPods.goBILDA_4_BAR_POD)
        pinpoint.setEncoderDirections(
            OdometryComputer.EncoderDirection.FORWARD,
            OdometryComputer.EncoderDirection.FORWARD
        )

        waitForStart()
        resetRuntime()

        while (opModeIsActive()){
            val telem = FtcDashboard.getInstance().telemetry

            pinpoint.update()

            val pos = pinpoint.position

            telem.addData("posX", pos.getX(DistanceUnit.METER))
            telem.addData("posY", pos.getY(DistanceUnit.METER))
            telem.addData("rotate", pos.getHeading(AngleUnit.DEGREES))

            telem.update()
        }
    }
}