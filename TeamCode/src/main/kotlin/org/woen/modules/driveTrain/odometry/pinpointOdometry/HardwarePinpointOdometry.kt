package org.woen.modules.driveTrain.odometry.pinpointOdometry

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import woen239.odometry.OdometryComputer
import java.util.concurrent.atomic.AtomicReference

class HardwarePinpointOdometry(private val _computerName: String) : IHardwareDevice {
    private lateinit var _computer: OdometryComputer

    var orientation = AtomicReference(HotRun.LAZY_INSTANCE.currentRunColor.get().startOrientation)
    var positionVelocity = AtomicReference(Vec2.ZERO)
    var headingVelocity = AtomicReference(0.0)

    private val _velXFilter = ExponentialFilter(Configs.ODOMETRY.POSITION_VELOCITY_K.get())
    private val _velYFilter = ExponentialFilter(Configs.ODOMETRY.POSITION_VELOCITY_K.get())

    private val _velHFilter = ExponentialFilter(Configs.ODOMETRY.HEADING_VELOCITY_K.get())

    private var _oldOrientation = HotRun.LAZY_INSTANCE.currentRunColor.get().startOrientation

    private val _deltaTime = ElapsedTime()

    override fun update() {
        _computer.update()

        val pose = _computer.position

        val currentOrientation = Orientation(
            Vec2(
                pose.getX(DistanceUnit.METER),
                pose.getY(DistanceUnit.METER)
            ), Angle(pose.getHeading(AngleUnit.RADIANS))
        ) + HotRun.LAZY_INSTANCE.currentRunColor.get().startOrientation

        orientation.set(currentOrientation)

        val deltaOrientation = currentOrientation - _oldOrientation

        val rawXVelocity = deltaOrientation.x / _deltaTime.seconds()
        val rawYVelocity = deltaOrientation.y / _deltaTime.seconds()
        val rawHVelocity = deltaOrientation.angle / _deltaTime.seconds()

        val oldXVelocity = positionVelocity.get().x
        val oldYVelocity = positionVelocity.get().y
        val oldHVelocity = headingVelocity.get()

        positionVelocity.set(
            Vec2(
                _velXFilter.updateRaw(oldXVelocity, rawXVelocity - oldXVelocity),
                _velYFilter.updateRaw(oldYVelocity, rawYVelocity - oldYVelocity)
            ).turn(-currentOrientation.angle)
        )

        headingVelocity.set(_velHFilter.updateRaw(oldHVelocity, rawHVelocity - oldHVelocity))

        _oldOrientation = currentOrientation

        _deltaTime.reset()
    }

    override fun init(hardwareMap: HardwareMap) {
        _computer = hardwareMap.get(_computerName) as OdometryComputer

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _computer.setEncoderResolution(OdometryComputer.GoBildaOdometryPods.goBILDA_4_BAR_POD)
            _computer.setEncoderDirections(
                OdometryComputer.EncoderDirection.FORWARD,
                OdometryComputer.EncoderDirection.FORWARD
            )
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            if (HotRun.LAZY_INSTANCE.currentRunMode.get() == HotRun.RunMode.AUTO)
                _computer.resetPosAndIMU()

            _deltaTime.reset()
        }

        Configs.ODOMETRY.POSITION_VELOCITY_K.onSet += {
            _velXFilter.coef = it
            _velYFilter.coef = it
        }

        Configs.ODOMETRY.HEADING_VELOCITY_K.onSet += {
            _velHFilter.coef = it
        }
    }

    override fun dispose() {

    }
}