package org.woen.modules

import com.qualcomm.robotcore.hardware.Gamepad
import org.woen.collector.Collector
import org.woen.collector.GameState
import org.woen.collector.RunMode

interface IGamepadListener {
    fun update(gamepadData: Gamepad)
}

class HoldGamepadListener(
    val activateState: Boolean, val buttonSuppler: (Gamepad) -> Boolean,
    val onTriggered: () -> Unit
) : IGamepadListener {
    override fun update(gamepadData: Gamepad) {
        if (buttonSuppler(gamepadData) == activateState)
            onTriggered()
    }
}

class ClickGamepadListener(
    val buttonSuppler: (Gamepad) -> Boolean,
    val onTriggered: () -> Unit,
    val activationState: Boolean = true
) : IGamepadListener {
    private var _oldState = false

    override fun update(gamepadData: Gamepad) {
        val currentState = buttonSuppler(gamepadData)

        if (currentState != _oldState && currentState == activationState)
            onTriggered()

        _oldState = currentState
    }
}

class AnalogGamepadListener(
    val inputSuppler: (Gamepad) -> Double,
    val onTriggered: (Double) -> Unit
) : IGamepadListener {
    override fun update(gamepadData: Gamepad) {
        val data = inputSuppler(gamepadData)

        onTriggered(data)
    }
}

class AddGamepadListenerEvent(val gamepadListener: IGamepadListener)

fun attachGamepad(collector: Collector) {
    val gamepad = collector.opMode.gamepad1
    val gamepadListeners = mutableSetOf<IGamepadListener>()

    collector.eventBus.subscribe(AddGamepadListenerEvent::class) {
        gamepadListeners.add(it.gamepadListener)
    }

    if (GameState.runMode == RunMode.MANUAL)
        collector.updateEvent += {
            for (i in gamepadListeners)
                i.update(gamepad)
        }
}