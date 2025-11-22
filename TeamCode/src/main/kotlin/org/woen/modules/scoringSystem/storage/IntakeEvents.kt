package org.woen.modules.scoringSystem.storage


import org.woen.threading.StoppingEvent
import woen239.enumerators.Ball
import woen239.enumerators.IntakeResult



class StorageGetReadyForIntakeEvent(
    var inputBall: Ball.Name = Ball.Name.NONE
)
class TerminateIntakeEvent()



class BallWasEatenByTheStorageEvent()
class StorageFinishedIntakeEvent(
    var intakeResult: IntakeResult.Name
)



data class BallCountInStorageEvent(var count: Int = 0): StoppingEvent