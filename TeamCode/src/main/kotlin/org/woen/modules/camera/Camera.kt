package org.woen.modules.camera

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import kotlinx.coroutines.DisposableHandle
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.robotcore.external.matrices.VectorF
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata
import org.firstinspires.ftc.vision.apriltag.AprilTagPoseRaw
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import org.woen.hotRun.HotRun
import org.woen.modules.scoringSystem.turret.Pattern
import org.woen.telemetry.Configs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.events.SimpleEvent
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

data class OnPatternDetectedEvent(val pattern: Pattern)

class Camera : DisposableHandle {
    companion object {
        private var _nullableInstance: Camera? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: Camera
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null)
                    _nullableInstance = Camera()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance?.dispose()
                _nullableInstance = null
            }
        }
    }

    private var _visionPortal: VisionPortal? = null

    private val _aprilProcessor: AprilTagProcessor =
        AprilTagProcessor.Builder().setOutputUnits(DistanceUnit.METER, AngleUnit.RADIANS)
            .setCameraPose(
                Position(
                    DistanceUnit.METER,
                    Configs.CAMERA.CAMERA_POSITION.x,
                    Configs.CAMERA.CAMERA_POSITION.y,
                    Configs.CAMERA.CAMERA_HEIGHT,
                    0
                ),
                YawPitchRollAngles(AngleUnit.DEGREES, 0.0, 0.0, -90.0, 0)
            )
            .setDrawAxes(true).build()

    var currentPattern: AtomicReference<Pattern?> = AtomicReference()

    val cameraPositionUpdateEvent = SimpleEvent<Vec2>()

    private val _thread = ThreadManager.Companion.LAZY_INSTANCE.register(thread(start = true) {
        while (!Thread.currentThread().isInterrupted && Configs.CAMERA.CAMERA_ENABLE) {
            if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN) {
                Thread.sleep(5)
                continue
            }

            val detections = _aprilProcessor.freshDetections

            if (detections == null) {
                Thread.sleep(5)
                continue
            }

            var suitableDetections = 0

            var sum = Vec2.Companion.ZERO

            for (detection in detections) {
                var pattern: Pattern? = null

                if (currentPattern.get() == null)
                    pattern = Pattern.patterns.find { it.cameraTagId == detection.id }

                if (pattern != null) {
                    currentPattern.set(pattern)

                    ThreadedEventBus.LAZY_INSTANCE.invoke(OnPatternDetectedEvent(pattern))
                } else if (detection.rawPose != null &&
                    detection.decisionMargin < Configs.CAMERA.CAMERA_ACCURACY
                ) {
                    val rawTagPose: AprilTagPoseRaw = detection.rawPose
                    var rawTagPoseVector: VectorF? = VectorF(
                        rawTagPose.x.toFloat(), rawTagPose.y.toFloat(), rawTagPose.z.toFloat()
                    )
                    val rawTagRotation = rawTagPose.R
                    val metadata: AprilTagMetadata = detection.metadata
                    val fieldTagPos =
                        metadata.fieldPosition.multiplied(DistanceUnit.mmPerInch.toFloat() / 1000f)
                    val fieldTagQ = metadata.fieldOrientation
                    rawTagPoseVector = rawTagRotation.inverted().multiplied(rawTagPoseVector)
                    val rotatedPosVector = fieldTagQ.applyToVector(rawTagPoseVector)

                    val dist = Vec2(
                        rotatedPosVector.get(0).toDouble(),
                        rotatedPosVector.get(1).toDouble()
                    ).length()

                    if (dist < Configs.CAMERA.CAMERA_TRIGGER_DISTANCE) {
                        val fieldCameraPos = fieldTagPos.subtracted(rotatedPosVector)

                        sum += Vec2(
                            fieldCameraPos.get(0).toDouble(),
                            fieldCameraPos.get(1).toDouble()
                        )

                        suitableDetections++
                    }
                }
            }

            if (suitableDetections != 0)
                cameraPositionUpdateEvent.invoke(sum / suitableDetections.toDouble())
        }
    })

    override fun dispose() {
        _visionPortal?.close()
    }

    private constructor() {
        if (Configs.CAMERA.CAMERA_ENABLE) {
            val hardwareMap =
                OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).hardwareMap

            val helpCameraProcessor = HelpCameraProcessor()

            _visionPortal =
                VisionPortal.Builder().setCamera(hardwareMap.get("Webcam 1") as WebcamName)
                    .addProcessors(_aprilProcessor, helpCameraProcessor).build()

            HotRun.LAZY_INSTANCE.opModeInitEvent += {
                FtcDashboard.getInstance().startCameraStream(helpCameraProcessor, 10.0)
            }

            HotRun.LAZY_INSTANCE.opModeStopEvent += {
                FtcDashboard.getInstance().stopCameraStream()
            }
        }
    }
}