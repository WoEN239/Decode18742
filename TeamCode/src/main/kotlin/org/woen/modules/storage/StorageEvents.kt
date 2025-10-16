package org.woen.modules.storage

import barrel.enumerators.IntakeResult
import barrel.enumerators.RequestResult

class TerminateIntakeEvent()
class TerminateRequestEvent()


class BarrelRequestIsReadyEvent()
data class BarrelFinishedRequestEvent(
    var requestResult: RequestResult.Name
)
data class BarrelFinishedIntakeEvent(
    var intakeResult: IntakeResult.Name
)


class GiveNextRequest()
class GiveNextIntake()