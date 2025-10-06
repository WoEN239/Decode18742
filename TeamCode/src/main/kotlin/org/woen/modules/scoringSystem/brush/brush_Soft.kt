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
        ACKT,
        NOT_ACKT,
        REVERS
    }

    init {
        ThreadedEventBus.LAZY_INSTANCE.subscribe(BallDownEvent::class, {
            turnOn=it.a;//1-ack; 2-notack; 3-brake;
        })

    }

    private var _currentJob: Job? = null //отслеживание текущей задачи
    private var bruh = brush_hard(Configs.BRUSH.BRUSH_MOTOR_NAME);
    private var sost =0;
    private var turnOn = AcktBrush.NOT_ACKT;
    private var tmr = ElapsedTime();
    suspend fun AvtoUse(){
        var difTmr=tmr.time()> Configs.BRUSH.BRUSH_DEF_TIME;
        if(turnOn == AcktBrush.REVERS) sost=3;
        when(sost){
            0->{
                bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_FORWARD);
                if(!bruh.IsSafe)sost=1;
                if(turnOn== AcktBrush.NOT_ACKT){ sost=2; tmr.reset();}

            }
            1->{
                bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_BACK);
                if(turnOn== AcktBrush.ACKT && bruh.IsSafe && difTmr)sost=0;
                if(turnOn== AcktBrush.NOT_ACKT) sost=2;
            }
            2->{
                bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_STOP);
                if(turnOn== AcktBrush.ACKT) sost=0;
            }
            3->{
                revers();
                if(turnOn== AcktBrush.ACKT) sost=0;
                if(turnOn== AcktBrush.ACKT) sost=2;
            }
            
        }
    }
    suspend fun revers(){
        bruh.setDir(Configs.BRUSH.BRUSH_MOTORS_STOP);
        delay(1000);
    }
    //тут будут щётки
    override suspend fun process() {
        _currentJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch { //запрос и запуск куратины
            AvtoUse();
        }
    }

    override val isBusy: Boolean
        get() = _currentJob != null && !_currentJob!!.isCompleted

    override fun dispose() {
        _currentJob?.cancel()
    }
}