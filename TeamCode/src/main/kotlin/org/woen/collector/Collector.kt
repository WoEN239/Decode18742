package org.woen.collector

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.HardwareMap
import org.woen.modules.Battery
import org.woen.modules.Telemetry
import org.woen.modules.actions.attachActionRunner
import org.woen.modules.drivetrain.attachDriveTrain
import org.woen.modules.attachGamepad
import org.woen.modules.attachLedFeedback
import org.woen.modules.attachLimelight
import org.woen.modules.attachSimpleStorage
import org.woen.modules.drivetrain.attachOdometry
import org.woen.modules.attachTurret
import org.woen.modules.drivetrain.attachRunner
import org.woen.scoringSystem.ScoringModulesConnector
import org.woen.utils.events.EventBus
import org.woen.utils.events.SimpleEmptyEvent

enum class RunMode {
    AUTO,
    MANUAL
}

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

    val runMode: RunMode

    constructor(opMode: LinearOpMode, runMode: RunMode) {
        this.runMode = runMode
        this.opMode = opMode
        this.hardwareMap = opMode.hardwareMap

        stopEvent += {
            for (i in opMode.hardwareMap.servoController) i.pwmDisable()
        }

        telemetry = Telemetry(this)
        battery = Battery(this)
        attachGamepad(this)
        attachOdometry(this)
        attachDriveTrain(this)
        attachTurret(this)
        attachLimelight(this)

        if(runMode == RunMode.AUTO){
            attachRunner(this)
            attachActionRunner(this)
        }

        attachLedFeedback(this)
        attachSimpleStorage(this)
//        ScoringModulesConnector(this)
    }
}