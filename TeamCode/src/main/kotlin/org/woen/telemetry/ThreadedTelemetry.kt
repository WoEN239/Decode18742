package org.woen.telemetry

import android.content.Context
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.canvas.Canvas
import com.acmerobotics.dashboard.config.ValueProvider
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.util.RobotLog
import kotlinx.coroutines.DisposableHandle
import org.firstinspires.ftc.ftccommon.external.OnCreate
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.woen.hotRun.HotRun
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

annotation class ThreadedConfig(val category: String, val name: String = "")

class ThreadedTelemetry private constructor(): DisposableHandle {
    companion object {
        var _nullableInstance: ThreadedTelemetry? = null

        val LAZY_INSTANCE: ThreadedTelemetry
            get() {
                if(_nullableInstance == null)
                    _nullableInstance = ThreadedTelemetry()

                return _nullableInstance!!
            }

        fun restart(){
            LAZY_INSTANCE.dispose()
            _nullableInstance = null
        }

        @OnCreate
        @JvmStatic
        fun start(context: Context?) {
            FtcDashboard.start(context)

            LAZY_INSTANCE.initConfigs()
        }
    }

    fun initConfigs(){
        val configs = ThreadedConfigs::class.declaredMemberProperties.filter {
            it.returnType.classifier == AtomicValueProvider::class &&
                    it.findAnnotation<ThreadedConfig>() != null
        }

        for (i in configs) {
            val value = i.get(ThreadedConfigs) as AtomicValueProvider<*>
            val annotation = i.annotations[0] as ThreadedConfig

            FtcDashboard.getInstance().addConfigVariable(
                annotation.category,
                annotation.name.ifEmpty { i.name }, value
            )
        }
    }

    private val _telemetrySenders = arrayListOf<(ThreadedTelemetry)-> Unit>()

    fun addTelemetrySender(sender: (ThreadedTelemetry)-> Unit){
        _telemetrySenders.add(sender)
    }

    private var _driverTelemetry: Telemetry? = null
    private var _dashboardPacket = TelemetryPacket()

    fun setDriveTelemetry(telemetry: Telemetry){
        _driverTelemetry = telemetry
    }

    private var _thread = thread(start = true) {
        while (!Thread.currentThread().isInterrupted) {
            if(HotRun.INSTANCE?.currentRunState?.get() == HotRun.RunState.RUN) {
                for (i in _telemetrySenders)
                    i.invoke(this)

                _driverTelemetry?.update()
                FtcDashboard.getInstance().sendTelemetryPacket(_dashboardPacket)

                _dashboardPacket = TelemetryPacket()
            }
            else{
                _driverTelemetry?.clearAll()

                _dashboardPacket = TelemetryPacket()
            }

            Thread.sleep((1000.0 / ThreadedConfigs.UPDATE_HZ.get()).toLong())
        }
    }

    fun addLines(vararg lines: String){
        if(Thread.currentThread() != _thread)
            return

        for(i in lines) {
            _driverTelemetry?.addLine(i)
            _dashboardPacket.addLine(i)
        }
    }

    fun addData(name: String, data: Any){
        if(Thread.currentThread() != _thread)
            return

        _driverTelemetry?.addData(name, data)
        _dashboardPacket.put(name, data)
    }

    private val _canvas: Canvas
        get() = _dashboardPacket.fieldOverlay()

    fun drawCircle(pos: Vec2, radius: Double, color: String) {
        if(Thread.currentThread() != _thread)
            return

        _canvas.setFill(color)
        _canvas.fillCircle(
            DistanceUnit.INCH.fromCm(pos.x),
            DistanceUnit.INCH.fromCm(pos.y),
            DistanceUnit.INCH.fromCm(radius)
        )
    }

    fun drawCircle(pos: Vec2, radius: Double, color: Color) =
        drawCircle(pos, radius, color.toString())

    fun drawPolygon(points: Array<Vec2>, color: String) {
        if(Thread.currentThread() != _thread)
            return

        val inchX = DoubleArray(points.size)
        val inchY = DoubleArray(points.size)

        for (i in points.indices) {
            inchX[i] = DistanceUnit.INCH.fromCm(points[i].x)
            inchY[i] = DistanceUnit.INCH.fromCm(points[i].y)
        }

        _canvas.setFill(color)
        _canvas.fillPolygon(inchX, inchY)
    }

    fun drawPolygon(points: Array<Vec2>, color: Color) = drawPolygon(points, color.toString())

    fun drawRect(center: Vec2, size: Vec2, rot: Double = 0.0, color: String) =
        drawPolygon(
            arrayOf(
                center + Vec2(-size.x / 2, size.y / 2).turn(rot),
                center + Vec2(size.x / 2, size.y / 2).turn(rot),
                center + Vec2(size.x / 2, -size.y / 2).turn(rot),
                center + Vec2(-size.x / 2, -size.y / 2).turn(rot)
            ), color
        )

    fun drawRect(center: Vec2, size: Vec2, rot: Double = 0.0, color: Color) =
        drawRect(center, size, rot, color.toString())

    override fun dispose() {
        _thread.interrupt()
    }

    class AtomicValueProvider<T>(default: T) : ValueProvider<T> {
        private var data = AtomicReference(default)

        override fun get(): T = data.get()

        override fun set(value: T?) {
            if (value != null)
                data.set(value)
        }
    }
}