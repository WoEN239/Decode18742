package org.woen.threading.hardware

import kotlinx.coroutines.DisposableHandle
import org.woen.modules.driveTrain.DriveTrain
import org.woen.modules.driveTrain.odometry.Odometry
import org.woen.modules.runner.segment.SegmentsRunner
import org.woen.modules.scoringSystem.brush.BrushSoft
import org.woen.modules.scoringSystem.turret.Turret
import org.woen.utils.smartMutex.SmartMutex

class HardwareThreads private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: HardwareThreads? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: HardwareThreads
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null) {
                    _nullableInstance = HardwareThreads()
                    _nullableInstance?.initModules()
                }

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance?.dispose()
                _nullableInstance = null
            }
        }
    }

    val CONTROL = HardwareThread(HardwareLink())

    val EXPANSION = HardwareThread(HardwareLink())

    override fun dispose() {
        CONTROL.dispose()
        EXPANSION.dispose()
    }

    fun initModules() {
//        CONTROL.link.addModules(Odometry(), DriveTrain(), SegmentsRunner())
        CONTROL.link.addModules(Odometry(), DriveTrain(), BrushSoft())
    }
}