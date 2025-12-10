package org.woen.modules.scoringSystem.storage


import org.woen.enumerators.Shooting
import org.woen.enumerators.BallRequest
import org.woen.enumerators.RequestResult


class StorageGiveSingleRequest(
    var ballRequest: BallRequest.Name)
class StorageGiveStreamDrumRequest()
class StorageGiveDrumRequest(
    var shootingMode:    Shooting.Mode,
    var requestPattern:  Array<BallRequest.Name>,
    var failsafePattern: Array<BallRequest.Name> = arrayOf())


class TerminateRequestEvent()


class StorageRequestIsReadyEvent()
class FullFinishedFiringEvent(
    var requestResult: RequestResult.Name)