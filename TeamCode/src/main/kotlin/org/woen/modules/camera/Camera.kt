package org.woen.modules.camera

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import kotlinx.coroutines.DisposableHandle
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import org.woen.hotRun.HotRun
import org.woen.modules.scoringSystem.turret.Pattern
import org.woen.telemetry.Configs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
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

    private var _aprilProcessor: AprilTagProcessor? = null

    var currentPattern: AtomicReference<Pattern?> = AtomicReference()

    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = true) {
        while (!Thread.currentThread().isInterrupted && Configs.CAMERA.CAMERA_ENABLE) {
            if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN || _aprilProcessor == null) {
                Thread.sleep(5)
                continue
            }

            val detections = _aprilProcessor?.freshDetections

            if (detections == null) {
                Thread.sleep(5)
                continue
            }

            for (detection in detections) {
                if (currentPattern.get() == null) {
                    val pattern = Pattern.patterns.find { it.cameraTagId == detection.id }

                    if (pattern != null) {
                        currentPattern.set(pattern)
                        ThreadedEventBus.LAZY_INSTANCE.invoke(OnPatternDetectedEvent(pattern))
                    }
                }
            }
        }
    })

    override fun dispose() {

    }

    private constructor() {
        if (Configs.CAMERA.CAMERA_ENABLE) {
            val hardwareMap =
                OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).hardwareMap

            HotRun.LAZY_INSTANCE.opModeInitEvent += {
                val helpCameraProcessor = HelpCameraProcessor()

                _aprilProcessor =
                    AprilTagProcessor.Builder().setDrawAxes(true).build()

                _visionPortal =
                    VisionPortal.Builder().setCamera(hardwareMap.get("Webcam 1") as WebcamName)
                        .addProcessors(_aprilProcessor, helpCameraProcessor).build()

                FtcDashboard.getInstance().startCameraStream(helpCameraProcessor, 10.0)
            }

            HotRun.LAZY_INSTANCE.opModeStopEvent += {
                FtcDashboard.getInstance().stopCameraStream()
                _visionPortal?.close()
            }
        }
    }
}