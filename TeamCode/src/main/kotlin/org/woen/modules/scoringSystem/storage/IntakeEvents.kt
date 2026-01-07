package org.woen.modules.scoringSystem.storage


import org.woen.enumerators.Ball

import org.woen.threading.StoppingEvent



class TerminateIntakeEvent()
class WaitForTerminateIntakeEvent() : StoppingEvent


class StorageGetReadyForIntakeEvent(
    var inputToBottomSlot: Ball.Name = Ball.Name.NONE)


data class StartLazyIntakeEvent(
    var startingResult: Boolean = false) : StoppingEvent
class StopLazyIntakeEvent()



class StorageUpdateAfterLazyIntakeEvent(
    var inputFromTurretSlotToBottom: Array<Ball.Name>,
    var startingResult: Boolean = false) : StoppingEvent

class FillStorageWithUnknownColorsEvent(
    var startingResult: Boolean = false) : StoppingEvent



class FullFinishedIntakeEvent(
    var ballCountInStorage: Int = 0)