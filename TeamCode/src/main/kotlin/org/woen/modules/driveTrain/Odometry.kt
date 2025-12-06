package org.woen.modules.driveTrain

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.units.Color
import org.woen.utils.units.Line
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

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

    override suspend fun process() {
        _odometryJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val currentOrientation = _hardwareOdometry.currentOrientation

            fun checkToLocate(): Boolean {
                val halfSize = Configs.DRIVE_TRAIN.ROBOT_SIZE / 2.0

                val cornerLeftForward = currentOrientation.pos + Vec2(-halfSize.x, halfSize.y)
                    .turn(currentOrientation.angle)
                val cornerRightForward = currentOrientation.pos + Vec2(halfSize.x, halfSize.y)
                    .turn(currentOrientation.angle)
                val cornerRightBack = currentOrientation.pos + Vec2(halfSize.x, -halfSize.y)
                    .turn(currentOrientation.angle)
                val cornerLeftBack = currentOrientation.pos + Vec2(-halfSize.x, -halfSize.y)
                    .turn(currentOrientation.angle)

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

                for (shootTriangle in Configs.DRIVE_TRAIN.SHOOT_TRIANGLES) {
                    for (shootLine in shootTriangle.lines) {
                        for (l in robotLines) {
                            if (!l.isIntersects(shootLine))
                                continue

                            val intersects = l.getIntersects(shootLine)

                            if (l.isPointOnLine(intersects) && shootLine.isPointOnLine(intersects))
                                return true
                        }
                    }

                    for (robotPoint in robotPoints)
                        if (shootTriangle.isPointLocated(robotPoint))
                            return true
                }

                return false
            }

            val locate = checkToLocate()

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

    override fun dispose() {
        _odometryJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareOdometry)

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.drawRect(
                _hardwareOdometry.currentOrientation.pos,
                Configs.DRIVE_TRAIN.ROBOT_SIZE,
                _hardwareOdometry.currentOrientation.angle,
                if (_robotLocatedInShootingArea) Color.GREEN else Color.RED
            )
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            RequireOdometryEvent::class,
            {
                it.odometryOrientation = _hardwareOdometry.currentOrientation
                it.odometryVelocity = _hardwareOdometry.velocity
                it.odometryRotateVelocity = _hardwareOdometry.headingVelocity
            })

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequireRobotLocatedShootingArea::class, {
            it.isLocated = _robotLocatedInShootingArea
        })
    }
}