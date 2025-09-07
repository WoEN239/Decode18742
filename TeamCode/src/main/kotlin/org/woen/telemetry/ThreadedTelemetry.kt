package org.woen.telemetry

import android.content.Context
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.canvas.Canvas
import com.acmerobotics.dashboard.config.ValueProvider
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.util.RobotLog
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.firstinspires.ftc.ftccommon.external.OnCreate
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.woen.hotRun.HotRun
import org.woen.threading.ThreadManager
import org.woen.utils.events.SimpleEvent
import org.woen.utils.timers.ReversedElapsedTime
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation


annotation class ThreadedConfig(val category: String, val name: String = "")

class ThreadedTelemetry private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: ThreadedTelemetry? = null

        private val _instanceMutex = Mutex()

        val LAZY_INSTANCE: ThreadedTelemetry
            get() = runBlocking {
                _instanceMutex.withLock {
                    if (_nullableInstance == null)
                        _nullableInstance = ThreadedTelemetry()

                    return@withLock _nullableInstance!!
                }
            }

        fun restart() {
            runBlocking {
                _instanceMutex.withLock {
                    _nullableInstance?.dispose()
                    _nullableInstance = null
                }
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
        val configs = ThreadedConfigs::class.declaredMemberProperties.filter {
            (it.returnType.classifier == AtomicValueProvider::class || it.returnType.classifier == AtomicEventProvider::class) &&
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

    @OptIn(InternalCoroutinesApi::class)
    private fun onUpdateHZChanged(value: Int) {
        val dash = FtcDashboard.getInstance()

        dash.telemetryTransmissionInterval = 1000 / value
    }

    override fun dispose() {
        ThreadedConfigs.TELEMETRY_UPDATE_HZ.onSet -= ::onUpdateHZChanged
    }

    private var _temporarySenders =
        mutableSetOf<Pair<ReversedElapsedTime, (ThreadedTelemetry) -> Unit>>()

    fun startTemporarySender(
        timer: ReversedElapsedTime,
        sender: (ThreadedTelemetry) -> Unit
    ) = runBlocking {
        _temporarySendersMutex.withLock {
            _temporarySenders.add(
                Pair(
                    timer,
                    sender
                )
            )
        }
    }

    private val _temporarySendersMutex = Mutex()

    val onTelemetrySend = SimpleEvent<ThreadedTelemetry>()

    private var _driverTelemetry: Telemetry? = null
    private var _dashboardPacket = TelemetryPacket()

    fun setDriveTelemetry(telemetry: Telemetry) {
        runBlocking {
            _driveTelemetryMutex.withLock {
                _driverTelemetry = telemetry
            }
        }
    }

    private val _driveTelemetryMutex = Mutex()

    @OptIn(InternalCoroutinesApi::class)
    private val _thread = ThreadManager.LAZY_INSTANCE.register(thread(start = true) {
        while (!Thread.currentThread().isInterrupted) {
            if (HotRun.LAZY_INSTANCE.currentRunState.get() != HotRun.RunState.STOP && ThreadedConfigs.TELEMETRY_ENABLE.get()) {
                onTelemetrySend.invoke(this)

                val telemetry = this

                runBlocking {
                    _temporarySendersMutex.withLock {
                        for (i in _temporarySenders)
                            i.second.invoke(telemetry)

                        _temporarySenders =
                            _temporarySenders.filter {
                                it.first.seconds() > 0.0
                            }.toMutableSet()
                    }
                }

                _dashboardPacket.addLine("\n")
                drawRect(Vec2.ZERO, Vec2.ZERO, 0.0, Color.RED)

                runBlocking {
                    _driveTelemetryMutex.withLock {
                        _driverTelemetry?.update()
                    }
                }

                FtcDashboard.getInstance().sendTelemetryPacket(_dashboardPacket)

                _dashboardPacket = TelemetryPacket()
            } else {
                runBlocking {
                    _driveTelemetryMutex.withLock {
                        _driverTelemetry?.clearAll()
                    }
                }
            }

            _dashboardPacket = TelemetryPacket()

            Thread.sleep((1000.0 / ThreadedConfigs.TELEMETRY_UPDATE_HZ.get()).toLong())
        }
    })

    init {
        ThreadedConfigs.TELEMETRY_UPDATE_HZ.onSet += ::onUpdateHZChanged

        onUpdateHZChanged(ThreadedConfigs.TELEMETRY_UPDATE_HZ.get())
    }

    @OptIn(InternalCoroutinesApi::class)
    fun addLines(vararg lines: String) {
        if (Thread.currentThread() != _thread)
            return

        for (i in lines) {
            runBlocking {
                _driveTelemetryMutex.withLock {
                    _driverTelemetry?.addLine(i)
                }
            }

            _dashboardPacket.addLine(i)

            logWithTag(i, "18742telemetry")
        }
    }

    fun addLine(line: String) = addLines(line)

    @OptIn(InternalCoroutinesApi::class)
    fun addData(name: String, data: Any) {
        if (Thread.currentThread() != _thread)
            return

        runBlocking {
            _driveTelemetryMutex.withLock {
                _driverTelemetry?.addData(name, data)
            }
        }

        logWithTag("$name: $data", "18742telemetry")
        _dashboardPacket.put(name, data)
    }

    private val _canvas: Canvas
        get() = _dashboardPacket.fieldOverlay()

    fun drawCircle(pos: Vec2, radius: Double, color: String) {
        if (Thread.currentThread() != _thread)
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
        if (Thread.currentThread() != _thread)
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

    fun log(vararg strs: String) {
        for (i in strs) {
            logWithTag(i, "18742robot")
        }
    }

    private fun logWithTag(str: String, tag: String) =
        RobotLog.dd(tag, "robot[" + Thread.currentThread().name + "]: " + str)

    open class AtomicValueProvider<T>(default: T) : ValueProvider<T> {
        private var data = AtomicReference(default)

        override fun get(): T = data.get()

        override fun set(value: T?) {
            if (value != null)
                data.set(value)
        }
    }

    class AtomicEventProvider<T>(default: T) : AtomicValueProvider<T>(default) {
        val onSet = SimpleEvent<T>()

        override fun set(value: T?) {
            super.set(value)

            if (value != null)
                onSet(value)
        }
    }
}