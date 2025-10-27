package org.woen.modules.storage

import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult



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