package org.woen.modules

import kotlinx.coroutines.DisposableHandle
import org.woen.threading.IHardwareDevice

interface IModule <T>: DisposableHandle {
    fun process(data: T)
    fun getData(): T
    fun isBusy(): Boolean
    fun getDevices(): Array<IHardwareDevice>
}