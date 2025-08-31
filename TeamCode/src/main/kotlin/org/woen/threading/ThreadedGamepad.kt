package org.woen.threading

import com.qualcomm.robotcore.eventloop.opmode.GamepadOpMode
import com.qualcomm.robotcore.hardware.Gamepad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ThreadedGamepad private constructor() {
    companion object{
        private var _nullableInstance: ThreadedGamepad? = null

        val LAZY_INSTANCE: ThreadedGamepad
            get() {
                if(_nullableInstance == null)
                    _nullableInstance = ThreadedGamepad()

                return _nullableInstance!!
            }

        fun restart(){
            _nullableInstance = null
        }
    }

    interface IListener{
        fun update(gamepadData: Gamepad)
    }

    private class HoldListener(val activateState: Boolean, val buttonSuppler: (Gamepad) -> Boolean,
                               val onTriggered: () -> Unit, val listenerCoroutineScope: CoroutineScope): IListener{
        override fun update(gamepadData: Gamepad) {
            if(buttonSuppler(gamepadData) == activateState)
                listenerCoroutineScope.launch {
                    onTriggered()
                }
        }
    }
    
    private class ClickListener(val activationState: Boolean, val buttonSuppler: (Gamepad) -> Boolean,
                                val onTriggered: () -> Unit, val listenerCoroutineScope: CoroutineScope): IListener{
        private var oldState = false
        
        override fun update(gamepadData: Gamepad) {
            val currentState = buttonSuppler(gamepadData)
            
            if(currentState != oldState && currentState == activationState){
                listenerCoroutineScope.launch {
                    onTriggered()
                }
            }
            
            oldState = currentState
        }
    }

    private class AnalogListener(val inputSuppler: (Gamepad) -> Double,
                                 val onTriggered: (Double) -> Unit, val listenerCoroutineScope: CoroutineScope): IListener {
        override fun update(gamepadData: Gamepad) {
            val data = inputSuppler(gamepadData)

            listenerCoroutineScope.launch {
                onTriggered(data)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun createHoldUpListener(buttonSuppler: (Gamepad) -> Boolean, onTriggered: () -> Unit,
                             listenerCoroutineScope: CoroutineScope = GlobalScope) =
        HoldListener(false, buttonSuppler, onTriggered, listenerCoroutineScope) as IListener
    
    @OptIn(DelicateCoroutinesApi::class)
    fun createHoldDownListener(buttonSuppler: (Gamepad) -> Boolean, onTriggered: () -> Unit,
                               listenerCoroutineScope: CoroutineScope = GlobalScope) =
        HoldListener(true, buttonSuppler, onTriggered, listenerCoroutineScope) as IListener

    @OptIn(DelicateCoroutinesApi::class)
    fun createClickUpListener(buttonSuppler: (Gamepad) -> Boolean, onTriggered: () -> Unit,
                              listenerCoroutineScope: CoroutineScope = GlobalScope) =
        ClickListener(false, buttonSuppler, onTriggered, listenerCoroutineScope) as IListener

    @OptIn(DelicateCoroutinesApi::class)
    fun createClickDownListener(buttonSuppler: (Gamepad) -> Boolean, onTriggered: () -> Unit,
                                listenerCoroutineScope: CoroutineScope = GlobalScope) =
        ClickListener(true, buttonSuppler, onTriggered, listenerCoroutineScope) as IListener

    @OptIn(DelicateCoroutinesApi::class)
    fun createAnalogListener(inputSuppler: (Gamepad) -> Double, onTriggered: (Double) -> Unit,
                             listenerCoroutineScope: CoroutineScope = GlobalScope) =
        AnalogListener(inputSuppler, onTriggered, listenerCoroutineScope) as IListener

    private val _allListeners = mutableSetOf<IListener>()

    @Synchronized
    fun addListener(listener: IListener) = _allListeners.add(listener)

    fun initCallbacks(opMode: GamepadOpMode){
        opMode.gamepad1Callback += {
            for(i in _allListeners)
                i.update(it)
        }
    }
}