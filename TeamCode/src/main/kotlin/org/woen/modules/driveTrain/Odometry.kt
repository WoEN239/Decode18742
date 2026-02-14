package org.woen.modules.driveTrain

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.camera.CameraUpdateEvent
import org.woen.telemetry.ThreadedTelemetry
import org.woen.telemetry.configs.Configs
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.units.Angle
import org.woen.utils.units.Color
import org.woen.utils.units.Line
import org.woen.utils.units.Orientation
import org.woen.utils.units.Triangle
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicReference

data class RequireOdometryEvent(
    var odometryOrientation: Orientation = Orientation.ZERO,
    var odometryRotateVelocity: Double = 0.0, var odometryVelocity: Vec2 = Vec2.ZERO
) : StoppingEvent

data class RequireRobotLocatedShootingArea(var isLocated: Boolean = false) : StoppingEvent

class RobotEnterShootingAreaEvent()

class RobotExitShootingAreaEvent()

class Odometry : IModule {
    private var _odometryJob: Job? = null

    private var _robotLocatedInShootingArea = false
    private var _oldRobotLocate = false

    private val _hardwareOdometry = HardwareOdometry()

    private var _currentOrientation = AtomicReference(HotRun.LAZY_INSTANCE.currentStartPosition.startOrientation)
    private var _oldOrientation = HotRun.LAZY_INSTANCE.currentStartPosition.startOrientation

    private val _xFilter = ExponentialFilter(Configs.ODOMETRY.MERGE_X_K.get())
    private val _yFilter = ExponentialFilter(Configs.ODOMETRY.MERGE_Y_K.get())
    private val _hFilter = ExponentialFilter(Configs.ODOMETRY.MERGE_H_K.get())

    override suspend fun process() {
        _odometryJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val odometryOrientation = _hardwareOdometry.currentOrientation

            val orientation = _currentOrientation.get().clone()

            _currentOrientation.set(orientation + (odometryOrientation - _oldOrientation))

            _oldOrientation = odometryOrientation

            fun checkToLocate(triangle: Triangle): Boolean {
                val halfSize = Configs.DRIVE_TRAIN.ROBOT_SIZE / 2.0

                val cornerLeftForward =
                    _currentOrientation.get().pos + Vec2(-halfSize.x, halfSize.y)
                        .turn(_currentOrientation.get().angle)
                val cornerRightForward =
                    _currentOrientation.get().pos + Vec2(halfSize.x, halfSize.y)
                        .turn(_currentOrientation.get().angle)
                val cornerRightBack = _currentOrientation.get().pos + Vec2(halfSize.x, -halfSize.y)
                    .turn(_currentOrientation.get().angle)
                val cornerLeftBack = _currentOrientation.get().pos + Vec2(-halfSize.x, -halfSize.y)
                    .turn(_currentOrientation.get().angle)

                val robotPoints = arrayOf(
                    cornerLeftBack, cornerRightBack,
                    cornerRightForward, cornerLeftForward
                )

                val robotLines = arrayOf(
                    Line(cornerLeftForward, cornerRightForward),
                    Line(cornerRightBack, cornerRightForward),
                    Line(cornerRightBack, cornerLeftBack),
                    Line(cornerLeftForward, cornerLeftBack)
                )

                for (shootLine in triangle.lines) {
                    for (l in robotLines) {
                        if (!l.isIntersects(shootLine))
                            continue

                        val intersects = l.getIntersects(shootLine)

                        if (l.isPointOnLine(intersects) && shootLine.isPointOnLine(intersects))
                            return true
                    }

                    for (robotPoint in robotPoints)
                        if (triangle.isPointLocated(robotPoint))
                            return true
                }

                return false
            }

            val shortLocate = checkToLocate(Configs.DRIVE_TRAIN.SHOOT_SHORT_TRIANGLE)
            val longLocate = checkToLocate(Configs.DRIVE_TRAIN.SHOOT_LONG_TRIANGLE)

            val locate = shortLocate || longLocate

            _robotLocatedInShootingArea = locate

            if (_oldRobotLocate != locate) {
                if (locate)
                    ThreadedEventBus.LAZY_INSTANCE.invoke(RobotEnterShootingAreaEvent())
                else
                    ThreadedEventBus.LAZY_INSTANCE.invoke(RobotExitShootingAreaEvent())
            }

            _oldRobotLocate = locate
        }
    }

    override val isBusy: Boolean
        get() = _odometryJob != null && !_odometryJob!!.isCompleted

    override fun opModeStart() {
        _xFilter.start()
        _yFilter.start()
        _hFilter.start()
    }

    override fun opModeStop() {

    }

    override fun dispose() {
        _odometryJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareOdometry)

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            val orientation = _currentOrientation.get().clone()

            it.drawRect(
                orientation.pos,
                Configs.DRIVE_TRAIN.ROBOT_SIZE,
                orientation.angle,
                if (_robotLocatedInShootingArea) Color.GREEN else Color.RED
            )

            it.addLine("robot position: $orientation")
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            RequireOdometryEvent::class,
            {
                it.odometryOrientation = _currentOrientation.get()
                it.odometryVelocity = _hardwareOdometry.velocity
                it.odometryRotateVelocity = _hardwareOdometry.headingVelocity
            })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequireRobotLocatedShootingArea::class, {
            it.isLocated = _robotLocatedInShootingArea
        })

        ThreadedGamepad.LAZY_INSTANCE.addGamepad1Listener(
            ThreadedGamepad.createClickDownListener(
                { it.circle },
                {
                    _hardwareOdometry.reset()
                })
        )

        Configs.ODOMETRY.MERGE_X_K.onSet += {
            _xFilter.coef = it
        }

        Configs.ODOMETRY.MERGE_Y_K.onSet += {
            _yFilter.coef = it
        }

        Configs.ODOMETRY.MERGE_H_K.onSet += {
            _hFilter.coef = it
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(CameraUpdateEvent::class, {
            val orientation = _currentOrientation.get().clone()

            _currentOrientation.set(
                Orientation(
                    Vec2(
                        _xFilter.updateRaw(
                            orientation.x,
                            it.orientation.x - orientation.x
                        ), _yFilter.updateRaw(orientation.y, it.orientation.y - orientation.y)
                    ),
                    Angle(
                        _hFilter.updateRaw(
                            orientation.angle,
                            (it.orientation.angl - orientation.angl).angle
                        )
                    )
                )
            )
        })
    }
}