package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min

import org.woen.enumerators.BallRequest
import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT



class DynamicPattern
{
    private var _permanentPattern = arrayOf    <BallRequest.Name>()
    private var _temporaryPattern = arrayListOf<BallRequest.Name>()



    @Synchronized
    fun fullReset()
    {
        _permanentPattern = arrayOf()
        _temporaryPattern = arrayListOf()
    }
    @Synchronized
    fun setPermanent(permanent: Array<BallRequest.Name>)
    {
        _permanentPattern = permanent.copyOf()
    }

    @Synchronized
    fun resetTemporary() = _temporaryPattern.clear()
    @Synchronized
    fun setTemporary(temporary: ArrayList<BallRequest.Name>)
    {
        _temporaryPattern = ArrayList(temporary)
    }
    @Synchronized
    fun setTemporary(temporary: Array<BallRequest.Name>)
    {
        _temporaryPattern = ArrayList(temporary.toList())
    }
    @Synchronized
    fun removeFromTemporary()
    {
        if (_temporaryPattern.isNotEmpty())
            _temporaryPattern.removeAt(0)
    }
    @Synchronized
    fun addToTemporary()
    {
        val tempCount = _temporaryPattern.size
        if (tempCount >= MAX_BALL_COUNT) _temporaryPattern.clear()

        if (tempCount < _permanentPattern.size)
            _temporaryPattern.add(_permanentPattern[tempCount])
    }



    fun lastUnfinished() = _temporaryPattern
    fun permanent() = _permanentPattern



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