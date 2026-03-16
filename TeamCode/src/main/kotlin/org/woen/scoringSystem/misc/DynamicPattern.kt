package org.woen.scoringSystem.misc


import kotlin.math.min
import org.woen.enumerators.BallRequest
import org.woen.configs.Alias.MAX_BALL_COUNT



class DynamicPattern
{
    private var _permanentPattern = ArrayList<BallRequest.Name>()
    private var _temporaryPattern = ArrayList<BallRequest.Name>()



    fun fullReset()
    {
        resetPermanent()
        resetTemporary()
    }

    fun resetPermanent() = _permanentPattern.clear()
    fun resetTemporary() = _temporaryPattern.clear()

    fun setPermanent(permanent: Array<BallRequest.Name>)
    {
        _permanentPattern = ArrayList(permanent.toList())
    }
    fun setTemporary(temporary: Array<BallRequest.Name>)
    {
        _temporaryPattern = ArrayList(temporary.toList())
    }


    fun removeFromTemporary()
    {
        if (_temporaryPattern.isNotEmpty())
            _temporaryPattern.removeAt(0)
    }
    fun addToTemporary()
    {
        val tempCount = _temporaryPattern.size
        if (tempCount >= MAX_BALL_COUNT) _temporaryPattern.clear()

        if (tempCount < _permanentPattern.size)
            _temporaryPattern.add(_permanentPattern[tempCount])
    }



    fun lastUnfinished() = ArrayList(_temporaryPattern)
    fun permanent()      = _permanentPattern



    companion object
    {
        @JvmStatic
        fun trimPattern(lastUnfinished: ArrayList<BallRequest.Name>,
                        pattern: Array<BallRequest.Name>)
                : Array<BallRequest.Name>
        {
            val newPatternLength = min(MAX_BALL_COUNT, lastUnfinished.size + pattern.size)
            val newPattern = Array(newPatternLength) { BallRequest.Name.NONE }

            var curRequestId = 0
            while (curRequestId < newPatternLength)
            {
                newPattern[curRequestId] = if (curRequestId < lastUnfinished.size)
                    lastUnfinished[curRequestId]
                else pattern[curRequestId]

                curRequestId++
            }

            return newPattern
        }
    }
}