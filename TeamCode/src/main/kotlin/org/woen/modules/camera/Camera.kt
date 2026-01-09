package org.woen.modules.camera

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.DisposableHandle
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import org.woen.hotRun.HotRun
import org.woen.modules.scoringSystem.turret.Pattern
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
import kotlin.concurrent.thread

data class OnPatternDetectedEvent(val pattern: Pattern)
class CloseCameraEvent()

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

    private var _aprilProcessor: AprilTagProcessor? = null

    var currentPattern: Pattern? = null

    private val _thread =
        ThreadManager.LAZY_INSTANCE.register(thread(start = true, name = "Camera thread") {
            while (!Thread.currentThread().isInterrupted && Configs.CAMERA.CAMERA_ENABLE) {
                if (HotRun.LAZY_INSTANCE.currentRunState != HotRun.RunState.RUN ||
                    _aprilProcessor == null ||
                    _visionPortal?.cameraState != VisionPortal.CameraState.STREAMING
                ) {
                    if (_visionPortal?.cameraState == VisionPortal.CameraState.ERROR) {
                        reCreateCamera()
                        ThreadedTelemetry.LAZY_INSTANCE.log("camera restarted cause: err")
                    }

                    Thread.sleep(5)
                    continue
                }

                val detections = _aprilProcessor?.freshDetections

                if (detections == null) {
                    Thread.sleep(5)
                    continue
                }

                if (currentPattern == null)
                    for (i in detections) {
                        currentPattern = Pattern.patterns.find { it.cameraTagId == i.id }

                        if (currentPattern != null) {
                            ThreadedEventBus.LAZY_INSTANCE.invoke(
                                OnPatternDetectedEvent(
                                    currentPattern!!
                                )
                            )

                            ThreadedTelemetry.LAZY_INSTANCE.log("motiv detected: " +
                                    "${currentPattern!!.subsequence[0]} ${currentPattern!!.subsequence[1]} ${currentPattern!!.subsequence[2]}")
                        }
                    }
            }
        })

    fun reCreateCamera() {
        if (_visionPortal?.cameraState == VisionPortal.CameraState.STREAMING) {
            FtcDashboard.getInstance().stopCameraStream()
            _visionPortal?.close()

            while (_visionPortal?.cameraState != VisionPortal.CameraState.CAMERA_DEVICE_CLOSED)
                Thread.sleep(5)
        }

        val hardwareMap =
            OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).hardwareMap

        _aprilProcessor =
            AprilTagProcessor.Builder()
                .setDrawAxes(true)
                .setDrawTagID(true)
                .setDrawTagOutline(true)
                .setDrawCubeProjection(true)
                .setDrawTagID(true)
                .setNumThreads(4)
                .setOutputUnits(DistanceUnit.METER, AngleUnit.RADIANS)
                .build()

        val dashboardCameraProcessor = DashboardCameraProcessor()

        _visionPortal =
            VisionPortal.Builder().setCamera(hardwareMap.get("Webcam 1") as WebcamName)
                .addProcessors(_aprilProcessor, dashboardCameraProcessor).build()

        FtcDashboard.getInstance().startCameraStream(
            dashboardCameraProcessor,
            Configs.TELEMETRY.TELEMETRY_UPDATE_HZ.get().toDouble()
        )
    }

    override fun dispose() {

    }

    private constructor() {
        if (Configs.CAMERA.CAMERA_ENABLE) {
            HotRun.LAZY_INSTANCE.opModeInitEvent += {
                reCreateCamera()
            }

            ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
                _visionPortal?.let { it1 ->
                    it.addData("camera fps", it1.fps.toString())
                }
            }

            HotRun.LAZY_INSTANCE.opModeStopEvent += {
                FtcDashboard.getInstance().stopCameraStream()
                _visionPortal?.close()
            }

            ThreadedEventBus.LAZY_INSTANCE.subscribe(CloseCameraEvent::class, {
                _thread.interrupt()

                FtcDashboard.getInstance().stopCameraStream()
                _visionPortal?.close()
            })
        }
    }
}