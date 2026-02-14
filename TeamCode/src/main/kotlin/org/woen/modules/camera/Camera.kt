package org.woen.modules.camera

import com.qualcomm.hardware.limelightvision.LLResult
import com.qualcomm.hardware.limelightvision.Limelight3A
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import kotlinx.coroutines.DisposableHandle
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun
import org.woen.modules.scoringSystem.turret.Pattern
import org.woen.modules.scoringSystem.turret.RequestTurretCurrentRotation
import org.woen.telemetry.ThreadedTelemetry
import org.woen.telemetry.configs.Configs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Angle
import org.woen.utils.units.Color
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import kotlin.concurrent.thread

data class OnPatternDetectedEvent(val pattern: Pattern)
data class CameraUpdateEvent(val orientation: Orientation)

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

    private var _currentOrientation = Orientation()

    var currentPattern: Pattern? = null

    private val _thread =
        ThreadManager.LAZY_INSTANCE.register(thread(start = false, name = "Limelight thread") {
            val hardwareMap =
                OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity).hardwareMap
            val limelight = hardwareMap.get(Limelight3A::class.java, "limelight")

            limelight.pipelineSwitch(0)
            limelight.start()

            while (!Thread.currentThread().isInterrupted && Configs.CAMERA.CAMERA_ENABLE) {
                val result: LLResult? = limelight.latestResult

                if (result != null && result.isValid) {
                    val botPos = result.botpose

                    val turretAngle =
                        Angle(0.0)// ThreadedEventBus.LAZY_INSTANCE.invoke(RequestTurretCurrentRotation()).rotation

                    val cameraRotation = botPos.orientation.getYaw(AngleUnit.RADIANS)

                    val robotRotation = Angle(cameraRotation) - turretAngle

                    _currentOrientation = Orientation(
                        Vec2(botPos.position.x, botPos.position.y) -
                                Configs.CAMERA.CAMERA_TURRET_POS.turn(cameraRotation) -
                                Configs.TURRET.TURRET_CENTER_POS.turn(robotRotation.angle),
                        robotRotation
                    )

                    ThreadedEventBus.LAZY_INSTANCE.invoke(CameraUpdateEvent(_currentOrientation))

                    val fiducialResults = result.fiducialResults
                    if (currentPattern == null && fiducialResults.isNotEmpty()) {
                        for (fr in fiducialResults) {
                            val tagId = fr.fiducialId
                            val foundPattern = Pattern.patterns.find { it.cameraTagId == tagId }

                            if (foundPattern != null) {
                                currentPattern = foundPattern
                                ThreadedEventBus.LAZY_INSTANCE.invoke(
                                    OnPatternDetectedEvent(
                                        currentPattern!!
                                    )
                                )
                                ThreadedTelemetry.LAZY_INSTANCE.log("LL Pattern: ${foundPattern.cameraTagId}")
                            }
                        }
                    }
                } else
                    Thread.sleep(5)
            }

            limelight.stop()
        })

    override fun dispose() {
        _thread.interrupt()
    }

    private constructor() {
        if (Configs.CAMERA.CAMERA_ENABLE) {
            ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
                it.drawRect(
                    _currentOrientation.pos,
                    Configs.DRIVE_TRAIN.ROBOT_SIZE,
                    _currentOrientation.angle,
                    Color.BLACK
                )
            }

            HotRun.LAZY_INSTANCE.opModeStartEvent += {
                if (Configs.CAMERA.CAMERA_ENABLE)
                    _thread.start()
            }

            HotRun.LAZY_INSTANCE.opModeStopEvent += {
                _thread.interrupt()
            }
        }
    }
}