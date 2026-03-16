package org.woen.collector

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.modules.Battery
import org.woen.modules.Telemetry
import org.woen.modules.attachDriveTrain
import org.woen.modules.attachGamepad
import org.woen.modules.attachOdometry
import org.woen.modules.attachTurret
import org.woen.utils.events.EventBus
import org.woen.utils.events.SimpleEmptyEvent

class Collector {
    val startEvent = SimpleEmptyEvent()
    val initUpdateEvent = SimpleEmptyEvent()
    val updateEvent = SimpleEmptyEvent()
    val stopEvent = SimpleEmptyEvent()

    val opMode: LinearOpMode

    val eventBus = EventBus()
    val telemetry: Telemetry
    val battery: Battery
    val hardwareMap: HardwareMap

    constructor(opMode: LinearOpMode) {
        this.opMode = opMode
        this.hardwareMap = opMode.hardwareMap

        stopEvent += {
            for (i in opMode.hardwareMap.servoController) i.pwmDisable()
        }

        telemetry = Telemetry(this)
        battery = Battery(this)
        attachOdometry(this)
        attachGamepad(this)
        attachDriveTrain(this)
        attachTurret(this)
    }
}