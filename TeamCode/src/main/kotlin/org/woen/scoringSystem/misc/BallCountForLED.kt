package org.woen.scoringSystem.misc



class BallCountForLED(
    var count: Int = 0,
    var infoIsGuaranteed: Boolean = true)
{
    fun update(
        count: Int = 0,
        infoIsGuaranteed: Boolean = true)
    {
        this.count = count
        this.infoIsGuaranteed = infoIsGuaranteed
    }
}