package org.woen.modules

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.util.RobotLog
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.woen.collector.Collector
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2

class Telemetry {
    private val _collector: Collector
    private val _dashboard: FtcDashboard

    private var _telemetryPacket = TelemetryPacket()

    private val _canvas
        get() = _telemetryPacket.fieldOverlay()

    constructor(collector: Collector) {
        _collector = collector
        _dashboard = FtcDashboard.getInstance()

        collector.updateEvent += {
            _dashboard.sendTelemetryPacket(_telemetryPacket)
            _telemetryPacket = TelemetryPacket()
        }
    }

    fun addData(name: String, data: Any) {
        _collector.opMode.telemetry.addData(name, data)
        _telemetryPacket.put(name, data)
    }

    fun addLine(line: String) {
        _collector.opMode.telemetry.addLine(line)
        _telemetryPacket.addLine(line)
    }

    fun drawCircle(pos: Vec2, radius: Double, color: String) {
        _canvas.setFill(color)
        _canvas.fillCircle(
            DistanceUnit.INCH.fromMeters(pos.x),
            DistanceUnit.INCH.fromMeters(pos.y),
            DistanceUnit.INCH.fromMeters(radius)
        )
    }

    fun drawCircle(pos: Vec2, radius: Double, color: Color) =
        drawCircle(pos, radius, color.toString())

    fun drawPolygon(points: Array<Vec2>, color: String) {
        val inchX = DoubleArray(points.size)
        val inchY = DoubleArray(points.size)

        for (i in points.indices) {
            inchX[i] = DistanceUnit.INCH.fromMeters(points[i].x)
            inchY[i] = DistanceUnit.INCH.fromMeters(points[i].y)
        }

        _canvas.setFill(color)
        _canvas.fillPolygon(inchX, inchY)
    }

    fun drawPolygon(points: Array<Vec2>, color: Color) = drawPolygon(points, color.toString())

    fun drawRect(center: Vec2, size: Vec2, rot: Double = 0.0, color: String) = drawPolygon(
        arrayOf(
            center + Vec2(-size.x / 2, size.y / 2).turn(rot),
            center + Vec2(size.x / 2, size.y / 2).turn(rot),
            center + Vec2(size.x / 2, -size.y / 2).turn(rot),
            center + Vec2(-size.x / 2, -size.y / 2).turn(rot)
        ), color
    )

    fun drawRect(center: Vec2, size: Vec2, rot: Double = 0.0, color: Color) =
        drawRect(center, size, rot, color.toString())

    fun log(msg: String) = logWithTag(msg, "18742robot")

    fun log(vararg msg: String) {
        for (s in msg) logWithTag(s, "18742robot")
    }

    fun logWithTag(str: String, tag: String) =
        RobotLog.dd(tag, str)
}