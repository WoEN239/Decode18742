package org.woen.modules

import org.woen.threading.IHardwareDevice

data class A(var a: Int = 5)

class TestModule: IModule<A> {
    override fun process(data: A) {

    }

    override fun getData(): A {
        val data = A()

        data.a = 5

        return data
    }

    override fun isBusy(): Boolean {
        return false
    }

    override fun getDevices(): Array<IHardwareDevice> {
        return arrayOf()
    }

    override fun dispose() {

    }
}