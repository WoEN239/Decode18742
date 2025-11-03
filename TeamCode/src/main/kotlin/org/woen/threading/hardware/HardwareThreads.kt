package org.woen.threading.hardware

import kotlinx.coroutines.DisposableHandle
import org.woen.modules.driveTrain.DriveTrain
import org.woen.modules.driveTrain.odometry.Odometry
import org.woen.modules.scoringSystem.brush.Brush
import org.woen.utils.smartMutex.SmartMutex

class HardwareThreads : DisposableHandle {
    companion object {
        private var _nullableInstance: HardwareThreads? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: HardwareThreads
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null)
                    _nullableInstance = HardwareThreads()

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

    private constructor() {
//        CONTROL.link.addModules(Odometry(), DriveTrain(), SegmentsRunner())
        CONTROL.link.addModules(Odometry(), DriveTrain(), Brush())
    }
}