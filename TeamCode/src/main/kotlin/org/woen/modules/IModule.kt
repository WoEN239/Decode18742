@file:Suppress("UNCHECKED_CAST")

package org.woen.modules

import kotlinx.coroutines.DisposableHandle

interface IModule : DisposableHandle {
    suspend fun process()
    val isBusy: Boolean
    fun opModeStart()
    fun opModeStop()
}