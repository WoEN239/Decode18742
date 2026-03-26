package org.woen.modules

import org.woen.collector.Collector
import org.woen.utils.drivers.LEDLine

class NewBallArrivedEvent(var ballsCount: Int)

fun attachLedFeedback(collector: Collector) {
    val ledLeftR = LEDLine("leftR", collector.hardwareMap)
    val ledLeftG = LEDLine("leftG", collector.hardwareMap)
    val ledLeftB = LEDLine("leftB", collector.hardwareMap)

    val ledRightR = LEDLine("rightR", collector.hardwareMap)
    val ledRightG = LEDLine("rightG", collector.hardwareMap)
    val ledRightB = LEDLine("rightB", collector.hardwareMap)

    ledLeftR.power = 0.0
    ledLeftG.power = 0.0
    ledLeftB.power = 0.0

    ledRightR.power = 0.0
    ledRightG.power = 0.0
    ledRightB.power = 0.0

    collector.startEvent += {
        collector.eventBus.invoke(NewBallArrivedEvent(0))
    }

    collector.eventBus.subscribe(NewBallArrivedEvent::class){
        when(it.ballsCount) {
            0 -> {
                ledLeftR.power = 1.0
                ledLeftG.power = 0.0
                ledLeftB.power = 0.0

                ledRightR.power = 1.0
                ledRightG.power = 0.0
                ledRightB.power = 0.0
            }
            1 -> {
                ledLeftR.power = 1.0
                ledLeftG.power = 0.125
                ledLeftB.power = 0.0

                ledRightR.power = 1.0
                ledRightG.power = 0.125
                ledRightB.power = 0.0
            }
            2 -> {
                ledLeftR.power = 1.0
                ledLeftG.power = 0.25
                ledLeftB.power = 0.0

                ledRightR.power = 1.0
                ledRightG.power = 0.25
                ledRightB.power = 0.0
            }

            3 -> {
                ledLeftR.power = 0.0
                ledLeftG.power = 1.0
                ledLeftB.power = 0.0

                ledRightR.power = 0.0
                ledRightG.power = 1.0
                ledRightB.power = 0.0
            }

        }
    }
}