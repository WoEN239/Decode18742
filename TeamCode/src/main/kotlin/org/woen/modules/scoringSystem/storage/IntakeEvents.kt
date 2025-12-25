package org.woen.modules.scoringSystem.storage


import org.woen.enumerators.Ball

import org.woen.threading.StoppingEvent



class TerminateIntakeEvent()
class StorageGetReadyForIntakeEvent(
    var inputToBottomSlot: Ball.Name = Ball.Name.NONE)


data class StartLazyIntakeEvent(
    var startingResult: Boolean = false) : StoppingEvent
class StopLazyIntakeEvent()

class StopAnyIntakeEvent(
    var intakeStoppingResult: Boolean = false) : StoppingEvent



class StorageUpdateAfterLazyIntakeEvent(
    var inputFromTurretSlotToBottom: Array<Ball.Name>,
    var startingResult: Boolean = false)

class FillStorageWithUnknownColorsEvent(
    var startingResult: Boolean = false) : StoppingEvent