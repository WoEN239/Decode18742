package org.woen.modules.scoringSystem.storage


import org.woen.enumerators.Ball
import org.woen.threading.StoppingEvent



class ShotWasFiredEvent()
data class BallCountInStorageEvent(var count: Int = 0): StoppingEvent
data class StorageHandleIdenticalColorsEvent(
    var maxIdenticalColorCount: Int = 0,
    var identicalColor: Ball.Name = Ball.Name.NONE) : StoppingEvent