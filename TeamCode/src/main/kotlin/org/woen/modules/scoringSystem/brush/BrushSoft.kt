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
import java.util.concurrent.atomic.AtomicReference

class SwitchBrush(var a: brush_Soft.AcktBrush, var time1: Long = 1000)

class brush_Soft : IModule {
    enum class AcktBrush {
        ACKT,
        NOT_ACKT,
        REVERS,
        SAFE
    }

    init {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(SwitchBrush::class, {
            turnOn.set(it.a);//1-ack; 2-notack; 3-brake; 4- revers with fixed time
            timerRevers.set(it.time1);
        })

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            turnOn.set(AcktBrush.ACKT)
        }
    }

    private var _currentJob: Job? = null //отслеживание текущей задачи
    private var bruh = brush_hard(Configs.BRUSH.BRUSH_MOTOR_NAME);
    private var turnOn = AtomicReference(AcktBrush.NOT_ACKT);
    private var timerRevers = AtomicReference<Long>(0);
    private var tmr = ElapsedTime();
    suspend fun AvtoUse() {
        val difTmr = tmr.time() > Configs.BRUSH.BRUSH_DEF_TIME;

        when (turnOn.get()) {
            AcktBrush.ACKT -> {
                bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_FORWARD);
                if (!bruh.IsSafe.get()){ turnOn.set(AcktBrush.SAFE); tmr.reset();}
            }

            AcktBrush.SAFE  -> {
                bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_BACK);
                if (bruh.IsSafe.get() && difTmr) turnOn.set(AcktBrush.ACKT);
            }

            AcktBrush.NOT_ACKT -> {
                bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_STOP);

            }

            AcktBrush.REVERS -> {
                revers(timerRevers.get());
                turnOn.set(AcktBrush.NOT_ACKT);
            }

        }
    }

    suspend fun revers(tmr1: Long = 1000) {
        bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_BACK);
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
}