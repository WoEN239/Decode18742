package org.woen.modules.scoringSystem.brush


import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.telemetry.configs.Configs
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import java.util.concurrent.atomic.AtomicReference


class SwitchBrushStateEvent(var brushState: Brush.BrushState, var reverseTime: Long = 1000)

class Brush : IModule {
    enum class BrushState {
        FORWARD,
        STOP,
        REVERSE,
        INFINITE_REVERSE,
        SAFE,
        STOP_ON_TIME
    }

    private var _currentJob: Job? = null
    private var hwBrush = HardwareBrush()
    private var turnOn = AtomicReference(BrushState.FORWARD)
    private var timerRevers = AtomicReference<Long>(0)
    private var tmr = ElapsedTime()
    private var tmr1 = ElapsedTime()
    private var tmr2 = ElapsedTime()
    private var f12 = false
    private var f11 = false
    suspend fun autoUse()
    {
        if (!f12)
        {
            f12 = true
            tmr1.reset()
        }
        val difTmr = tmr.time() > Configs.BRUSH.BRUSH_DEF_TIME
        val startTmr = tmr1.time() > Configs.BRUSH.BRUSH_SAFE_TIME
        val errTime = tmr2.time() > Configs.BRUSH.BRUSH_ERR_TIME

        when (turnOn.get()) {
            BrushState.FORWARD -> {
                hwBrush.setDir(HardwareBrush.BrushDirection.FORWARD)

                if (!hwBrush.isSafe && !f11)
                {
                    f11 = true
                    tmr2.reset()
                }

                if (!hwBrush.isSafe && startTmr && errTime)
                {
                    turnOn.set(BrushState.SAFE)
                    tmr.reset()
                }
            }

            BrushState.SAFE -> {
                hwBrush.setDir(HardwareBrush.BrushDirection.REVERSE)
                if (hwBrush.isSafe && difTmr) {
                    turnOn.set(BrushState.FORWARD)
                }
                tmr2.reset()
                tmr1.reset()
                f11 = false
            }

            BrushState.STOP -> {
                hwBrush.setDir(HardwareBrush.BrushDirection.STOP)
                tmr2.reset()
                tmr1.reset()
                f11 = false
            }

            BrushState.REVERSE -> {
                reverse(timerRevers.get())
                if (turnOn.get() == BrushState.REVERSE) turnOn.set(BrushState.STOP)
                tmr2.reset()
                tmr1.reset()
                f11 = false
            }
            BrushState.INFINITE_REVERSE -> {
                turnOn.set(BrushState.FORWARD)
            }

            BrushState.STOP_ON_TIME->{
                hwBrush.setDir(HardwareBrush.BrushDirection.STOP)
                tmr2.reset()
                tmr1.reset()
                if(startTmr)turnOn.set(BrushState.FORWARD)
                f11 = false
            }

        }
    }

    suspend fun reverse(tmr1: Long = 1000) {
        hwBrush.setDir(HardwareBrush.BrushDirection.REVERSE)
        delay(tmr1)
    }

    override suspend fun process() {
        _currentJob =
            ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                autoUse()
            }
    }

    override val isBusy: Boolean
        get() = _currentJob != null && !_currentJob!!.isCompleted

    override fun opModeStart() {
        turnOn.set(BrushState.FORWARD)
    }

    override fun opModeStop() {

    }

    override fun dispose() {
        _currentJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(hwBrush)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SwitchBrushStateEvent::class, {
            turnOn.set(it.brushState)
            timerRevers.set(it.reverseTime)
        })

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addLine(turnOn.get().toString())
            it.addLine(hwBrush.volt.toString())
        }
    }
}