package org.woen.modules.scoringSystem.storage


import org.woen.enumerators.Ball

import org.woen.threading.StoppingEvent



data class TerminateIntakeEvent(
    var stoppingResult: Boolean = false) : StoppingEvent
data class WaitForTerminateIntakeEvent(
    var stoppingResult: Boolean = false) : StoppingEvent


class StorageGetReadyForIntakeEvent(
    var inputToBottomSlot: Ball.Name = Ball.Name.NONE)


data class StartLazyIntakeEvent(
    var startingResult: Boolean = false) : StoppingEvent
class StopLazyIntakeEvent() : StoppingEvent



@Suppress("ArrayInDataClass")
data class StorageUpdateAfterLazyIntakeEvent(
    var inputFromTurretSlotToBottom: Array<Ball.Name>,
    var startingResult: Boolean = false) : StoppingEvent

data class FillStorageWithUnknownColorsEvent(
    var startingResult: Boolean = false) : StoppingEvent



data class FullFinishedIntakeEvent(
    var ballCountInStorage: Int = 0)