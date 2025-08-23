package org.woen.hotRun

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import kotlinx.coroutines.DisposableHandle
import org.woen.threading.HardwareThreads

class HotRun private constructor(): DisposableHandle {
    companion object{
        var INSTANCE: HotRun? = null

        fun init(){
            INSTANCE = HotRun()
            HardwareThreads.init()
        }

        fun restart(){
            INSTANCE?.dispose()
            init()
        }
    }

    var currentRunState = RunState.STOP
        private set

    enum class RunState{
        INIT,
        STOP,
        RUN
    }

    fun run(opMode: LinearOpMode){
        currentRunState = RunState.INIT

        opMode.waitForStart()

        currentRunState = RunState.RUN

        while (opMode.opModeIsActive()){

        }

        currentRunState = RunState.STOP
    }

    override fun dispose() {
        HardwareThreads.INSTANCE?.dispose()
    }
}