@file:Suppress("UNCHECKED_CAST")

package org.woen.modules

import kotlinx.coroutines.DisposableHandle

interface IModule: DisposableHandle {
    fun process()
    val isBusy: Boolean
}