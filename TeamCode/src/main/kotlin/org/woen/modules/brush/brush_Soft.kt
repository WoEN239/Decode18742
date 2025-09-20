package org.woen.modules.brush

import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.modules.brush.brush_hard
import org.woen.telemetry.ThreadedConfigs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedTimers

class BallDownEvent(var a: brush_Soft.AcktBrush)

class brush_Soft: IModule {
    enum class AcktBrush{
        Ackt,
        NotAckt,
        revers
    }

    init {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(BallDownEvent::class, {
            zapuskZov=it.a;//1-ack; 2-notack; 3-brake;
        })

    }

    private var _currentJob: Job? = null //отслеживание текущей задачи
    private var bruh = brush_hard(ThreadedConfigs.BRUSH_MOTOR_NAME.get());
    private var sost =0;
    private var zapuskZov = AcktBrush.NotAckt;
    private var tmr = ElapsedTime();
    suspend fun AvtoZov(){
        var difTmr=tmr.time()> ThreadedConfigs.BRUSH_DEF_TIME.get();
        if(zapuskZov == AcktBrush.revers) sost=3;
        when(sost){
            0->{
                bruh.setDir(ThreadedConfigs.BRUSH_MOTORS_FORWARD.get());
                if(!bruh.IsSafe)sost=1;
                if(zapuskZov== AcktBrush.NotAckt){ sost=2; tmr.reset();}

            }
            1->{
                bruh.setDir(ThreadedConfigs.BRUSH_MOTORS_BACK.get());
                if(zapuskZov== AcktBrush.Ackt && bruh.IsSafe && difTmr)sost=0;
                if(zapuskZov== AcktBrush.NotAckt) sost=2;
            }
            2->{
                bruh.setDir(ThreadedConfigs.BRUSH_MOTORS_STOP.get());
                if(zapuskZov== AcktBrush.Ackt) sost=0;
            }
            3->{
                zovback();
                if(zapuskZov== AcktBrush.Ackt) sost=0;
                if(zapuskZov== AcktBrush.Ackt) sost=2;
            }
        }
    }
    suspend fun zovback(){
        bruh.setDir(ThreadedConfigs.BRUSH_MOTORS_STOP.get());
        delay(1000);
    }
    //тут будут щётки
    override suspend fun process() {
        _currentJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch { //запрос и запуск куратины
            AvtoZov();
        }
    }

    override val isBusy: Boolean
        get() = _currentJob != null && !_currentJob!!.isCompleted

    override fun dispose() {
        _currentJob?.cancel()
    }
}