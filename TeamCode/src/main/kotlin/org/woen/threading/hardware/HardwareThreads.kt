package org.woen.threading.hardware

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.woen.modules.driveTrain.DriveTrain
import org.woen.modules.camera.Camera
import org.woen.modules.driveTrain.odometry.Odometry
import org.woen.modules.runner.actions.ActionRunner
import org.woen.modules.runner.segment.SegmentsRunner

class HardwareThreads private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: HardwareThreads? = null

        private val _instanceMutex = Mutex()

        @JvmStatic
        val LAZY_INSTANCE: HardwareThreads
            get() =
                runBlocking {
                    _instanceMutex.withLock {
                        if (_nullableInstance == null) {
                            _nullableInstance = HardwareThreads()

                            _nullableInstance?.initModules()
                        }

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
    }

    val CONTROL = HardwareThread(HardwareLink())
    val EXPANSION = HardwareThread(HardwareLink())

    fun initModules() {
        CONTROL.link.addModules(DriveTrain(), Odometry(), SegmentsRunner())
        val actionRunner = ActionRunner()
    }

    override fun dispose() {
        CONTROL.dispose()
        EXPANSION.dispose()
    }
}