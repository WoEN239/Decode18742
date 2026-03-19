package org.woen.modules

import com.qualcomm.hardware.limelightvision.LLResult
import com.qualcomm.hardware.limelightvision.Limelight3A
import org.woen.collector.Collector
import org.woen.utils.units.Orientation

class OnCameraUpdateEvent(val orientation: Orientation)

fun attachLimelight(collector: Collector) {
    val telem = collector.telemetry
    var odometry: GetRobotOdometry


    val ll = collector.hardwareMap.get(Limelight3A::class.java, "limelight")
    ll.start()
    val ll_to_arr: DoubleArray = DoubleArray(8)
    var orient_by_ll: ll.Position

    var results: LLResult

    collector.updateEvent += {
        odometry = collector.eventBus.invoke(GetRobotOdometry())

        ll.pipelineSwitch(0)
        results = ll.latestResult
        orient_by_ll = results.botpose.position

        collector.eventBus.invoke(OnCameraUpdateEvent(orient_by_ll))

        ll.pipelineSwitch(1)

        ll_to_arr.set(0,odometry.orientation.x)
        ll_to_arr.set(1,odometry.orientation.y)
        ll_to_arr.set(2,odometry.orientation.angle)

        ll.updatePythonInputs(ll_to_arr)

        results = ll.latestResult
        var x = results.pythonOutput[0]
        var y = results.pythonOutput[1]
        var angl = results.pythonOutput[2]


        telem.addData("x",x)
        telem.addData("y",y)
        telem.addData("angl",angl)
    }
}

