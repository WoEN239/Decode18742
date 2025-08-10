package org.woen.threading

import kotlinx.coroutines.DisposableHandle

interface IHardwareDevice: DisposableHandle {
    fun update()
    fun init()
}