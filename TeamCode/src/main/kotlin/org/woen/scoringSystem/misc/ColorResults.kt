package org.woen.scoringSystem.misc


import org.woen.enumerators.Ball
import org.woen.enumerators.StorageSlot


class StorageSlotsUpdates(
    var bottom: Boolean = true,
    var center: Boolean = false,
    var turret: Boolean = false)


class ColorResults
{
    var updateTargetsBCT = StorageSlotsUpdates()
    var parsedResults = arrayOf(Ball.Name.NOT_UPDATED, Ball.Name.NOT_UPDATED, Ball.Name.NOT_UPDATED)


    fun isEmptyBySensors()
        =  Ball.isMaskEmpty(parsedResults[StorageSlot.BOTTOM])
        && Ball.isMaskEmpty(parsedResults[StorageSlot.CENTER])
        && Ball.isMaskEmpty(parsedResults[StorageSlot.TURRET])
    fun onlyLastBallForShooting()
        =   Ball.isMaskEmpty(parsedResults[StorageSlot.BOTTOM])
        &&  Ball.isMaskEmpty(parsedResults[StorageSlot.CENTER])
        && !Ball.isMaskEmpty(parsedResults[StorageSlot.TURRET])
}