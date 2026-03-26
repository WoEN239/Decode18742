package org.woen.scoringSystem.misc


import kotlin.math.min
import org.woen.enumerators.BallRequest
import org.woen.scoringSystem.storage.MAX_BALL_COUNT



class DynamicPattern
{
    private var _permanentPattern = ArrayList<BallRequest>()
    private var _dynamicOffset: Int = 0



    fun resetPermanent() = _permanentPattern.clear()
    fun resetOffset()
    {
        _dynamicOffset = 0
    }

    fun setPermanent(permanent: Array<BallRequest>)
    {
        _permanentPattern = ArrayList(permanent.toList())
    }


    fun addToOffset()
    {
        _dynamicOffset = (_dynamicOffset + 1) % MAX_BALL_COUNT
    }
    fun removeFromOffset()
    {
        _dynamicOffset = (_dynamicOffset - 1 + MAX_BALL_COUNT) % MAX_BALL_COUNT
    }



    fun offset() = _dynamicOffset
    fun permanent() = _permanentPattern.toTypedArray()
    fun permanentWasDetected() = _permanentPattern.isNotEmpty()



    companion object
    {
        @JvmStatic
        fun trimPattern(pattern: Array<BallRequest>,
                        offset: Int): Array<BallRequest>
        {
            if (pattern.isEmpty()) return Array(MAX_BALL_COUNT) { BallRequest.NONE }
            val newPattern = Array(
                min(MAX_BALL_COUNT, pattern.size))
                    { BallRequest.NONE }

            var curRequestId = 0
            while (curRequestId < pattern.size)
            {
                newPattern[curRequestId] = pattern[(curRequestId + offset) % pattern.size]

                curRequestId++
            }

            return newPattern
        }
    }
}