package org.woen.modules

import com.qualcomm.hardware.limelightvision.LLResult
import com.qualcomm.hardware.limelightvision.LLResultTypes
import com.qualcomm.hardware.limelightvision.Limelight3A
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import org.woen.collector.Collector
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

class OnCameraUpdateEvent(val orientation: Orientation)

fun attachLimelight(collector: Collector) {
    val telemetry = collector.telemetry
    var odometry: GetRobotOdometry
    val ll = collector.hardwareMap.get(Limelight3A::class.java, "limelight")

    ll.start()

    var posByLL: Position
    var orientByLL: YawPitchRollAngles
    var results: LLResult
    var tagIds: List<Int>
    var isPatternScanned: Boolean = false

    var obeliskTagId: Int

    collector.updateEvent += {
        odometry = collector.eventBus.invoke(GetRobotOdometry())

        ll.pipelineSwitch(0)

        results = ll.latestResult
        tagIds = results.fiducialResults.map { it.fiducialId }

        if (!isPatternScanned) {
            tagIds = results.fiducialResults.map { it.fiducialId }
            for (id in tagIds) {
                if (id in 21..23) {
                    isPatternScanned = true
                    obeliskTagId = id
                    telemetry.addData("obeliski", id)
                    break
                }
            }
        }

        posByLL = results.botpose.position
        orientByLL = results.botpose.orientation

        collector.eventBus.invoke(
            OnCameraUpdateEvent(
                Orientation(
                    Vec2(posByLL.x, posByLL.y), Angle(
                        orientByLL.getYaw(
                            AngleUnit.RADIANS
                        )
                    )
                )
            )
        )



        ll.pipelineSwitch(1)
    }
}

