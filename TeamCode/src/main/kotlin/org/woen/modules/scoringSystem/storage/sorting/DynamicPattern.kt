package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min

import woen239.enumerators.BallRequest
import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT



class DynamicPattern
{
    private var _permanentPattern = arrayOf<BallRequest.Name>()
    private var _temporaryPattern = arrayListOf<BallRequest.Name>()



    fun fullReset()
    {
        _permanentPattern = arrayOf()
        _temporaryPattern = arrayListOf()
    }
    fun setPermanent(permanent: Array<BallRequest.Name>)
    {
        _permanentPattern = permanent.copyOf()
    }

    fun resetTemporary() = _temporaryPattern.clear()
    fun setTemporary(temporary: ArrayList<BallRequest.Name>)
    {
        _temporaryPattern = ArrayList(temporary)
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
        if (_temporaryPattern.size >= MAX_BALL_COUNT) _temporaryPattern.clear()

        val tempCount = _temporaryPattern.size
        if (_permanentPattern.size > tempCount)
            _temporaryPattern.add(_permanentPattern[_temporaryPattern.size])
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
            val newPattern = arrayOf<BallRequest.Name>()

            var curRequestId = 0
            while (curRequestId < newPatternLength) {
                newPattern[curRequestId] = if (curRequestId < lastUnfinished.size)
                    lastUnfinished[curRequestId]
                else pattern[curRequestId]

                curRequestId++
            }

            return newPattern
        }
    }
}