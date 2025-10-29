package org.woen.modules.scoringSystem.storage

import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult



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