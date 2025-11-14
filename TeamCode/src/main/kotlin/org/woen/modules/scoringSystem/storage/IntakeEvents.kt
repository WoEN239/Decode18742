package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Ball
import woen239.enumerators.IntakeResult



class StorageGetReadyForIntakeEvent(
    var inputBall: Ball.Name = Ball.Name.NONE
)
class TerminateIntakeEvent()



class StorageIsReadyToEatIntakeEvent()
class BallWasEatenByTheStorageEvent()
class StorageFinishedIntakeEvent(
    var intakeResult: IntakeResult.Name
)