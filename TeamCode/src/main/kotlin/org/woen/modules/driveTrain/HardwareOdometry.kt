package org.woen.modules.driveTrain

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.RobotLog
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import woen239.odometry.OdometryComputer
import kotlin.math.PI

class HardwareOdometry : IHardwareDevice {
    private lateinit var _computer: GoBildaPinpointDriver
    var currentOrientation = Orientation()
    var velocity = Vec2.ZERO
    var headingVelocity = 0.0

    override fun update() {
        _computer.update()

        val pos = _computer.position

        currentOrientation = Orientation(
            Vec2(
                pos.getX(DistanceUnit.METER),
                pos.getY(DistanceUnit.METER)
            ).turn(HotRun.LAZY_INSTANCE.currentStartPosition.startOrientation.angle),
            Angle(pos.getHeading(AngleUnit.RADIANS))
        ) + HotRun.LAZY_INSTANCE.currentStartPosition.startOrientation

        velocity =
            Vec2(_computer.getVelX(DistanceUnit.METER), _computer.getVelY(DistanceUnit.METER)).turn(
                HotRun.LAZY_INSTANCE.currentStartPosition.startOrientation.angle
            ).turn(-currentOrientation.angle)
        headingVelocity = _computer.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS)
    }

    override fun init(hardwareMap: HardwareMap) {
        _computer = hardwareMap.get("odometry") as GoBildaPinpointDriver

        currentOrientation = HotRun.LAZY_INSTANCE.currentStartPosition.startOrientation

        _computer.recalibrateIMU()
        _computer.resetPosAndIMU()

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _computer.setOffsets(
                Configs.ODOMETRY.X_ODOMETER_POSITION, Configs.ODOMETRY.Y_ODOMETER_POSITION,
                DistanceUnit.METER
            )

            _computer.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
            _computer.setEncoderDirections(
                GoBildaPinpointDriver.EncoderDirection.FORWARD,
                GoBildaPinpointDriver.EncoderDirection.FORWARD
            )
        }

    }

    override fun opModeStart() {

    }

    override fun opModeStop() {

    }

    override fun dispose() {

    }

    fun reset(){
        _computer.resetPosAndIMU()
    }
}