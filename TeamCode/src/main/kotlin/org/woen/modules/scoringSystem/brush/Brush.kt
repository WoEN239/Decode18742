package org.woen.modules.scoringSystem.brush


import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.telemetry.Configs
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
        REVERS,
        SAFE,
        STOP_ON_TIME
    }

    private var _currentJob: Job? = null
    private var bruh = HardwareBrush()
    private var turnOn = AtomicReference(BrushState.STOP)
    private var timerRevers = AtomicReference<Long>(0)
    private var tmr = ElapsedTime()
    private var tmr1 = ElapsedTime()
    private var tmr2 = ElapsedTime()
    private var tmr3 = ElapsedTime()
    private var f12 = false
    private var f11 = false
    suspend fun AvtoUse() {
        if (!f12) {
            f12 = true; tmr1.reset()
        }
        val difTmr = tmr.time() > Configs.BRUSH.BRUSH_DEF_TIME
        val startTmr = tmr1.time() > Configs.BRUSH.BRUSH_SAFE_TIME
        val errTime = tmr2.time() > Configs.BRUSH.BRUSH_ERR_TIME
        val stopTime = tmr3.time() > Configs.BRUSH.BRUSH_STOP_TIME

        when (turnOn.get()) {
            BrushState.FORWARD -> {
                bruh.setDir(HardwareBrush.BrushDirection.FORWARD)
                if (!bruh.isSafe && !f11) {
                    f11 = true; tmr2.reset()
                }
               if(!bruh.isSafe1){
                   turnOn.set(BrushState.STOP_ON_TIME)
                   tmr3.reset()
               }
                if (!bruh.isSafe && startTmr && errTime) {
                    turnOn.set(BrushState.SAFE); tmr.reset()
                }
            }

            BrushState.SAFE -> {
                bruh.setDir(HardwareBrush.BrushDirection.REVERS)
                if (bruh.isSafe && difTmr) {
                    turnOn.set(BrushState.FORWARD)
                }
                tmr2.reset()
                tmr1.reset()
                f11 = false
            }

            BrushState.STOP -> {
                bruh.setDir(HardwareBrush.BrushDirection.STOP)
                tmr2.reset()
                tmr1.reset()
                f11 = false
            }

            BrushState.REVERS -> {
                revers(timerRevers.get())
                turnOn.set(BrushState.STOP)
                tmr2.reset()
                tmr1.reset()
                f11 = false
            }

            BrushState.STOP_ON_TIME->{
                bruh.setDir(HardwareBrush.BrushDirection.STOP)
                tmr2.reset()
                tmr1.reset()
                if(startTmr)turnOn.set(BrushState.FORWARD)
                f11 = false
            }

        }
    }

    suspend fun revers(tmr1: Long = 1000) {
        bruh.setDir(HardwareBrush.BrushDirection.REVERS)
        delay(tmr1)
    }

    override suspend fun process() {
        _currentJob =
            ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {
                AvtoUse()
            }
    }

    override val isBusy: Boolean
        get() = _currentJob != null && !_currentJob!!.isCompleted

    override fun dispose() {
        _currentJob?.cancel()
    }

    constructor() {
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(bruh)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SwitchBrushStateEvent::class, {
            turnOn.set(it.brushState)
            timerRevers.set(it.reverseTime)
        })

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            turnOn.set(BrushState.FORWARD)
        }

        ThreadedTelemetry.LAZY_INSTANCE.onTelemetrySend += {
            it.addLine(turnOn.get().toString())
            it.addLine(bruh.volt.toString())
        }
    }
}