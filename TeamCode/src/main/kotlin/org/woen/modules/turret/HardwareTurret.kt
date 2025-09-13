package org.woen.modules.turret

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.hotRun.HotRun
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.hardware.IHardwareDevice
import org.woen.threading.hardware.ThreadedBattery
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.regulator.Regulator
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI

class HardwareTurret(private val _motorName: String) : IHardwareDevice {
    private lateinit var _motor: DcMotorEx

    var targetVelocity: Double
        get() = _realTargetVelocity.get() /
                ThreadedConfigs.PULLEY_TICKS_IN_REVOLUTION.get() * (2.0 * PI * ThreadedConfigs.PULLEY_RADIUS.get())
        set(value) {
            _realTargetVelocity.set(
                value /
                        (2.0 * PI * ThreadedConfigs.PULLEY_RADIUS.get()) * ThreadedConfigs.PULLEY_TICKS_IN_REVOLUTION.get()
            )
        }

    private var _realTargetVelocity = AtomicReference(0.0)

    private val _pulleyRegulator = Regulator(ThreadedConfigs.PULLEY_REGULATOR)

    private val _pulleyRegulatorMutex = Mutex()

    private val _velocityFilterMutex = Mutex()

    private val _velocityFilter =
        ExponentialFilter(ThreadedConfigs.PULLEY_VELOCITY_FILTER_COEF.get())

    private var _oldMotorPosition = 0.0

    private var _motorVelocity = 0.0

    override fun update() {
        val currentMotorPosition = _motor.currentPosition.toDouble()

        val rawVelocity = currentMotorPosition - _oldMotorPosition

        runBlocking {
            _velocityFilterMutex.withLock {
                _motorVelocity =
                    _velocityFilter.updateRaw(_motorVelocity, rawVelocity - _motorVelocity)
            }
        }

        _oldMotorPosition = currentMotorPosition

        if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.RUN)
            return

        val target = _realTargetVelocity.get()
        val velErr = target - _motorVelocity

        runBlocking {
            _pulleyRegulatorMutex.withLock {
                _motor.power = ThreadedBattery.LAZY_INSTANCE.voltageToPower(
                    _pulleyRegulator.update(
                        velErr,
                        target
                    )
                )
            }
        }
    }

    override fun init(hardwareMap: HardwareMap) {
        _motor = hardwareMap.get(_motorName) as DcMotorEx

        _motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        _motor.mode = DcMotor.RunMode.RESET_ENCODERS
        _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        ThreadedConfigs.PULLEY_VELOCITY_FILTER_COEF.onSet += {
            runBlocking {
                _velocityFilterMutex.withLock {
                    _velocityFilter.coef = it
                }
            }
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            runBlocking {
                _pulleyRegulatorMutex.withLock {
                    _pulleyRegulator.start()
                }
            }
        }

        HotRun.LAZY_INSTANCE.opModeInitEvent +={
            runBlocking {
                _velocityFilterMutex.withLock {
                    _velocityFilter.start()
                }
            }
        }
    }

    override fun dispose() {

    }
}