package org.woen.modules.scoringSystem.storage


import org.woen.threading.StoppingEvent



class StorageOpenTurretGateEvent()
class StorageCloseTurretGateEvent()



class ShotWasFiredEvent()
data class BallCountInStorageEvent(var count: Int = 0): StoppingEvent