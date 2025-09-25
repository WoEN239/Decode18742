package org.woen.modules.scoringSystem.brush

import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.telemetry.Configs
import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus

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
    private var bruh = brush_hard(Configs.BRUSH.BRUSH_MOTOR_NAME);
    private var sost =0;
    private var zapuskZov = AcktBrush.NotAckt;
    private var tmr = ElapsedTime();
    suspend fun AvtoZov(){
        var difTmr=tmr.time()> Configs.BRUSH.BRUSH_DEF_TIME;
        if(zapuskZov == AcktBrush.revers) sost=3;
        when(sost){
            0->{
                bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_FORWARD);
                if(!bruh.IsSafe)sost=1;
                if(zapuskZov== AcktBrush.NotAckt){ sost=2; tmr.reset();}

            }
            1->{
                bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_BACK);
                if(zapuskZov== AcktBrush.Ackt && bruh.IsSafe && difTmr)sost=0;
                if(zapuskZov== AcktBrush.NotAckt) sost=2;
            }
            2->{
                bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_STOP);
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
        bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_STOP);
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