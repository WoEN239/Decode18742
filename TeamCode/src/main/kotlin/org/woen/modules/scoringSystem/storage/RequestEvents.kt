package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Shooting
import woen239.enumerators.BallRequest



class StorageGiveSingleRequest(
    var ballRequest: BallRequest.Name
)
class StorageGiveStreamDrumRequest()
class StorageGiveDrumRequest(
    var shootingMode:    Shooting.Mode,
    var requestPattern:  Array<BallRequest.Name>,
    var failsafePattern: Array<BallRequest.Name> = arrayOf()
)


class TerminateRequestEvent()


class StorageRequestIsReadyEvent()