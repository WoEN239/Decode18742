package org.woen.modules

import com.qualcomm.hardware.limelightvision.Limelight3A
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.woen.collector.Collector
import org.woen.enumerators.BallRequest
import org.woen.enumerators.StockPattern
import org.woen.utils.units.Vec2
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation


class OnCameraUpdateEvent(val orientation: Orientation = Orientation.ZERO)
class OnPatternDetectedEvent(val pattern: Array<BallRequest.Name>)
class GetCurrentPatternEvent(var pattern: Array<BallColor>? = null)


fun attachLimelight(collector: Collector) {
    val ll = collector.hardwareMap.get(Limelight3A::class.java, "limelight")

    ll.start()

    var isPatternDetected = false
    var pattern: Array<BallColor> = arrayOf()
    var patternReq: Array<BallRequest.Name> = arrayOf()


    collector.eventBus.subscribe(GetCurrentPatternEvent::class){
        if(isPatternDetected)
            it.pattern = pattern
    }

    collector.updateEvent += {
        ll.pipelineSwitch(0)

        val result = ll.latestResult

        if (result.isValid) {
            if (!isPatternDetected) {
                val tagIds = result.fiducialResults.map { it.fiducialId }

                isPatternDetected = true

                for (id in tagIds) {
                    when (id) {
                        21 -> {
                            pattern = arrayOf(BallColor.GREEN, BallColor.PURPLE, BallColor.PURPLE)
                            patternReq = StockPattern.Request.GPP
                        }
                        22 -> {
                            pattern = arrayOf(BallColor.PURPLE, BallColor.GREEN, BallColor.PURPLE)
                            patternReq = StockPattern.Request.PGP
                        }
                        23 -> {
                            pattern = arrayOf(BallColor.PURPLE, BallColor.PURPLE, BallColor.GREEN)
                            patternReq = StockPattern.Request.PPG
                        }
                        else -> isPatternDetected = false
                    }
                }

                if (isPatternDetected)
                    collector.eventBus.invoke(
                        OnPatternDetectedEvent(patternReq))
            }

            val position = result.botpose.position
            val rotation = Angle(
                result.botpose.orientation.getYaw(
                    AngleUnit.RADIANS
            )   )

            collector.eventBus.invoke(
                OnCameraUpdateEvent(
                    Orientation(Vec2(position.x, position.y), rotation)
            )   )
        }

        ll.pipelineSwitch(1)
    }
}

