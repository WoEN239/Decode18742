package org.woen.utils.timers

import com.qualcomm.robotcore.util.ElapsedTime

open class ElapsedTimeExtra : ElapsedTime() {
    fun pause() {
        if (_isPause) return

        _nsPauseStart = nanoseconds()

        _isPause = true

        _pauseTime.reset()
    }

    fun start() {
        if (!_isPause) return

        _isPause = false

        nsStartTime += _pauseTime.nanoseconds()
    }

    override fun nanoseconds(): Long {
        if (_isPause) return _nsPauseStart

        return super.nanoseconds()
    }

    private val _pauseTime = ElapsedTime()
    private var _nsPauseStart: Long = 0
    private var _isPause = false

    fun isPause(): Boolean {
        return _isPause
    }
}