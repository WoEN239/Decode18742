package org.woen.utils.updateCounter

import com.qualcomm.robotcore.util.ElapsedTime
import java.util.concurrent.atomic.AtomicReference

class UpdateCounter {
    private val _updateTimer = ElapsedTime()

    val currentUPS: Double
        get() = _atomicCurrentUPS.get()

    private val _atomicCurrentUPS = AtomicReference(0.0)

    @Synchronized
    fun update() {
        _atomicCurrentUPS.set(1.0 / _updateTimer.seconds())

        _updateTimer.reset()
    }
}