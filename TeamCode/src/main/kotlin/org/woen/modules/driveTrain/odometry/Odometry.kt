package org.woen.modules.driveTrain.odometry

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.camera.Camera
import org.woen.modules.driveTrain.HardwareGyro
import org.woen.modules.driveTrain.odometry.odometersOdometry.OdometersOdometry
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Angle
import org.woen.utils.units.Color
import org.woen.utils.units.Line
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicBoolean

data class OnOdometryUpdateEvent(
    val odometryOrientation: Orientation,
    val odometryRotateVelocity: Double, val odometryVelocity: Vec2
)

data class RequireOdometryEvent(
    var odometryOrientation: Orientation = Orientation.ZERO,
    var odometryRotateVelocity: Double = 0.0, var odometryVelocity: Vec2 = Vec2.ZERO
) : StoppingEvent

data class RequireRobotLocatedShootingArea(var isLocated: Boolean = false) : StoppingEvent

class RobotEnterShootingAreaEvent()

class RobotExitShootingAreaEvent()

class Odometry : IModule {
    private var _odometryJob: Job? = null

    private var _currentOrientation = Orientation.ZERO
    private var _currentPositionVelocity = Vec2.ZERO
    private var _currentRotationVelocity = 0.0

    private var _robotLocatedInShootingArea = AtomicBoolean(false)
    private var _oldRobotLocate = false

    private val _gyro = HardwareGyro()

    private val _gyroMutex = SmartMutex()
    private val _odometryMutex = SmartMutex()
    private val _mergePositionMutex = SmartMutex()

    private val _gyroFilter = ExponentialFilter(Configs.GYRO.GYRO_MERGE_COEF.get())
    private val _positionXFilter = ExponentialFilter(Configs.ODOMETRY.ODOMETRY_MERGE_COEF.get())
    private val _positionYFilter = ExponentialFilter(Configs.ODOMETRY.ODOMETRY_MERGE_COEF.get())

    private val _odometryHandler = OdometersOdometry()

    override suspend fun process() {
        _odometryJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
                return@launch

            _odometryMutex.smartLock {
                val odometryTick = _odometryHandler.update(_currentOrientation.angl)

                _currentOrientation.pos += odometryTick.deltaPos
                _currentOrientation.angl += odometryTick.deltaRotation

                _currentPositionVelocity = odometryTick.positionVelocity
                _currentRotationVelocity = odometryTick.rotationVelocity
            }

            fun checkToLocate(): Boolean {
                val halfSize = Configs.DRIVE_TRAIN.ROBOT_SIZE / 2.0

                val cornerLeftForward = _currentOrientation.pos + Vec2(-halfSize.x, halfSize.y)
                    .turn(_currentOrientation.angle)
                val cornerRightForward = _currentOrientation.pos + Vec2(halfSize.x, halfSize.y)
                    .turn(_currentOrientation.angle)
                val cornerRightBack = _currentOrientation.pos + Vec2(halfSize.x, -halfSize.y)
                    .turn(_currentOrientation.angle)
                val cornerLeftBack = _currentOrientation.pos + Vec2(-halfSize.x, -halfSize.y)
                    .turn(_currentOrientation.angle)

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

            _robotLocatedInShootingArea.set(locate)

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

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_gyro)

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            _odometryMutex.smartLock {
                it.drawRect(
                    _currentOrientation.pos,
                    Configs.DRIVE_TRAIN.ROBOT_SIZE,
                    _currentOrientation.angle,
                    if (_robotLocatedInShootingArea.get()) Color.GREEN else Color.RED
                )

                it.addData("orientation", _currentOrientation)
                it.addData("vel", _currentPositionVelocity)
                it.addData("rotation vel", _currentRotationVelocity)
            }
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(
            RequireOdometryEvent::class,
            {
                _odometryMutex.smartLock {
                    it.odometryOrientation = _currentOrientation
                    it.odometryVelocity = _currentPositionVelocity
                    it.odometryRotateVelocity = _currentRotationVelocity
                }
            })

        _gyro.gyroUpdateEvent += {
            _gyroMutex.smartLock {
                _odometryMutex.smartLock {
                    _currentOrientation.angl = Angle(
                        _gyroFilter.updateRaw(
                            _currentOrientation.angle,
                            (it - _currentOrientation.angl).angle
                        )
                    )
                }
            }
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _gyroMutex.smartLock {
                _gyroFilter.start()
            }

            _mergePositionMutex.smartLock {
                _positionXFilter.start()
                _positionYFilter.start()
            }

            if (HotRun.LAZY_INSTANCE.currentRunMode.get() == HotRun.RunMode.AUTO)
                _currentOrientation = HotRun.LAZY_INSTANCE.currentRunColor.get().startOrientation
        }

        Configs.ODOMETRY.ODOMETRY_MERGE_COEF.onSet += {
            _mergePositionMutex.smartLock {
                _positionXFilter.coef = it
                _positionYFilter.coef = it
            }
        }

        Camera.LAZY_INSTANCE.cameraPositionUpdateEvent += {
            _mergePositionMutex.smartLock {
                _odometryMutex.smartLock {
                    _currentOrientation.pos = Vec2(
                        _positionXFilter.updateRaw(
                            _currentOrientation.x,
                            it.x - _currentOrientation.x
                        ),
                        _positionYFilter.updateRaw(
                            _currentOrientation.y,
                            it.y - _currentOrientation.y
                        )
                    )
                }
            }
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequireRobotLocatedShootingArea::class, {
            it.isLocated = _robotLocatedInShootingArea.get()
        })
    }
}