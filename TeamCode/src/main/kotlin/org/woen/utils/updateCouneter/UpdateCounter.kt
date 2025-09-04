package org.woen.utils.updateCouneter

import com.qualcomm.robotcore.util.ElapsedTime

class UpdateCounter {
    private val _updateTimer = ElapsedTime()

    var currentUPS = 0.0 // update per second
        private set

    @Synchronized
    fun update(){
        currentUPS = 1.0 / _updateTimer.seconds()

        _updateTimer.reset()
    }
}