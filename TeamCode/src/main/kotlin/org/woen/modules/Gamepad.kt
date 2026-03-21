package org.woen.modules

import com.qualcomm.robotcore.hardware.Gamepad
import org.woen.collector.Collector
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

class AddGamepad1ListenerEvent(val gamepadListener: IGamepadListener)
class AddGamepad2ListenerEvent(val gamepadListener: IGamepadListener)

fun attachGamepad(collector: Collector) {
    val gamepad1 = collector.opMode.gamepad1
    val gamepad2 = collector.opMode.gamepad2

    val gamepad1Listeners = mutableSetOf<IGamepadListener>()
    val gamepad2Listeners = mutableSetOf<IGamepadListener>()

    collector.eventBus.subscribe(AddGamepad1ListenerEvent::class) {
        gamepad1Listeners.add(it.gamepadListener)
    }

    collector.eventBus.subscribe(AddGamepad2ListenerEvent::class) {
        gamepad2Listeners.add(it.gamepadListener)
    }

    if (collector.runMode == RunMode.MANUAL)
        collector.updateEvent += {
            for (i in gamepad1Listeners)
                i.update(gamepad1)

            for(i in gamepad2Listeners)
                i.update(gamepad2)
        }
}