package org.woen.modules

import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.Collector
import org.woen.collector.GameColor
import org.woen.collector.GameSettings
import org.woen.collector.RunMode
import org.woen.utils.drivers.LEDLine
import kotlin.math.sin

class BallCountUpdateEvent(var ballsCount: Int)

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

    val timer = ElapsedTime()
    timer.reset()
    var prevTime: Double = timer.milliseconds()

    var sinStep: Double = 0.00
    var powerValue: Double = 0.00
    var currentBallsCount: Int = 0

    var sinStepL: Double = 0.00
    var sinStepR: Double = 3.14
    var powerValueL: Double = 0.00
    var powerValueR: Double = 0.00
    val color = GameSettings.startOrientation.gameColor

    collector.initUpdateEvent += {
        when(color) {
            GameColor.RED -> {
                if (timer.milliseconds() - prevTime > 0.02) {
                    if(sinStepL>6.282){
                        sinStepL = 0.000
                    }
                    else {
                        sinStepL += 0.0125
                    }

                    if(sinStepR>6.282){
                        sinStepR = 0.000
                    }
                    else {
                        sinStepR += 0.0125
                    }

                    powerValueL = (sin(sinStepL)+1)/2
                    powerValueR = (sin(sinStepR)+1)/2

                    ledLeftR.power = powerValueL
                    ledLeftG.power = 0.0
                    ledLeftB.power = 0.0

                    ledRightR.power = powerValueR
                    ledRightG.power = 0.0
                    ledRightB.power = 0.0

                    prevTime = timer.milliseconds()
                }
            }
            GameColor.BLUE -> {
                if (timer.milliseconds() - prevTime > 0.02) {
                    if(sinStepL>6.282){
                        sinStepL = 0.000
                    }
                    else {
                        sinStepL += 0.0125
                    }

                    if(sinStepR>6.282){
                        sinStepR = 0.000
                    }
                    else {
                        sinStepR += 0.0125
                    }

                    powerValueL = (sin(sinStepL)+1)/2
                    powerValueR = (sin(sinStepR)+1)/2

                    ledLeftR.power = 0.0
                    ledLeftG.power = 0.0
                    ledLeftB.power = powerValueL

                    ledRightR.power = 0.0
                    ledRightG.power = 0.0
                    ledRightB.power = powerValueR

                    prevTime = timer.milliseconds()
                }
            }
        }
    }

    collector.startEvent += {
        collector.eventBus.invoke(BallCountUpdateEvent(0))
    }

    collector.eventBus.subscribe(BallCountUpdateEvent::class) {
        currentBallsCount = it.ballsCount
        prevTime = timer.milliseconds()
    }

    collector.updateEvent += {
        if(collector.runMode == RunMode.MANUAL) {
            if (timer.milliseconds() - prevTime > 0.01) {
                when (currentBallsCount) {
                    0 -> {
                        if (timer.milliseconds() - prevTime > 0.02) {
                            if(sinStep>6.282){
                                sinStep = 0.000
                            }
                            else {
                                sinStep += 0.25
                            }

                            powerValue = (sin(sinStep)+1)/2

                            ledLeftR.power = powerValue/2+0.5
                            ledLeftG.power = 0.0
                            ledLeftB.power = 0.0

                            ledRightR.power = powerValue/2+0.5
                            ledRightG.power = 0.0
                            ledRightB.power = 0.0

                            prevTime = timer.milliseconds()
                        }
                    }

                    1 -> {
                        if (timer.milliseconds() - prevTime > 0.02) {
                            if(sinStep>6.282){
                                sinStep = 0.000
                            }
                            else {
                                sinStep += 0.25
                            }

                            powerValue = (sin(sinStep)+1)/2

                            ledLeftR.power = powerValue/2+0.5
                            ledLeftG.power = (powerValue/2+0.5)/8
                            ledLeftB.power = 0.0

                            ledRightR.power = powerValue/2+0.5
                            ledRightG.power = (powerValue/2+0.5)/8
                            ledRightB.power = 0.0

                            prevTime = timer.milliseconds()
                        }
                    }

                    2 -> {
                        if (timer.milliseconds() - prevTime > 0.02) {
                            if(sinStep>6.282){
                                sinStep = 0.000
                            }
                            else {
                                sinStep += 0.25
                            }

                            powerValue = (sin(sinStep)+1)/2

                            ledLeftR.power = powerValue/2+0.5
                            ledLeftG.power = (powerValue/2+0.5)/4
                            ledLeftB.power = 0.0

                            ledRightR.power = powerValue/2+0.5
                            ledRightG.power = (powerValue/2+0.5)/4
                            ledRightB.power = 0.0

                            prevTime = timer.milliseconds()
                        }
                    }

                    3 -> {
                        if (timer.milliseconds() - prevTime > 0.02) {
                            if(sinStep>6.282){
                                sinStep = 0.000
                            }
                            else {
                                sinStep += 0.5
                            }

                            powerValue = (sin(sinStep)+1)/2

                            ledLeftR.power = 0.0
                            ledLeftG.power = powerValue/2+0.5
                            ledLeftB.power = 0.0

                            ledRightR.power = 0.0
                            ledRightG.power = powerValue/2+0.5
                            ledRightB.power = 0.0

                            prevTime = timer.milliseconds()
                        }
                    }
                }
            }
        }

        else if(collector.runMode == RunMode.AUTO) {
            if (timer.milliseconds() - prevTime > 0.01) {
                when (color) {
                    GameColor.RED -> {
                        if(sinStep>6.282){
                            sinStep = 0.000
                        }
                        else {
                            sinStep += 0.25
                        }

                        powerValue = (sin(sinStep)+1)/2

                        ledLeftR.power = powerValue
                        ledLeftG.power = 0.0
                        ledLeftB.power = 0.0

                        ledRightR.power = powerValue
                        ledRightG.power = 0.0
                        ledRightB.power = 0.0
                    }

                    GameColor.BLUE -> {
                        if(sinStep>6.282){
                            sinStep = 0.000
                        }
                        else {
                            sinStep += 0.25
                        }

                        powerValue = (sin(sinStep)+1)/2

                        ledLeftR.power = 0.0
                        ledLeftG.power = 0.0
                        ledLeftB.power = powerValue

                        ledRightR.power = 0.0
                        ledRightG.power = 0.0
                        ledRightB.power = powerValue
                    }
                }

                prevTime = timer.milliseconds()
            }
        }

    }
}