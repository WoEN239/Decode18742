package org.woen.threading

import com.qualcomm.robotcore.eventloop.opmode.GamepadOpMode
import com.qualcomm.robotcore.hardware.Gamepad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.hotRun.HotRun

class ThreadedGamepad private constructor() {
    companion object {
        private var _nullableInstance: ThreadedGamepad? = null

        private val _instanceMutex = Mutex()

        @JvmStatic
        val LAZY_INSTANCE: ThreadedGamepad
            get() =
                runBlocking {
                    _instanceMutex.withLock {
                        if (_nullableInstance == null)
                            _nullableInstance = ThreadedGamepad()

                        return@withLock _nullableInstance!!
                    }
                }

        fun restart() {
            runBlocking {
                _instanceMutex.withLock {
                    _nullableInstance = null
                }
            }
        }


        @OptIn(DelicateCoroutinesApi::class)
        fun createHoldUpListener(
            buttonSuppler: (Gamepad) -> Boolean, onTriggered: () -> Unit,
            listenerCoroutineScope: CoroutineScope = ThreadManager.LAZY_INSTANCE.globalCoroutineScope
        ) =
            HoldListener(false, buttonSuppler, onTriggered, listenerCoroutineScope) as IListener

        @OptIn(DelicateCoroutinesApi::class)
        fun createHoldDownListener(
            buttonSuppler: (Gamepad) -> Boolean, onTriggered: () -> Unit,
            listenerCoroutineScope: CoroutineScope = ThreadManager.LAZY_INSTANCE.globalCoroutineScope
        ) =
            HoldListener(true, buttonSuppler, onTriggered, listenerCoroutineScope) as IListener

        @OptIn(DelicateCoroutinesApi::class)
        fun createClickUpListener(
            buttonSuppler: (Gamepad) -> Boolean, onTriggered: () -> Unit,
            listenerCoroutineScope: CoroutineScope = ThreadManager.LAZY_INSTANCE.globalCoroutineScope
        ) =
            ClickListener(false, buttonSuppler, onTriggered, listenerCoroutineScope) as IListener

        @OptIn(DelicateCoroutinesApi::class)
        fun createClickDownListener(
            buttonSuppler: (Gamepad) -> Boolean, onTriggered: () -> Unit,
            listenerCoroutineScope: CoroutineScope = ThreadManager.LAZY_INSTANCE.globalCoroutineScope
        ) =
            ClickListener(true, buttonSuppler, onTriggered, listenerCoroutineScope) as IListener

        @OptIn(DelicateCoroutinesApi::class)
        fun createAnalogListener(
            inputSuppler: (Gamepad) -> Double, onTriggered: (Double) -> Unit,
            listenerCoroutineScope: CoroutineScope = ThreadManager.LAZY_INSTANCE.globalCoroutineScope
        ) =
            AnalogListener(inputSuppler, onTriggered, listenerCoroutineScope) as IListener
    }

    interface IListener {
        fun update(gamepadData: Gamepad)
    }

    private class HoldListener(
        val activateState: Boolean, val buttonSuppler: (Gamepad) -> Boolean,
        val onTriggered: () -> Unit, val listenerCoroutineScope: CoroutineScope
    ) : IListener {
        override fun update(gamepadData: Gamepad) {
            if (buttonSuppler(gamepadData) == activateState)
                listenerCoroutineScope.launch {
                    onTriggered()
                }
        }
    }

    private class ClickListener(
        val activationState: Boolean, val buttonSuppler: (Gamepad) -> Boolean,
        val onTriggered: () -> Unit, val listenerCoroutineScope: CoroutineScope
    ) : IListener {
        private var oldState = false

        override fun update(gamepadData: Gamepad) {
            val currentState = buttonSuppler(gamepadData)

            if (currentState != oldState && currentState == activationState) {
                listenerCoroutineScope.launch {
                    onTriggered()
                }
            }

            oldState = currentState
        }
    }

    private class AnalogListener(
        val inputSuppler: (Gamepad) -> Double,
        val onTriggered: (Double) -> Unit, val listenerCoroutineScope: CoroutineScope
    ) : IListener {
        override fun update(gamepadData: Gamepad) {
            val data = inputSuppler(gamepadData)

            listenerCoroutineScope.launch {
                onTriggered(data)
            }
        }
    }

    private val _listenersMutex = Mutex()

    private val _allListeners = mutableSetOf<IListener>()

    @Synchronized
    fun addListener(listener: IListener) =
        runBlocking {
            _listenersMutex.withLock {
                _allListeners.add(listener)
            }
        }

    fun initCallbacks(opMode: GamepadOpMode) {
        if(HotRun.LAZY_INSTANCE.currentRunMode.get() == HotRun.RunMode.MANUAL) {
            opMode.gamepad1Callback += {
                runBlocking {
                    _listenersMutex.withLock {
                        for (i in _allListeners)
                            i.update(it)
                    }
                }
            }
        }
    }
}