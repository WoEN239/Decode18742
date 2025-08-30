package com.qualcomm.robotcore.eventloop.opmode

import com.qualcomm.robotcore.hardware.Gamepad
import org.woen.utils.events.SimpleEvent

abstract class GamepadOpMode: LinearOpMode() {
    val gamepad1Callback = SimpleEvent<Gamepad>()
    val gamepad2Callback = SimpleEvent<Gamepad>()

    override fun newGamepadDataAvailable(
        latestGamepad1Data: Gamepad?,
        latestGamepad2Data: Gamepad?
    ) {
        super.newGamepadDataAvailable(latestGamepad1Data, latestGamepad2Data)

        latestGamepad1Data?.let { gamepad1Callback.invoke(it) }
        latestGamepad2Data?.let { gamepad2Callback.invoke(it) }
    }
}