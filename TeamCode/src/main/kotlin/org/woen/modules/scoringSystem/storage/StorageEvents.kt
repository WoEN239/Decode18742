package org.woen.modules.scoringSystem.storage

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult



class TerminateIntakeEvent()
class TerminateRequestEvent()



class StorageIsReadyToEatIntakeEvent()
class BallWasEatenByTheStorageEvent()
data class StorageFinishedIntakeEvent(
    var intakeResult: IntakeResult.Name
)



class GiveNextRequest()

class StorageRequestIsReadyEvent()
data class StorageFinishedEveryRequestEvent(
    var requestResult: RequestResult.Name
)