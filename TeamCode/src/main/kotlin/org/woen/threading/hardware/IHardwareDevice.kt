package org.woen.threading.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import kotlinx.coroutines.DisposableHandle

interface IHardwareDevice : DisposableHandle {
    fun update()
    fun init(hardwareMap: HardwareMap)
    fun opModeStart()
    fun opModeStop()
}