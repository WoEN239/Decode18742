package org.woen.threading

import com.qualcomm.robotcore.hardware.HardwareMap
import kotlinx.coroutines.DisposableHandle
import org.firstinspires.ftc.ftccommon.external.OnCreate
import org.woen.modules.TestModule

class HardwareThreads private constructor(): DisposableHandle {
    companion object{
        private var _nullableInstance: HardwareThreads? = null

        val LAZY_INSTANCE : HardwareThreads
            get() {
                if(_nullableInstance == null)
                    _nullableInstance = HardwareThreads()

                return _nullableInstance!!
            }

        fun restart(){
            _nullableInstance?.dispose()
            _nullableInstance = null
        }
    }

    val CONTROL = HardwareThread(HardwareLink(arrayOf(TestModule())))
    val EXPANSION = HardwareThread(HardwareLink(arrayOf()))

    override fun dispose() {
        CONTROL.dispose()
        EXPANSION.dispose()
    }
}