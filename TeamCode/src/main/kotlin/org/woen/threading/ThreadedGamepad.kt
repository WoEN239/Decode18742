package org.woen.threading

import com.qualcomm.robotcore.hardware.Gamepad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.woen.hotRun.HotRun
import org.woen.utils.smartMutex.SmartMutex
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

class ThreadedGamepad {
    companion object {
        private var _nullableInstance: ThreadedGamepad? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: ThreadedGamepad
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null)
                    _nullableInstance = ThreadedGamepad()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance = null
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
        suspend fun update(gamepadData: Gamepad)
    }

    private class HoldListener(
        val activateState: Boolean, val buttonSuppler: (Gamepad) -> Boolean,
        val onTriggered: suspend () -> Unit, val listenerCoroutineScope: CoroutineScope
    ) : IListener {
        override suspend fun update(gamepadData: Gamepad) {
            if (buttonSuppler(gamepadData) == activateState)
                listenerCoroutineScope.launch {
                    onTriggered()
                }
        }
    }

    private class ClickListener(
        val activationState: Boolean, val buttonSuppler: (Gamepad) -> Boolean,
        val onTriggered: suspend () -> Unit, val listenerCoroutineScope: CoroutineScope
    ) : IListener {
        private var oldState = false

        override suspend fun update(gamepadData: Gamepad) {
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
        val onTriggered: suspend (Double) -> Unit, val listenerCoroutineScope: CoroutineScope
    ) : IListener {
        override suspend fun update(gamepadData: Gamepad) {
            val data = inputSuppler(gamepadData)

            listenerCoroutineScope.launch {
                onTriggered(data)
            }
        }
    }

    private val _gamepad1Listeners = CopyOnWriteArrayList<IListener>()
    private val _gamepad2Listeners = CopyOnWriteArrayList<IListener>()

    private lateinit var _gamepad1: Gamepad
    private lateinit var _gamepad2: Gamepad

    fun addGamepad1Listener(listener: IListener) = _gamepad1Listeners.add(listener)
    fun addGamepad2Listener(listener: IListener) = _gamepad2Listeners.add(listener)

    private fun getIsGamepadTriggered(gamepad: Gamepad) =
        abs(gamepad.left_stick_x) > 0.01 || abs(gamepad.left_stick_y) > 0.01 ||
                abs(gamepad.right_stick_x) > 0.01 || abs(gamepad.right_stick_y) > 0.01 ||
                gamepad.left_trigger > 0.01 || gamepad.right_trigger > 0.01 || gamepad.left_bumper ||
                gamepad.right_bumper || gamepad.ps || gamepad.touchpad || gamepad.dpad_up ||
                gamepad.dpad_down || gamepad.dpad_left || gamepad.dpad_right ||
                gamepad.circle || gamepad.square || gamepad.triangle || gamepad.cross

    fun getIsGamepadsTriggered() = getIsGamepadTriggered(_gamepad1) || getIsGamepadTriggered(_gamepad2)

    fun rumble1(durationSec: Double) {
        if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.MANUAL)
            _gamepad1.rumble((durationSec * 1000.0).toInt())
    }

    fun rumble2(durationSec: Double) {
        if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.MANUAL)
            _gamepad1.rumble((durationSec * 1000.0).toInt())
    }

    fun init(gamepad1: Gamepad, gamepad2: Gamepad) {
        _gamepad1 = gamepad1
        _gamepad2 = gamepad2
    }

    private constructor() {
        HotRun.LAZY_INSTANCE.opModeUpdateEvent += {
            if (HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.MANUAL) {
                runBlocking {
                    for (i in _gamepad1Listeners)
                        i.update(_gamepad1)

                    for(i in _gamepad2Listeners)
                        i.update(_gamepad2)
                }
            }
        }
    }
}