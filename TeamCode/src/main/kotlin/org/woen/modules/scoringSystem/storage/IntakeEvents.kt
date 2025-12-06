package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Ball
import woen239.enumerators.IntakeResult


class TerminateIntakeEvent()
class StorageGetReadyForIntakeEvent(
    var inputBall: Ball.Name = Ball.Name.NONE
)


data class StartLazyIntakeEvent(
    var startingResult: IntakeResult.Name
)
class StopLazyIntakeEvent()
class StorageUpdateAfterLazyIntakeEvent(
    var inputBallsFromBottomToMobileOut: Array<Ball.Name>
)