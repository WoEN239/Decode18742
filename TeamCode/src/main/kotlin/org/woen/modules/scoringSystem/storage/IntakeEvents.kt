package org.woen.modules.scoringSystem.storage


import org.woen.enumerators.Ball
import org.woen.enumerators.IntakeResult

import org.woen.threading.StoppingEvent



class TerminateIntakeEvent()
class StorageGetReadyForIntakeEvent(
    var inputBall: Ball.Name = Ball.Name.NONE)


data class StartLazyIntakeEvent(
    var startingResult: IntakeResult.Name) : StoppingEvent
class StopLazyIntakeEvent()

class StorageUpdateAfterLazyIntakeEvent(
    var inputFromTurretSlotToBottom: Array<Ball.Name>)