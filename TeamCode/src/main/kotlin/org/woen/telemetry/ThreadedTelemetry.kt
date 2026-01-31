package org.woen.telemetry

import android.content.Context
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.canvas.Canvas
import com.acmerobotics.dashboard.config.ValueProvider
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.util.RobotLog
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import org.firstinspires.ftc.ftccommon.external.OnCreate
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.woen.threading.ThreadManager
import org.woen.utils.events.SimpleEvent
import org.woen.utils.smartMutex.SmartMutex
import org.woen.utils.timers.ReversedElapsedTime
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2
import kotlin.concurrent.thread
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

annotation class EventConfig()

class ThreadedTelemetry : DisposableHandle {
    companion object {
        private var _nullableInstance: ThreadedTelemetry? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: ThreadedTelemetry
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null) _nullableInstance = ThreadedTelemetry()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance?.dispose()
                _nullableInstance = null
            }
        }

        @OnCreate
        @JvmStatic
        fun start(context: Context?) {
            FtcDashboard.start(context)
            LAZY_INSTANCE.initConfigs()
        }
    }

    fun initConfigs() {
        FtcDashboard.getInstance().withConfigRoot {
            Configs::class.nestedClasses.forEach { jt ->
                val objClass = jt.objectInstance

                if (objClass != null) {
                    jt.declaredMemberProperties.forEach { zt ->
                        if (zt.findAnnotation<EventConfig>() != null && zt.returnType.classifier == EventValueProvider::class) {
                            val value = zt.call(objClass) as EventValueProvider<*>

                            FtcDashboard.getInstance()
                                .addConfigVariable(jt.simpleName, zt.name, value)
                        }
                    }
                }
            }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun onUpdateHZChanged(value: Int) {
        val dash = FtcDashboard.getInstance()

        dash.telemetryTransmissionInterval = 1000 / value
    }

    override fun dispose() {
        Configs.TELEMETRY.TELEMETRY_UPDATE_HZ.onSet -= ::onUpdateHZChanged
    }

    private var _temporarySenders =
        mutableSetOf<Pair<ReversedElapsedTime, (ThreadedTelemetry) -> Unit>>()

    fun startTemporarySender(
        timer: ReversedElapsedTime, sender: (ThreadedTelemetry) -> Unit
    ) = _temporarySendersMutex.smartLock {
        _temporarySenders.add(
            Pair(
                timer, sender
            )
        )
    }

    private val _temporarySendersMutex = SmartMutex()

    val onTelemetrySend = SimpleEvent<ThreadedTelemetry>()

    private var _driverTelemetry: Telemetry? = null
    private var _dashboardPacket = TelemetryPacket()

    fun setDriveTelemetry(telemetry: Telemetry) {
        _driveTelemetryMutex.smartLock {
            _driverTelemetry = telemetry
        }
    }

    private val _driveTelemetryMutex = SmartMutex()

    var telemetryEnabled = false

    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = true) {
        while (!Thread.currentThread().isInterrupted) {
            if (telemetryEnabled && Configs.TELEMETRY.TELEMETRY_ENABLE) {
                onTelemetrySend.invoke(this)

                val telemetry = this

                _temporarySendersMutex.smartLock {
                    for (i in _temporarySenders) i.second.invoke(telemetry)

                    _temporarySenders = _temporarySenders.filter {
                        it.first.seconds() > 0.0
                    }.toMutableSet()
                }

                _dashboardPacket.addLine("\n")
                drawRect(Vec2.ZERO, Vec2.ZERO, 0.0, Color.RED)

                _driveTelemetryMutex.smartLock {
                    _driverTelemetry?.update()
                }

                FtcDashboard.getInstance().sendTelemetryPacket(_dashboardPacket)
            } else
                _driveTelemetryMutex.smartLock {
                    _driverTelemetry?.clear()
                }

            _dashboardPacket = TelemetryPacket()

            Thread.sleep((1000.0 / Configs.TELEMETRY.TELEMETRY_UPDATE_HZ.get()).toLong())
        }
    })

    @OptIn(InternalCoroutinesApi::class)
    fun addLines(vararg lines: String) {
        for (i in lines) {
            _driveTelemetryMutex.smartLock {
                _driverTelemetry?.addLine(i)
            }

            _dashboardPacket.addLine(i)

            logWithTag(i, "18742telemetry")
        }
    }

    fun addLine(line: String) = addLines(line)

    @OptIn(InternalCoroutinesApi::class)
    fun addData(name: String, data: Any) {
        _driveTelemetryMutex.smartLock {
            _driverTelemetry?.addData(name, data)
        }

        logWithTag("$name: $data", "18742telemetry")
        _dashboardPacket.put(name, data)
    }

    private val _canvas: Canvas
        get() = _dashboardPacket.fieldOverlay()

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
        RobotLog.dd(tag, "robot[" + Thread.currentThread().name + "]: " + str)

    class EventValueProvider<T>(private var _value: T) : ValueProvider<T> {
        val onSet = SimpleEvent<T>()

        override fun get() = _value

        override fun set(value: T?) {
            if (value != null) {
                _value = value
                onSet(value)
            }
        }
    }

    private constructor() {
        Configs.TELEMETRY.TELEMETRY_UPDATE_HZ.onSet += ::onUpdateHZChanged

        onUpdateHZChanged(Configs.TELEMETRY.TELEMETRY_UPDATE_HZ.get())
    }
}