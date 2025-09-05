package org.woen.threading.exponentialFilter

import com.qualcomm.robotcore.util.ElapsedTime

class ExponentialFilter(var coef: Double) {
    private val _deltaTime = ElapsedTime()

    @Synchronized
    fun start() {
        _deltaTime.reset()
    }

    @Synchronized
    fun updateRaw(value: Double, delta: Double): Double {
        val result = value + delta * (_deltaTime.seconds() / (coef + _deltaTime.seconds()))

        _deltaTime.reset()

        return result
    }

    @Synchronized
    fun update(val1: Double, val2: Double): Double {
        return updateRaw(val1, val2 - val1)
    }
}