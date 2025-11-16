package org.woen.hotRun

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.modules.camera.Camera
import org.woen.modules.runner.actions.ActionRunner
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad
import org.woen.threading.ThreadedTimers
import org.woen.threading.hardware.HardwareThreads
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.events.SimpleEvent
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

class HotRun private constructor() {
    companion object {
        private var _nullableInstance: HotRun? = null

        private val _instanceMutex = SmartMutex()

        val LAZY_INSTANCE: HotRun
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null)
                    _nullableInstance = HotRun()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance = null
            }

            ThreadManager.restart()
            ThreadedTelemetry.restart()
            ActionRunner.restart()
            ThreadedTimers.restart()
            ThreadedEventBus.restart()
            Camera.restart()
            ThreadedGamepad.restart()
            ThreadedBattery.restart()
            HardwareThreads.restart()
        }
    }

    var currentRunState = AtomicReference(RunState.STOP)
        private set

    var currentRunMode = AtomicReference(RunMode.MANUAL)
        private set

    enum class RunState {
        INIT,
        STOP,
        RUN
    }

    enum class RunMode {
        MANUAL,
        AUTO
    }

    enum class RunColor(
        private val _basketPosition: Vec2, private val _startOrientation: Orientation
    ) {
        RED(Configs.TURRET.RED_BASKET_POSITION, Configs.ODOMETRY.START_RED_ORIENTATION),
        BLUE(Configs.TURRET.BLUE_BASKET_POSITION, Configs.ODOMETRY.START_BLUE_ORIENTATION);

        val basketPosition
            get() = _basketPosition.clone()

        val startOrientation
            get() = _startOrientation.clone()
    }

    var currentRunColor = AtomicReference(RunColor.RED)

    val opModeInitEvent = SimpleEvent<LinearOpMode>()
    val opModeStartEvent = SimpleEvent<LinearOpMode>()
    val opModeUpdateEvent = SimpleEvent<LinearOpMode>()
    val opModeStopEvent = SimpleEvent<LinearOpMode>()
    lateinit var _gamepad: Gamepad

    fun run(opMode: LinearOpMode, runMode: RunMode, isGamepadStart: Boolean = false) {
        try {
            _gamepad = opMode.gamepad1

            currentRunMode.set(runMode)
            currentRunState.set(RunState.INIT)

            ThreadManager.LAZY_INSTANCE
            ThreadedGamepad.LAZY_INSTANCE.init()
            ThreadedTelemetry.LAZY_INSTANCE.setDriveTelemetry(opMode.telemetry)
            HardwareThreads.LAZY_INSTANCE
            ActionRunner.LAZY_INSTANCE
            Camera.LAZY_INSTANCE

            opModeInitEvent.invoke(opMode)

            ThreadedTelemetry.LAZY_INSTANCE.log("init complited")

            while (!opMode.isStarted()) {
                if (isGamepadStart && (opMode.gamepad1.left_bumper || opMode.gamepad1.right_bumper ||
                    abs(opMode.gamepad1.left_stick_y.toDouble()) > 0.01 || abs(opMode.gamepad1.left_stick_x.toDouble()) > 0.01
                    || abs(opMode.gamepad1.right_stick_x.toDouble()) > 0.01
                ))
                    OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().activity)
                        .startActiveOpMode()
            }
            opMode.resetRuntime()

            ThreadedTelemetry.LAZY_INSTANCE.log("start")

            currentRunState.set(RunState.RUN)

            opModeStartEvent.invoke(opMode)

            while (opMode.opModeIsActive()) {
                opModeUpdateEvent.invoke(opMode)
            }

            currentRunState.set(RunState.STOP)

            opModeStopEvent.invoke(opMode)
        } catch (e: Exception) {
            throw e
        }
    }

    fun gamepadRumble(duration: Double){
        _gamepad.rumble((duration * 1000.0).toInt())
    }
}