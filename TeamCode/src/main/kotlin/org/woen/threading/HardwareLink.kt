package org.woen.threading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.woen.modules.TestModule
import java.util.concurrent.Executors

class HardwareLink(val modules: Array<TestModule>){
    companion object{
        private val _coroutineExecutor = Executors.newSingleThreadExecutor()
        private val _coroutineDispatcher = _coroutineExecutor.asCoroutineDispatcher()
        private val _coroutineScope = CoroutineScope(_coroutineDispatcher + Job())
    }

    fun update(): Job = _coroutineScope.launch {
        for(i in modules){
            if(i.isBusy()){
                val data = i.getData()
                i.process(data)
            }
        }
    }
}