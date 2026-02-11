package org.woen.modules.camera

import com.qualcomm.hardware.limelightvision.LLResult
import com.qualcomm.hardware.limelightvision.Limelight3A
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import kotlinx.coroutines.DisposableHandle
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun
import org.woen.modules.scoringSystem.turret.Pattern
import org.woen.telemetry.configs.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
import kotlin.concurrent.thread

// Твои события
data class OnPatternDetectedEvent(val pattern: Pattern)
data class GetRobotCoords(val x: Double, val z: Double)

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
    }

    private val hardwareMap = OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).hardwareMap
    private val limelight = hardwareMap.get(Limelight3A::class.java, "limelight")
    private val localizer = TurretLocalizer()

    @Volatile
    var turretAngle: Double = 0.0

    @Volatile
    var robotPose: Pose3D? = null

    // Геттеры для удобного доступа
    val robotX: Double get() = robotPose?.position?.x ?: 0.0
    val robotZ: Double get() = robotPose?.position?.y ?: 0.0 // Используем y из Pose3D как вторую координату поля

    var currentPattern: Pattern? = null

    private val _thread =
        ThreadManager.LAZY_INSTANCE.register(thread(start = true, name = "Limelight thread") {
            limelight.pipelineSwitch(0)
            limelight.start()

            while (!Thread.currentThread().isInterrupted && Configs.CAMERA.CAMERA_ENABLE) {
                if (HotRun.LAZY_INSTANCE.currentRunState != HotRun.RunState.RUN) {
                    Thread.sleep(20)
                    continue
                }

                val result: LLResult? = limelight.latestResult

                if (result != null && result.isValid) {
                    // 1. Расчет локализации
                    val newPose = localizer.getGlobalRobotPose(limelight, turretAngle)

                    if (newPose != null) {
                        robotPose = newPose
                        // Генерируем событие с координатами
                        ThreadedEventBus.LAZY_INSTANCE.invoke(
                            GetRobotCoords(robotX, robotZ)
                        )
                    }

                    // 2. Логика паттернов
                    val fiducialResults = result.fiducialResults
                    if (currentPattern == null && fiducialResults.isNotEmpty()) {
                        for (fr in fiducialResults) {
                            val tagId = fr.fiducialId
                            val foundPattern = Pattern.patterns.find { it.cameraTagId == tagId.toInt() }

                            if (foundPattern != null) {
                                currentPattern = foundPattern
                                ThreadedEventBus.LAZY_INSTANCE.invoke(OnPatternDetectedEvent(currentPattern!!))
                                ThreadedTelemetry.LAZY_INSTANCE.log("LL Pattern: ${foundPattern.cameraTagId}")
                            }
                        }
                    }
                }
                Thread.sleep(10)
            }
        })

    override fun dispose() {
        _thread.interrupt()
    }

    private constructor() {
        if (Configs.CAMERA.CAMERA_ENABLE) {
            ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
                it.addData("Robot X", "%.3f".format(robotX))
                it.addData("Robot Z", "%.3f".format(robotZ))
            }
        }
    }
}