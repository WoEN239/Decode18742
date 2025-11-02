package org.woen.modules.scoringSystem.brush

import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.hotRun.HotRun
import org.woen.modules.IModule
import org.woen.telemetry.Configs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.hardware.HardwareThreads
import java.util.concurrent.atomic.AtomicReference

class SwitchBrush(var a: BrushSoft.AcktBrush, var time1: Long = 1000)

class BrushSoft : IModule {
    enum class AcktBrush {
        ACKT,
        NOT_ACKT,
        REVERS,
        SAFE
    }

    private var _currentJob: Job? = null //отслеживание текущей задачи
    private var bruh = BrushHard("brushMotor");
    private var turnOn = AtomicReference(AcktBrush.NOT_ACKT);
    private var timerRevers = AtomicReference<Long>(0);
    private var tmr = ElapsedTime();
    private var tmr1 = ElapsedTime();
    private var tmr2= ElapsedTime();
    private var f12 =false ;
    private var f11=false;
    suspend fun AvtoUse() {
        if(!f12){f12=true; tmr1.reset();}
        val difTmr = tmr.time() > Configs.BRUSH.BRUSH_DEF_TIME;
        val startTmr = tmr1.time() > Configs.BRUSH.BRUSH_SAFE_TIME;
        val errTime=tmr1.time() > Configs.BRUSH.BRUSH_ERR_TIME;
        when (turnOn.get()) {
            AcktBrush.ACKT -> {
                bruh.setDir(BrushHard.motor_state.ACKT);
                if(!bruh.IsSafe.get()&&!f11){f11=true;tmr2.reset();}
                if (!bruh.IsSafe.get()&&startTmr&&errTime){ turnOn.set(AcktBrush.SAFE); tmr.reset();}
            }

            AcktBrush.SAFE  -> {
                bruh.setDir(BrushHard.motor_state.REVERS);
                if (bruh.IsSafe.get() && difTmr){ turnOn.set(AcktBrush.ACKT);}
                tmr1.reset();
                f11=false;
            }

            AcktBrush.NOT_ACKT -> {
                bruh.setDir(BrushHard.motor_state.NOT_ACKT);
                tmr1.reset();
                f11=false;
            }

            AcktBrush.REVERS -> {
                revers(timerRevers.get());
                turnOn.set(AcktBrush.NOT_ACKT);
                tmr1.reset();
                f11=false;
            }

        }
    }

    suspend fun revers(tmr1: Long = 1000) {
        bruh.setDir(BrushHard.motor_state.REVERS);
        delay(tmr1);
    }

    //тут будут щётки
    override suspend fun process() {
        _currentJob =
            ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch { //запрос и запуск куратины
                AvtoUse();
            }
    }

    override val isBusy: Boolean
        get() = _currentJob != null && !_currentJob!!.isCompleted

    override fun dispose() {
        _currentJob?.cancel()
    }

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(bruh)

        ThreadedEventBus.LAZY_INSTANCE.subscribe(SwitchBrush::class, {
            turnOn.set(it.a);//1-ack; 2-notack; 3-brake; 4- revers with fixed time
            timerRevers.set(it.time1);
        })

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            turnOn.set(AcktBrush.ACKT)
        }
    }
}