package org.woen.modules.scoringSystem.storage.sorting


import kotlin.math.min

import woen239.enumerators.BallRequest
import org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT



class DynamicPattern
{
    private var _temporaryPattern = arrayListOf<BallRequest.Name>()

    private var _permanentPattern = arrayOf<BallRequest.Name>()



    fun setPermanent(permanent: Array<BallRequest.Name>)
    {
        _permanentPattern = permanent.clone()
    }

    fun resetTemporary() = _temporaryPattern.clear()
    fun removeFromTemporary()
    {
        if (_temporaryPattern.isNotEmpty())
            _temporaryPattern.removeAt(_temporaryPattern.size - 1)
    }
    fun addToTemporary()
    {
        if (_temporaryPattern.size >= MAX_BALL_COUNT) _temporaryPattern.clear()

        _temporaryPattern.add(_permanentPattern[_temporaryPattern.size])
    }



    fun lastUnfinished(): ArrayList<BallRequest.Name> = _temporaryPattern

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