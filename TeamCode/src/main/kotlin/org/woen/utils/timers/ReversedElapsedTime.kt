package org.woen.utils.timers

class ReversedElapsedTime(startTimeSeconds: Double = 0.0) : ElapsedTimeExtra() {
    private var _startTimeNanos = (startTimeSeconds * SECOND_IN_NANO).toLong()

    fun resetWithStartSeconds(time: Double) {
        _startTimeNanos = (time * SECOND_IN_NANO).toLong()
    }

    fun resetWithStartNanos(time: Long) {
        _startTimeNanos = time
    }

    fun resetWithStartMillis(time: Double) {
        _startTimeNanos = (time * MILLIS_IN_NANO).toLong()
    }

    override fun nanoseconds(): Long {
        return _startTimeNanos - super.nanoseconds()
    }
}