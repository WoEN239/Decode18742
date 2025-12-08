package org.woen.modules.scoringSystem.turret

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.woen.hotRun.HotRun
import org.woen.telemetry.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.regulator.Regulator
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs

class HardwareTurret :
    IHardwareDevice {
    private lateinit var _motor: DcMotorEx
    private lateinit var _angleSevo: Servo

    var currentVoltage = 0.0

    val currentVelocity: Double
        get() = (_motorVelocity * 2.0 * PI * Configs.TURRET.PULLEY_RADIUS) / Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION

    var anglePosition = 0.0

    private val _velocityFilter =
        ExponentialFilter(Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.get())

    private var _oldMotorPosition = 0.0

    private var _motorVelocity = 0.0
    var shotWasFired = false

    private val _deltaTime = ElapsedTime()
    private val _shootTriggerTimer = ElapsedTime()

    private var _motorAmps = 0.0

    override fun update() {
        _motorAmps = _motor.getCurrent(CurrentUnit.AMPS)

        if (_motorAmps > Configs.TURRET.TURRET_SHOOT_DETECT_CURRENT)
            shotWasFired = _shootTriggerTimer.seconds() > Configs.TURRET.SHOOT_TRIGGER_DELAY
        else {
            _shootTriggerTimer.reset()
            shotWasFired = false
        }

        if (HotRun.LAZY_INSTANCE.currentRunState != HotRun.RunState.RUN)
            return

        _angleSevo.position = anglePosition

        val currentMotorPosition = _motor.currentPosition.toDouble()

        val rawVelocity = (currentMotorPosition - _oldMotorPosition) / _deltaTime.seconds()

        _deltaTime.reset()

        _motorVelocity =
            _velocityFilter.updateRaw(_motorVelocity, rawVelocity - _motorVelocity)

        _oldMotorPosition = currentMotorPosition

        _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(currentVoltage)
    }

    override fun init(hardwareMap: HardwareMap) {
        _motor = hardwareMap.get("pulleyMotor") as DcMotorEx
        _angleSevo = hardwareMap.get("turretAngleServo") as Servo

        val rotateServo = hardwareMap.get("turretRotateServo") as Servo

        Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.onSet += {
            _velocityFilter.coef = it
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            _velocityFilter.start()
            _deltaTime.reset()
            _shootTriggerTimer.reset()

            rotateServo.position = 0.51
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            _motor.mode = DcMotor.RunMode.RESET_ENCODERS
            _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addData("currentTurretVelocity", currentVelocity)
            it.addData("current ticks velocity", _motorVelocity)
            it.addData("angle pos", anglePosition)
            it.addData("pulley amps", _motorAmps)
        }
    }

    override fun dispose() {

    }
}