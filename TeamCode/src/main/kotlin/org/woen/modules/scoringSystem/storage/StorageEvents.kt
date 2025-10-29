package org.woen.modules.scoringSystem.storage

import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult



class TerminateIntakeEvent()
class TerminateRequestEvent()



data class StorageFinishedIntakeEvent(
    var intakeResult: IntakeResult.Name
)

class StorageRequestIsReadyEvent()
data class StorageFinishedEveryRequestEvent(
    var requestResult: RequestResult.Name
)



class GiveNextRequest()
class GiveNextIntake()