package org.woen.modules.camera

import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import kotlinx.coroutines.DisposableHandle
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.robotcore.external.matrices.VectorF
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata
import org.firstinspires.ftc.vision.apriltag.AprilTagPoseRaw
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import org.woen.hotRun.HotRun
import org.woen.modules.turret.Pattern
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.ThreadManager
import org.woen.utils.events.SimpleEvent
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Vec2
import kotlin.concurrent.thread

class Camera private constructor() : DisposableHandle {
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
        AprilTagProcessor.Builder().setOutputUnits(DistanceUnit.CM, AngleUnit.DEGREES)
            .setDrawAxes(true).build()

    var currentPattern: Pattern? = null
        private set

    val cameraPositionUpdateEvent = SimpleEvent<Vec2>()

    private val _thread = ThreadManager.Companion.LAZY_INSTANCE.register(thread {
        while (!Thread.currentThread().isInterrupted && ThreadedConfigs.CAMERA_ENABLE.get()) {
            val detections = _aprilProcessor.freshDetections

            if (detections == null) {
                Thread.sleep(5)
                continue
            }

            var suitableDetections = 0

            var sum = Vec2.Companion.ZERO

            for (detection in detections) {
                val pattern = Pattern.Companion.patterns.find { it.cameraTagId == detection.id }

                if (pattern != null)
                    currentPattern = pattern
                else if (detection.rawPose != null &&
                    detection.decisionMargin < ThreadedConfigs.CAMERA_ACCURACY.get()
                ) {
                    val rawTagPose: AprilTagPoseRaw = detection.rawPose
                    var rawTagPoseVector: VectorF? = VectorF(
                        rawTagPose.x.toFloat(), rawTagPose.y.toFloat(), rawTagPose.z.toFloat()
                    )
                    val rawTagRotation = rawTagPose.R
                    val metadata: AprilTagMetadata = detection.metadata
                    val fieldTagPos =
                        metadata.fieldPosition.multiplied(DistanceUnit.mmPerInch.toFloat() / 10f)
                    val fieldTagQ = metadata.fieldOrientation
                    rawTagPoseVector = rawTagRotation.inverted().multiplied(rawTagPoseVector)
                    val rotatedPosVector = fieldTagQ.applyToVector(rawTagPoseVector)

                    val dist = Vec2(
                        rotatedPosVector.get(0).toDouble(),
                        rotatedPosVector.get(1).toDouble()
                    ).length()

                    val fieldCameraPos = fieldTagPos.subtracted(rotatedPosVector)

                    sum += Vec2(fieldCameraPos.get(0).toDouble(), fieldCameraPos.get(1).toDouble())

                    suitableDetections++
                }
            }

            if (suitableDetections != 0)
                cameraPositionUpdateEvent.invoke(sum / suitableDetections.toDouble())
        }
    })

    override fun dispose() {
        _visionPortal?.close()
    }

    init {
        if (ThreadedConfigs.CAMERA_ENABLE.get()) {
            val hardwareMap =
                OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).hardwareMap

            _visionPortal =
                VisionPortal.Builder().setCamera(hardwareMap.get("Webcam 1") as WebcamName)
                    .addProcessor(_aprilProcessor).build()

            HotRun.Companion.LAZY_INSTANCE.opModeInitEvent += {
                _thread.start()
            }

            HotRun.Companion.LAZY_INSTANCE.opModeStopEvent += {
                _thread.interrupt()
            }
        }
    }
}