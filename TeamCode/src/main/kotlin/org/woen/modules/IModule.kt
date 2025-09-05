@file:Suppress("UNCHECKED_CAST")

package org.woen.modules

import kotlinx.coroutines.DisposableHandle

interface IModule <T>: DisposableHandle {
    fun process(data: T)
    fun getData(): T
    fun isBusy(): Boolean
}