package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Ball
import org.woen.threading.StoppingEvent



class EnableSortingModuleEvent()
class DisableSortingModuleEvent()



class ShotWasFiredEvent()
data class BallCountInStorageEvent(var count: Int = 0): StoppingEvent
data class StorageHandleIdenticalColorsEvent(
    var maxIdenticalColorCount: Int = 0,
    var identicalColor: Ball.Name) : StoppingEvent