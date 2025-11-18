package org.woen.modules.scoringSystem.storage


import woen239.enumerators.BallRequest
import woen239.enumerators.RequestResult
import woen239.enumerators.Shooting



class StorageGiveSingleRequest(
    var ballRequest: BallRequest.Name
)
class StorageGiveSimpleDrumRequest()
class StorageGiveDrumRequest(
    var shootingMode:    Shooting.Mode,
    var requestPattern:  Array<BallRequest.Name>,
    var failsafePattern: Array<BallRequest.Name> = arrayOf()
)


class TerminateRequestEvent()





class StorageRequestIsReadyEvent()
data class StorageFinishedEveryRequestEvent(
    var requestResult: RequestResult.Name
)

class ShotWasFiredEvent()




class StorageOpenTurretGateEvent()
class StorageCloseTurretGateEvent()