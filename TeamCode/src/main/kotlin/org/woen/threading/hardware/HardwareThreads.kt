package org.woen.threading.hardware


import org.woen.utils.smartMutex.SmartMutex
import kotlinx.coroutines.DisposableHandle

import org.woen.modules.driveTrain.DriveTrain
import org.woen.modules.driveTrain.odometry.Odometry

import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.turret.Turret

import org.woen.modules.scoringSystem.simple.SimpleStorage
import org.woen.modules.scoringSystem.ScoringModulesConnector



class HardwareThreads private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: HardwareThreads? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: HardwareThreads
            get() {
                var isInited = false

                _instanceMutex.smartLock {
                    if (_nullableInstance == null) {
                        _nullableInstance = HardwareThreads()

                        isInited = true
                    }
                }

                if (isInited)
                    _nullableInstance?.initModules()

                return _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance?.dispose()
                _nullableInstance = null
            }
        }
    }

    val CONTROL = HardwareThread()
//    val EXPANSION = HardwareThread()
//    val COLOR_SENSORS = HardwareThread()

    override fun dispose() {
        CONTROL.dispose()
//        EXPANSION.dispose()
//        COLOR_SENSORS.dispose()
    }

    private fun initModules() {
//        CONTROL.link.addModules(Odometry(), DriveTrain(), SegmentsRunner())
        CONTROL.link.addModules(Odometry(), DriveTrain(), Brush(), Turret(), /*SimpleStorage()*/)

        ScoringModulesConnector()
    }
}