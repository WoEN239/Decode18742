package org.woen.modules.runner.segment

import com.acmerobotics.roadrunner.AngularVelConstraint
import com.acmerobotics.roadrunner.MinVelConstraint
import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.ProfileAccelConstraint
import com.acmerobotics.roadrunner.ProfileParams
import com.acmerobotics.roadrunner.TrajectoryBuilder
import com.acmerobotics.roadrunner.TrajectoryBuilderParams
import com.acmerobotics.roadrunner.TranslationalVelConstraint
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.modules.driveTrain.RequireOdometryEvent
import org.woen.modules.driveTrain.SetDriveTargetVelocityEvent
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.StoppingEvent
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.process.Process
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

data class RunSegmentEvent(val segment: ISegment, val process: Process = Process())

data class RequireRRBuilderEvent(
    val startOrientation: Orientation? = null,
    var trajectoryBuilder: TrajectoryBuilder? = null
) : StoppingEvent

class SegmentsRunner : IModule {
    private var _runnerJob: Job? = null

    private var _segmentsQueue = ArrayDeque<Pair<ISegment, Process>>()

    private val _segmentTimer = ElapsedTime()
    private val _segmentTimerMutex = SmartMutex()

    private val _segmentsMutex = SmartMutex()

    private var _targetOrientation = Orientation.ZERO
    private var _targetTranslateVelocity = Vec2.ZERO
    private var _targetRotateVelocity = 0.0

    override suspend fun process() {
        if (HotRun.LAZY_INSTANCE.currentRunMode != HotRun.RunMode.AUTO)
            return

        _runnerJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
            val odometry = ThreadedEventBus.LAZY_INSTANCE.invoke(RequireOdometryEvent())

            val orientationErr = _targetOrientation - odometry.odometryOrientation

            ThreadedEventBus.LAZY_INSTANCE.invoke(
                SetDriveTargetVelocityEvent(
                    (orientationErr.pos * Vec2(
                        Configs.ROAD_RUNNER.ROAD_RUNNER_POS_X_P,
                        Configs.ROAD_RUNNER.ROAD_RUNNER_POS_Y_P
                    ) + _targetTranslateVelocity +
                            (_targetTranslateVelocity - odometry.odometryVelocity) *
                            Vec2(
                                Configs.ROAD_RUNNER.ROAD_RUNNER_VEL_X_P,
                                Configs.ROAD_RUNNER.ROAD_RUNNER_VEL_Y_P
                            )).turn(-odometry.odometryOrientation.angle),
                    orientationErr.angl.angle * Configs.ROAD_RUNNER.ROAD_RUNNER_POS_H_P +
                            _targetRotateVelocity + (_targetRotateVelocity - odometry.odometryRotateVelocity) * Configs.ROAD_RUNNER.ROAD_RUNNER_VEL_H_P
                )
            )

            val currentTime = _segmentTimerMutex.smartLock {
                _segmentTimer.seconds()
            }

            _segmentsMutex.smartLock {
                if (_segmentsQueue.isEmpty())
                    return@smartLock

                val currentSegment = _segmentsQueue.first().first

                if (currentSegment.isEnd(currentTime)) {
                    _segmentsQueue.removeFirst().second.close()

                    _segmentTimerMutex.smartLock {
                        _segmentTimer.reset()
                    }

                    if (_segmentsQueue.isEmpty()) {
                        _targetTranslateVelocity = Vec2.ZERO
                        _targetRotateVelocity = 0.0
                    }

                    return@smartLock
                }

                _targetOrientation = currentSegment.targetOrientation(currentTime)
                _targetTranslateVelocity = currentSegment.translateVelocity(currentTime)
                _targetRotateVelocity = currentSegment.rotateVelocity(currentTime)
            }
        }
    }

    override val isBusy: Boolean
        get() = _runnerJob != null && !_runnerJob!!.isCompleted

    override fun opModeStart() {
        _segmentTimerMutex.smartLock {
            _segmentTimer.reset()
        }

        _targetOrientation = HotRun.LAZY_INSTANCE.currentStartPosition.startOrientation
    }

    override fun opModeStop() {

    }

    override fun dispose() {
        _runnerJob?.cancel()
    }

    constructor() {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(RunSegmentEvent::class, {
            _segmentsMutex.smartLock {
                if (_segmentsQueue.isEmpty())
                    _segmentTimerMutex.smartLock {
                        _segmentTimer.reset()
                    }

                _segmentsQueue.addLast(Pair(it.segment, it.process))
            }
        })

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("target", _targetOrientation)
        }

        ThreadedEventBus.LAZY_INSTANCE.subscribe(RequireRRBuilderEvent::class, {
            val startOrientation: Orientation

            if (it.startOrientation == null) {
                val isSegmentsEmpty = _segmentsMutex.smartLock {
                    _segmentsQueue.isEmpty()
                }

                startOrientation = if (isSegmentsEmpty) {
                    _targetOrientation
                } else {
                    _segmentsMutex.smartLock {
                        val lastSegment = _segmentsQueue.last().first

                        lastSegment.targetOrientation(lastSegment.duration())
                    }
                }
            } else
                startOrientation = it.startOrientation

            it.trajectoryBuilder = TrajectoryBuilder(
                TrajectoryBuilderParams(1e-6, ProfileParams(0.1, 0.1, 0.01)),
                Pose2d(startOrientation.x, startOrientation.y, startOrientation.angl.angle), 0.0,
                MinVelConstraint(
                    listOf(
                        TranslationalVelConstraint(Configs.ROAD_RUNNER.ROAD_RUNNER_TRANSLATE_VELOCITY),
                        AngularVelConstraint(Configs.ROAD_RUNNER.ROAD_RUNNER_ROTATE_VELOCITY)
                    )
                ),
                ProfileAccelConstraint(
                    Configs.ROAD_RUNNER.ROAD_RUNNER_MIN_TRANSLATION_ACCEL,
                    Configs.ROAD_RUNNER.ROAD_RUNNER_MAX_TRANSLATION_ACCEL
                )
            )
        })
    }
}