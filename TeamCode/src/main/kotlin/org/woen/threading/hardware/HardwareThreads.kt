package org.woen.threading.hardware


import kotlinx.coroutines.DisposableHandle
import org.woen.modules.camera.Camera
import org.woen.utils.smartMutex.SmartMutex

import org.woen.modules.driveTrain.Odometry
import org.woen.modules.driveTrain.DriveTrain
import org.woen.modules.light.Light

import org.woen.modules.runner.actions.ActionRunner
import org.woen.modules.runner.segment.SegmentsRunner

import org.woen.modules.scoringSystem.brush.Brush
import org.woen.modules.scoringSystem.turret.Turret
import org.woen.modules.scoringSystem.simple.SimpleStorage

import org.woen.modules.scoringSystem.SortingAutoLogic
import org.woen.modules.scoringSystem.ScoringModulesConnector



class HardwareThreads private constructor() : DisposableHandle {
    companion object {
        private var _nullableInstance: HardwareThreads? = null

        private val _instanceMutex = SmartMutex()

        @JvmStatic
        val LAZY_INSTANCE: HardwareThreads
            get() {
                var isInitialised = false

                _instanceMutex.smartLock {
                    if (_nullableInstance == null) {
                        _nullableInstance = HardwareThreads()

                        isInitialised = true
                    }
                }

                if (isInitialised)
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

    val CONTROL = HardwareThread("Control Hub")
    val EXPANSION = HardwareThread("Expansion Hub")

    override fun dispose() {
        CONTROL.dispose()
        EXPANSION.dispose()
        ActionRunner.restart()
    }

    private fun initModules() {
        CONTROL.link.addModules(Odometry(), DriveTrain(), SegmentsRunner(), Light())
        ActionRunner.LAZY_INSTANCE
        Camera.LAZY_INSTANCE
        
        
//        EXPANSION.link.addModules(Turret(), Brush(), SimpleStorage())
        
        EXPANSION.link.addModules(Turret(), Brush())
        ScoringModulesConnector()
        SortingAutoLogic()
    }
}