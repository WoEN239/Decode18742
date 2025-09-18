package org.woen.threading.hardware

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.modules.driveTrain.DriveTrain
import org.woen.modules.camera.Camera
import org.woen.modules.driveTrain.odometry.Odometry
import org.woen.modules.driveTrain.odometry.RequireOdometryEvent
import org.woen.modules.runner.actions.ActionRunner
import org.woen.modules.runner.segment.SegmentsRunner
import org.woen.telemetry.ThreadedTelemetry
import org.woen.threading.ThreadedEventBus
import org.woen.utils.units.Color
import org.woen.utils.units.Vec2

class HardwareThreads private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: HardwareThreads? = null

        private val _instanceMutex = Mutex()

        @JvmStatic
        val LAZY_INSTANCE: HardwareThreads
            get() =
                runBlocking {
                    val isCreated: Boolean

                    _instanceMutex.withLock {
                        if (_nullableInstance == null) {
                            _nullableInstance = HardwareThreads()

                            isCreated = true
                        }
                        else
                            isCreated = false
                    }

                    if(isCreated)
                        _nullableInstance?.initModules()

                    return@runBlocking _nullableInstance!!
                }


        fun restart() {
            runBlocking {
                _instanceMutex.withLock {
                    _nullableInstance?.dispose()
                    _nullableInstance = null
                }
            }
        }
    }

    val CONTROL = HardwareThread(HardwareLink())

    val EXPANSION = HardwareThread(HardwareLink())

    fun initModules() {
        CONTROL.link.addModules(Odometry())
    }

    override fun dispose() {
        CONTROL.dispose()
        EXPANSION.dispose()
    }
}